package dk.sdu.subscription.ServiceLayer.UserService;

import dk.sdu.subscription.Interfaces.Exceptions.UserFetchException;
import dk.sdu.subscription.Interfaces.IDatabaseService;
import dk.sdu.subscription.Interfaces.ISecretService;
import dk.sdu.subscription.Interfaces.IUser;
import dk.sdu.subscription.Interfaces.IUserService;
import dk.sdu.subscription.ServiceLayer.DatabaseService.UserData;
import dk.sdu.subscription.ServiceLayer.SecretService.SecretServiceProvider;
import org.eclipse.jetty.server.Authentication;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserService implements IUserService {

    private IDatabaseService databaseService;
    private ISecretService secretService = SecretServiceProvider.getSecretService();

    public UserService(IDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public IUser getUserFromId(String id, boolean shouldUseOwnDatabase) throws Exception {
        if(shouldUseOwnDatabase){
            return this.databaseService.getUserFromId(id);
        }
        // Replace "mock_host" with "user_host" if we want to connect to Data Collection
        URL url = new URL("http://" + secretService.getStringSecret("user_host") + "/users/" + id);
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setUseCaches(false);
            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            if (response.toString().isBlank() || response.toString().trim().equalsIgnoreCase("null")) {
                throw new UserFetchException("Could not fetch external user: User does not exist.");
            }

            //Read JSON response and print
            JSONObject jsonObject = new JSONObject(response.toString());

            IUser user = new UserData(
                    jsonObject.getString("_id"),
                    jsonObject.getString("name"),
                    jsonObject.getString("email"),
                    "role",
                    "Subtype");
            databaseService.saveUser(user);
            return user;
        }catch (JSONException e){
            throw new Exception("Could not fetch external user: Response from Data Collection is not valid JSON: " + e.getMessage());
        } catch(Exception e)  {
            if(e instanceof UserFetchException ue){
                throw ue;
            }
            throw new Exception("Could not connect to Data Collection for external users: " + e.getMessage(), e);
        }
    }
}

