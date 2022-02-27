package dk.sdu.subscription.Fulfillment;

import dk.sdu.subscription.Interfaces.Exceptions.MailException;
import dk.sdu.subscription.Interfaces.Exceptions.PaymentMethodException;
import dk.sdu.subscription.Interfaces.IDatabaseService;
import dk.sdu.subscription.Interfaces.IPaymentService;
import dk.sdu.subscription.Interfaces.IUser;
import dk.sdu.subscription.Interfaces.IUserService;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.IPaymentMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Route;
import spark.RouteGroup;

import static spark.Spark.*;
import static spark.Spark.get;
import static spark.Spark.post;

public class PaymentController implements RouteGroup  {
    private final IPaymentService paymentService;
    private final IDatabaseService databaseService;
    private final IUserService userService;

    public PaymentController(IPaymentService stripeService, IDatabaseService databaseService, IUserService userService) {
        this.paymentService = stripeService;
        this.databaseService = databaseService;
        this.userService = userService;
    }

    @Override
    public void addRoutes() {
        get("/", index());
        post("/", createPaymentMethod());
        get("/get", getPaymentMethods());
        delete("/all", deleteAllPaymentMethods());
        delete("/:paymentId", deletePaymentMethod());
        patch("/:paymentId", setDefaultMethod());
        patch("/edit/:paymentId", editPaymentMethod());
    }

    private Route editPaymentMethod() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            JSONObject jsonResponse = new JSONObject();
            String paymentId;
            IPaymentMethod paymentMethod;

            String body = request.body();
            String cardNumber;
            int expireMonth;
            int expireYear;
            int cvc;

            try {
                paymentId = request.params(":paymentId");
            } catch (JSONException e) {
                return RequestUtils.badRequest(response, "PaymentId missing or invalid");
            }
            try {
                JSONObject jsonBody = new JSONObject(body);
                cardNumber = jsonBody.getString("cardNumber");
                expireMonth = Integer.parseInt(jsonBody.getString("expireMonth"));
                expireYear = Integer.parseInt(jsonBody.getString("expireYear"));
                cvc = Integer.parseInt(jsonBody.getString("cvc"));
                //paymentMethod = jsonBody.getString("method");

            } catch (JSONException e) {
                return RequestUtils.badRequest(response,"Request body not JSON valid, or missing payment parameter");
            }

            boolean userHasRights = false;
            try {
                paymentMethod = paymentService.getPaymentMethod(paymentId);
                for (IPaymentMethod method: paymentService.getPaymentMethodsForUser(user)) {
                    if (method.getPaymentId().equals(paymentId) || user.hasAdminRights()) {
                        userHasRights = true;
                        break;
                    }
                }
                if (!userHasRights) {
                    return RequestUtils.notAcceptable(response, "User has not rights to delete payment");
                }
            } catch (PaymentMethodException e) {
                e.printStackTrace();
                return RequestUtils.badRequest(response, "Payment method does not match user or does not exist");
            }

            try {
                paymentService.removePaymentMethod(paymentMethod);
                paymentService.addPaymentMethod(user, cardNumber, expireMonth, expireYear, cvc);
            } catch (PaymentMethodException e) {
                e.printStackTrace();
                return RequestUtils.badRequest(response, "Something went wrong while updating payment method:" + paymentId);
            }

            jsonResponse.put("success", true);
            jsonResponse.put("message", "payment updated Successfully");
            jsonResponse.put("paymentId", paymentService.getPaymentMethodsForUser(user).get(0).getPaymentId());
            return jsonResponse.toString();
        };
    }

    private Route setDefaultMethod() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            JSONObject jsonResponse = new JSONObject();
            String paymentId;
            IPaymentMethod paymentMethod;

            try {
                paymentId = request.params(":paymentId");
            } catch (JSONException e) {
                return RequestUtils.badRequest(response, "Missing paymentId");
            }
            try {
                paymentMethod = paymentService.getPaymentMethod(paymentId);
                paymentService.setDefaultPaymentMethod(user, paymentMethod);
            } catch (PaymentMethodException e) {
                return RequestUtils.badRequest(response,"Payment method invalid");
            }
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Payment method set");
            return jsonResponse;
        };
    }

    private Route getPaymentMethods() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);

            JSONArray jsonArray = new JSONArray();

            for (IPaymentMethod paymentMethod: paymentService.getPaymentMethodsForUser(user)) {
                JSONObject card = new JSONObject();
                card.put("expireMonth", paymentMethod.getExpireMonth());
                card.put("expireYear", paymentMethod.getExpireYear());
                card.put("lastFourDigits", paymentMethod.getLastFourDigits());
                card.put("cardType", paymentMethod.getCardType());
                card.put("paymentId", paymentMethod.getPaymentId());
                card.put("default", false); //TODO: Hardcoded for now, will be dynamic later
                jsonArray.put(card);
            }

            return jsonArray;
        };
    }

    private Route deletePaymentMethod() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");

            JSONObject jsonResponse = new JSONObject();
            String paymentId;
            IPaymentMethod paymentMethod;
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            boolean userHasRights = false;

            try {
                paymentId = request.params(":paymentId");
            } catch (JSONException e) {
                return RequestUtils.badRequest(response, "Request body not JSON valid, or missing paymentId parameter");
            }

            try {
                paymentMethod = paymentService.getPaymentMethod(paymentId);
                for (IPaymentMethod method: paymentService.getPaymentMethodsForUser(user)) {
                    if (method.getPaymentId().equals(paymentId) || user.hasAdminRights()) {
                        userHasRights = true;
                        break;
                    }
                }
                if (!userHasRights) {
                    return RequestUtils.notAcceptable(response, "User has not rights to delete payment");
                }
            } catch (PaymentMethodException e) {
                e.printStackTrace();
                return RequestUtils.badRequest(response, "Payment method does not match user or does not exist");
            }

            try {
                paymentService.removePaymentMethod(paymentMethod);
            } catch (PaymentMethodException e) {
                e.printStackTrace();
                return RequestUtils.badRequest(response, "Something went wrong while deleting payment method:" + paymentId);
            }
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Payment method deleted Successfully");
            return jsonResponse.toString();
        };
    }

    private Route deleteAllPaymentMethods() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonResponse = new JSONObject();

            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);

            if (paymentService.getPaymentMethodsForUser(user).isEmpty()) {
                jsonResponse.put("success", true);
                jsonResponse.put("message", "All payments deleted, user had no payments to delete");

                return jsonResponse;
            } else {
                try {
                    paymentService.removeAllPaymentMethods(user);
                } catch (PaymentMethodException e) {
                    e.printStackTrace();
                    return RequestUtils.badRequest(response, "Something went wrong while deleting payment methods for:" + user.getName());
                }
            }
            jsonResponse.put("success", true);
            jsonResponse.put("message", "All payment methods deleted Successfully");
            return jsonResponse.toString();
        };
    }

    private Route createPaymentMethod() {
        return (request, response) -> {

            String body = request.body();
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);

            JSONObject jsonResponse = new JSONObject();

            String cardNumber;
            int expireMonth;
            int expireYear;
            int cvc;

            try {
                JSONObject jsonBody = new JSONObject(body);
                cardNumber = jsonBody.getString("cardNumber");
                expireMonth = Integer.parseInt(jsonBody.getString("expireMonth"));
                expireYear = Integer.parseInt(jsonBody.getString("expireYear"));
                cvc = Integer.parseInt(jsonBody.getString("cvc"));
                //paymentMethod = jsonBody.getString("method");

            } catch (JSONException e) {
                return RequestUtils.badRequest(response,"Request body not JSON valid, or missing payment parameter");
            }

            try {
                paymentService.addPaymentMethod(user, cardNumber, expireMonth, expireYear, cvc);
            } catch (PaymentMethodException e) {
                return RequestUtils.notAcceptable(response, "Card is invalid");
            }
            JSONObject mailObject = new JSONObject();
            mailObject.put("mailTo",user.getEmail());
            mailObject.put("subject","Added payment method");
            mailObject.put("body","You successfully added card with **** " + cardNumber.substring(12,16) + " as your payment method.");

            RequestUtils rq = new RequestUtils();
            try{
                rq.sendMail(mailObject);
            }catch (MailException ex){
                ex.printStackTrace();
            }
            jsonResponse.put("success", true);
            jsonResponse.put("message", "payment created Successfully");
            jsonResponse.put("paymentId", paymentService.getPaymentMethodsForUser(user).get(0).getPaymentId());
            return jsonResponse.toString();
        };
    }

    private Route index() {
        return (Route) (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonObject = new JSONObject();

            JSONObject createPaymentBody = new JSONObject();
            createPaymentBody.put("cardNumber", "4242424242424242");
            createPaymentBody.put("expireMonth", "12");
            createPaymentBody.put("expireYear", "23");
            createPaymentBody.put("cvc", "123");

            jsonObject.put("message", "Payment method endpoint. Use this endpoint to create edit and delete payment method for a user");
            jsonObject.put("getPaymentMethod",createEndpointDescriptor("Returns the users payment methods", "/get", "GET"));
            jsonObject.put("createPaymentMethod", createEndpointDescriptor("Creates a new Payment method", "/", "POST", createPaymentBody.toString()));
            jsonObject.put("deleteAllPaymentMethod", createEndpointDescriptor("Deletes all the payment methods the user has", "/all", "DELETE"));
            jsonObject.put("deletePaymentMethod", createEndpointDescriptor("Deletes one payment method", "/<paymentId>", "DELETE"));
            jsonObject.put("setDefaultMethod", createEndpointDescriptor("Set the payment method as default", "/<paymentId>", "PATCH"));
            jsonObject.put("editPaymentMethod", createEndpointDescriptor("Edits the payment method (Deletes the one to be edited and creates a new", "/edit/<paymentId>", "PATCH"));
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
