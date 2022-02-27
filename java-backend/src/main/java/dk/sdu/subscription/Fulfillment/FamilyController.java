package dk.sdu.subscription.Fulfillment;

import dk.sdu.subscription.Interfaces.IDatabaseService;
import dk.sdu.subscription.Interfaces.IFamily;
import dk.sdu.subscription.Interfaces.IUser;
import dk.sdu.subscription.Interfaces.IUserService;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.SubscriptionType;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Route;
import spark.RouteGroup;

import static spark.Spark.*;

public class FamilyController implements RouteGroup {
    private IDatabaseService databaseService;
    private IUserService userService;

    public FamilyController(IDatabaseService databaseService, IUserService userService) {
        this.databaseService = databaseService;
        this.userService = userService;
    }

    @Override
    public void addRoutes() {
        get("/", index());
        get("/user", userFamily());
        post("/", createFamily());
        post("/:familyId", joinFamily());
        delete("/", leaveFamily());
    }

    private Route leaveFamily() {
        return (Route) (request, response) -> {
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);

            boolean leaved = this.databaseService.leaveFamily(user);
            if(!leaved){
                return RequestUtils.badRequest(response, "You are not part of a family.");
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", true);
            jsonObject.put("message", "Successfully left family");
            return jsonObject.toString();
        };
    }

    private Route joinFamily() {
        return (Route) (request, response) -> {
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            String familyId = request.params(":familyId");

            boolean joined = this.databaseService.joinFamily(familyId, user);
            if(!joined){
                return RequestUtils.badRequest(response, "Family is either full, or does not exist.");
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", true);
            jsonObject.put("message", "Successfully joined family");
            return jsonObject.toString();
        };
    }

    private Route createFamily() {
        return (Route) (request, response) -> {
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            IFamily family;
            try{
                family = this.databaseService.createFamily(user);
            }catch(Exception e){
                e.printStackTrace();
                return RequestUtils.badRequest(response, "You cannot create a family if you're already a part of one.");
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", true);
            jsonObject.put("message", "Family created successfully!");
            jsonObject.put("createdFamilyId", family.getFamilyId());
            return jsonObject.toString();
        };
    }

    private Route userFamily() {
        return (Route) (request, response) -> {
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            IFamily family = this.databaseService.getFamily(user);

            JSONArray familyMembers = new JSONArray();
            for(IUser member : family.getUsers()){
                JSONObject familyMember = new JSONObject();
                familyMember.put("userId", member.getId());
                familyMember.put("name", member.getName());
                familyMember.put("subscription", member.getSubtype());
                familyMembers.put(familyMember);
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("isInFamily", !family.getFamilyId().isEmpty());
            jsonObject.put("familyIsPremium", family.getUsers().stream().anyMatch(x->x.getSubtype().equalsIgnoreCase(SubscriptionType.FAMILY.name())));
            jsonObject.put("familyMembers", familyMembers);
            jsonObject.put("familyId", family.getFamilyId());
            return jsonObject.toString();
        };
    }

    private Route index() {
        return (Route) (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "Family endpoint. Use this endpoint to create, join and leave families");
            jsonObject.put("createFamily", createEndpointDescriptor("Creates a new family", "/", "POST"));
            jsonObject.put("userFamily", createEndpointDescriptor("Shows what family the user is in", "/user", "GET"));
            jsonObject.put("leaveFamily", createEndpointDescriptor("Leaves a family", "/<family-id>", "DELETE"));
            jsonObject.put("joinFamily",createEndpointDescriptor("Joins a family", "/<family-id>", "POST"));
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
}
