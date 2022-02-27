package dk.sdu.subscription.Fulfillment;

import dk.sdu.subscription.Interfaces.Exceptions.MailException;
import dk.sdu.subscription.Interfaces.Exceptions.UserFetchException;
import dk.sdu.subscription.Interfaces.ISecretService;
import dk.sdu.subscription.Interfaces.IUser;
import dk.sdu.subscription.Interfaces.IUserService;
import dk.sdu.subscription.ServiceLayer.SecretService.SecretServiceProvider;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static spark.Spark.halt;

public class RequestUtils {

    ISecretService secretService = SecretServiceProvider.getSecretService();

    public static String badRequest(Response response, String message){
        JSONObject jsonResponse = new JSONObject();
        response.status(400);
        jsonResponse.put("error", true);
        jsonResponse.put("message","Bad request: " + message);
        return jsonResponse.toString();
    }

    public static String notAcceptable(Response response, String message){
        JSONObject jsonResponse = new JSONObject();
        response.status(406);
        jsonResponse.put("error", true);
        jsonResponse.put("message","Not acceptable: " + message);
        return jsonResponse.toString();
    }

    public static String preconditionFailed(Response response, String message){
        JSONObject jsonResponse = new JSONObject();
        response.status(412);
        jsonResponse.put("error", true);
        jsonResponse.put("message","Precondition Failed: " + message);
        return jsonResponse.toString();
    }

    public static String internalError(Response response, String message, Exception exception){
        JSONObject jsonResponse = new JSONObject();
        response.status(500);
        jsonResponse.put("error", true);
        jsonResponse.put("message","Internal Server error: " + message);
        jsonResponse.put("exception", exception);
        return jsonResponse.toString();
    }

    public static String internalError(Response response, String message){
        JSONObject jsonResponse = new JSONObject();
        response.status(500);
        jsonResponse.put("error", true);
        jsonResponse.put("message","Internal Server error: " + message);
        return jsonResponse.toString();
    }

    public static String getCurrentUserId(Request request){
        return request.attribute("userId");
    }

    public static IUser getCurrentUser(Request request, Response response, IUserService userService) throws Exception {
        String header = request.headers("x-test-db");
        boolean shouldUseTestDb = header != null && header.equals("1");
        try {
            return userService.getUserFromId(getCurrentUserId(request), shouldUseTestDb);
        }catch(Exception e){
            if(e instanceof UserFetchException){
                halt(RequestUtils.internalError(response, e.getMessage()));
                return null;
            }
            halt(RequestUtils.internalError(response, "Failed to fetch user info", e));
            return null;
        }
    }

    public void sendMail(JSONObject object) throws Exception {
        if(object.get("mailTo").equals("NULL")){
            throw new MailException("No email on user");
        }

        URL url = new URL(secretService.getStringSecret("mail_host") + "/mail");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type","application/json");
        con.setDoOutput(true);
        con.setUseCaches(false);

        DataOutputStream dataOutputStream = new DataOutputStream(con.getOutputStream());
        dataOutputStream.writeBytes(object.toString());
        dataOutputStream.flush();
        dataOutputStream.close();

        int responseCode = con.getResponseCode();
        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Post Data : " + object);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String output;
        StringBuffer response = new StringBuffer();

        while ((output = in.readLine()) != null) {
            response.append(output);
        }
        in.close();

        //printing result from response
        System.out.println(response.toString());
    }
}
