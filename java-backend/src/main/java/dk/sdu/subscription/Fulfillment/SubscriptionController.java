package dk.sdu.subscription.Fulfillment;

import dk.sdu.subscription.Interfaces.Exceptions.SubscriptionException;
import dk.sdu.subscription.Interfaces.*;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.ISubscription;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.SubscriptionType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Route;
import spark.RouteGroup;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static spark.Spark.*;

public class SubscriptionController implements RouteGroup {
    private final IPaymentService paymentService;
    private final IDatabaseService databaseService;
    private final IInvoiceService invoiceService;
    private final IUserService userService;

    public SubscriptionController(IPaymentService paymentService, IDatabaseService databaseService, IInvoiceService invoiceService, IUserService userService) {
        this.paymentService = paymentService;
        this.databaseService = databaseService;
        this.invoiceService = invoiceService;
        this.userService = userService;
    }

    @Override
    public void addRoutes() {
        get("/", index());
        get("/list", list()); //Shows subscriptions you can sign up for
        get("/user", showUser()); //Shows subscription information about the logged-in user
        post("/create", create()); //Creates a new subscription
        get("/:subscriptionId", show()); //Shows information about a given susbscription
        post("/:subscriptionId/continue", undoCancel()); //Cancels a cancellation
        delete("/:subscriptionId", cancel(false)); //Cancels a subscription
        delete("/:subscriptionId/force", cancel(true)); //Forcefully cancels a subscription
        patch("/:subscriptionId", update()); //Updates a subscription tier
        post("/refreshSubscriptionTable", refreshSubscriptions()); //Refreshes the subscription table (admin only)
    }

    /**
     * Lists all available subscriptions
     *
     * @return
     */
    public Route list() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONArray jsonResponse = new JSONArray();

            List<ISubscription> subscriptions = paymentService.getAllSubscriptions();

            for (ISubscription subscriptionData : subscriptions) {
                JSONObject subscription = new JSONObject();
                SubscriptionType subscriptionType = subscriptionData.getSubscriptionType();
                if (subscriptionType == SubscriptionType.UNKNOWN) {
                    continue;
                }
                subscription.put("name", subscriptionData.getName());
                subscription.put("price", subscriptionData.getPrice());
                subscription.put("currency", subscriptionData.getCurrency());
                subscription.put("id", subscriptionType.name());

                jsonResponse.put(subscription);
            }

            return jsonResponse.toString();
        };
    }

    /**
     * Creates a subscription for a given user.
     *
     * @return
     */
    private Route create() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            response.header("Accept", "application/json");
            JSONObject jsonResponse = new JSONObject();

            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            String subscription;
            String body = request.body();
            ISubscription sub = null;

            try {
                JSONObject jsonBody = new JSONObject(body);
                subscription = jsonBody.getString("subscription");
            } catch (JSONException e) {
                return RequestUtils.badRequest(response, "Request body is not JSON valid, or is missing `subscription` parameter");
            }

            SubscriptionType subscriptionType;
            try {
                subscriptionType = SubscriptionType.valueOf(subscription.toUpperCase());
            } catch (IllegalArgumentException e) {
                return RequestUtils.badRequest(response, "Invalid `subscription` parameter. Subscription tier not found.");
            }

            //Check if in family that has subscription
            if(this.databaseService.userHasPremium(user)){
                boolean isInFamily = user.getFamilyId() != null;
                String familyDescription = isInFamily ? ", or leave your family." : ".";
                return RequestUtils.notAcceptable(response, "You already have a subscription. Cancel your current subscription" + familyDescription);
            }

            try {
                //Saves the user in the subscriptions table
                sub = this.paymentService.createSubscriptionForUser(user, subscriptionType);
                databaseService.saveUserSubscribed(user, sub);
            } catch (SubscriptionException e) {
                return RequestUtils.badRequest(response, "Could not create subscription: " + e.getMessage());
            }
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Subscription created Successfully");
            jsonResponse.put("subscriptionId", sub.getId());

            return jsonResponse.toString();
        };
    }

    /**
     * Shows subscription information from a subscription id
     *
     * @return
     */
    private Route show() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonResponse = new JSONObject();

            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);

            ISubscription subscription;
            try {
                subscription = this.paymentService.getSubscription(request.params(":subscriptionId"));
                if (!subscription.getSubscriptionOwner().equals(user.getId()) && !user.hasAdminRights()) {
                    return RequestUtils.notAcceptable(response, "You can only fetch subscriptions for your own account");
                }
            } catch (Exception e) {
                return RequestUtils.badRequest(response, "Invalid subscription id: " + e.getMessage());
            }

            jsonResponse.put("id", subscription.getId());
            jsonResponse.put("status", subscription.getStatus());
            jsonResponse.put("name", subscription.getName());
            jsonResponse.put("price", subscription.getPrice());
            jsonResponse.put("currency", subscription.getCurrency());
            jsonResponse.put("renewsAt", subscription.getNextRenewal());
            jsonResponse.put("tier", subscription.getSubscriptionType().name());
            jsonResponse.put("cancelsAtNextRenewal", subscription.isCanceled());
            return jsonResponse.toString();
        };
    }

    /**
     * Shows subscription information about a given user.
     *
     * @return
     */
    private Route showUser() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonResponse = new JSONObject();
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            List<ISubscription> subscriptionList = this.paymentService.getActiveSubscriptions(user);

            JSONArray subscriptionArray = new JSONArray();
            for (ISubscription subscription : subscriptionList) {
                JSONObject subscriptionObject = new JSONObject();
                subscriptionObject.put("id", subscription.getId());
                subscriptionObject.put("status", subscription.getStatus());
                subscriptionObject.put("name", subscription.getName());
                subscriptionObject.put("price", subscription.getPrice());
                subscriptionObject.put("currency", subscription.getCurrency());
                subscriptionObject.put("renewsAt", subscription.getNextRenewal());
                subscriptionObject.put("tier", subscription.getSubscriptionType().name());
                subscriptionObject.put("cancelsAtNextRenewal", subscription.isCanceled());
                subscriptionArray.put(subscriptionObject);
            }
            jsonResponse.put("subscriptions", subscriptionArray);

            //Get subscriptions from family
            boolean premium = this.databaseService.userHasPremium(user);
            boolean premiumFromFamily = premium && subscriptionList.isEmpty();
            String effectiveTier;
            if (subscriptionList.isEmpty()) {
                effectiveTier = premium ? SubscriptionType.FAMILY.name() : SubscriptionType.FREE.name();
            } else {
                effectiveTier = this.paymentService.getSubscriptionTypeForUser(user).name();
            }

            jsonResponse.put("effectiveTier", effectiveTier);
            jsonResponse.put("premiumFromFamily", premiumFromFamily);
            return jsonResponse.toString();
        };
    }

    /**
     * Cancels a subscription
     *
     * @return
     */
    private Route cancel(boolean force) {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonResponse = new JSONObject();

            ISubscription subscription;
            try {
                subscription = this.paymentService.getSubscription(request.params(":subscriptionId"));
            } catch (Exception e) {
                return RequestUtils.badRequest(response, "Invalid subscription id");
            }

            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);

            if (!subscription.getSubscriptionOwner().equals(user.getId()) && !user.hasAdminRights()) {
                return RequestUtils.notAcceptable(response, "You can only cancel subscriptions for your own account");
            }
            String message;
            String subscriptionName = subscription.getName();
            try {
                if (force) {
                    this.paymentService.forceCancelSubscription(subscription);
                    this.databaseService.deleteSubscription(subscription);
                    message = "Subscription " + subscriptionName + " has been canceled with immediate effect";
                } else {
                    this.paymentService.cancelSubscription(subscription);
                    Instant expireAt = Instant.ofEpochSecond(subscription.getNextRenewal());
                    message = "Subscription " + subscriptionName + " will be canceled on the " + expireAt.toString();
                    if (!user.getEmail().equals("NULL")) {
                        RequestUtils rq = new RequestUtils();
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("mailTo", user.getEmail());
                        jsonObject.put("subject", "Canceled Subscription");
                        jsonObject.put("body", "Dear " + user.getName() + "\nThis email has been send to you, to notify you that you have cancelled your subscription to Music Stream APS on "
                                + new Date()
                                + "\n Your subscription is valid until "
                                + new Date(subscription.getNextRenewal() * 1000));
                        rq.sendMail(jsonObject);
                    }
                }
            } catch (Exception e) {
                return RequestUtils.badRequest(response, "Failed to cancel subscription: " + e.getMessage());
            }
            jsonResponse.put("message", message);
            if (!force) jsonResponse.put("cancelsAt", subscription.getNextRenewal());
            jsonResponse.put("success", true);
            return jsonResponse.toString();
        };
    }

    /**
     * Cancels a subscription
     *
     * @return
     */
    private Route undoCancel() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonResponse = new JSONObject();

            ISubscription subscription;
            try {
                subscription = this.paymentService.getSubscription(request.params(":subscriptionId"));
            } catch (Exception e) {
                return RequestUtils.badRequest(response, "Invalid subscription id");
            }

            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);

            if (!subscription.getSubscriptionOwner().equals(user.getId()) && !user.hasAdminRights()) {
                return RequestUtils.notAcceptable(response, "You can only cancel subscriptions for your own account");
            }
            try {
                this.paymentService.cancelCancellation(subscription);
            } catch (Exception e) {
                return RequestUtils.badRequest(response, "Failed to cancel subscription: " + e.getMessage());
            }

            jsonResponse.put("message", "Subscription " + subscription.getName() + " will no longer be cancelled.");
            jsonResponse.put("renewsAt", subscription.getNextRenewal());
            jsonResponse.put("success", true);
            return jsonResponse.toString();
        };
    }

    /**
     * Updates a subscription to another tier (both upgrade and downgrade)
     *
     * @return
     */
    private Route update() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonResponse = new JSONObject();

            ISubscription subscription = this.paymentService.getSubscription(request.params(":subscriptionId"));
            if (subscription == null) {
                return RequestUtils.badRequest(response, "Invalid subscription id");
            }

            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);

            if (!subscription.getSubscriptionOwner().equals(user.getId()) && !user.hasAdminRights()) {
                return RequestUtils.notAcceptable(response, "You can only change subscriptions for your own account");
            }

            String subscriptionId;
            String body = request.body();
            try {
                JSONObject jsonBody = new JSONObject(body);
                subscriptionId = jsonBody.getString("subscription");
            } catch (JSONException e) {
                return RequestUtils.badRequest(response, "Request body is not JSON valid, or is missing `subscription` parameter");
            }

            SubscriptionType subscriptionType;
            try {
                subscriptionType = SubscriptionType.valueOf(subscriptionId.toUpperCase());
            } catch (IllegalArgumentException e) {
                return RequestUtils.badRequest(response, "Invalid `subscription` parameter. Subscription tier not found.");
            }

            try {
                this.paymentService.updateSubscription(subscription, subscriptionType);
            } catch (SubscriptionException e) {
                return RequestUtils.notAcceptable(response, "Could not update subscription: " + e.getMessage());
            }

            this.databaseService.updateSubscription(subscription);

            jsonResponse.put("message", "Subscription " + subscription.getId() + " updated to tier " + subscriptionType.name() + " successfully!");
            jsonResponse.put("success", true);
            return jsonResponse.toString();
        };
    }

    private Route refreshSubscriptions() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonResponse = new JSONObject();


            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);

            if(!user.hasAdminRights()){
                return RequestUtils.notAcceptable(response, "You do not have permission to use this endpoint");
            }

            try {
                List<ISubscription> allSubscriptions = this.paymentService.getAllSubscriptionsForDatabaseResync();

                this.databaseService.refreshSubscriptionTable(allSubscriptions);
            } catch (SubscriptionException e) {
                return RequestUtils.notAcceptable(response, "Could not refresh subscription table: " + e.getMessage());
            }
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Subscription table refreshed successfully");

            return jsonResponse.toString();
        };
    }

    private Route index() {
        return (Route) (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonObject = new JSONObject();

            JSONObject createBody = new JSONObject();
            createBody.put("subscription", "<Tier>");

            jsonObject.put("message", "Subscription endpoint. Use this endpoint to ");
            jsonObject.put("list",createEndpointDescriptor("Shows subscriptions you can sign up for", "/list", "GET"));
            jsonObject.put("showUser", createEndpointDescriptor("Shows subscription information about the logged-in user", "/user", "GET"));
            jsonObject.put("create", createEndpointDescriptor("Creates a new subscription", "/create", "POST", createBody.toString()));
            jsonObject.put("show", createEndpointDescriptor("Shows information about a given susbscription", "/<subscriptionId>", "GET"));
            jsonObject.put("undoCancel", createEndpointDescriptor("Cancels a cancellation", "/<subscriptionId>/continue", "POST"));
            jsonObject.put("cancel(false)", createEndpointDescriptor("Cancels a subscription", "/<subscriptionId>", "DELETE"));
            jsonObject.put("cancel(true)", createEndpointDescriptor("Forcefully cancels a subscription", "/<subscriptionId>/force", "DELETE"));
            jsonObject.put("update", createEndpointDescriptor("Updates a subscription tier", "/<subscriptionId>", "PATCH", createBody.toString()));
            jsonObject.put("refreshSubscriptions", createEndpointDescriptor("Refreshes the subscription table (admin only)", "/refreshSubscriptionTable", "POST"));

            return jsonObject.toString();
        };
    }

    private static JSONObject createEndpointDescriptor(String description, String endpoint, String method) {
        JSONObject json = new JSONObject();
        json.put("description", description);
        json.put("endpoint", endpoint);
        json.put("method", method);
        return json;
    }

    private static JSONObject createEndpointDescriptor(String description, String endpoint, String method, String body) {
        JSONObject json = new JSONObject();
        json.put("description", description);
        json.put("endpoint", endpoint);
        json.put("method", method);
        json.put("requestBody", body);
        return json;
    }
}
