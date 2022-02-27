package dk.sdu.subscription.ServiceLayer.SecretService;

import dk.sdu.subscription.Interfaces.ISecretService;

import java.io.IOException;

public class SecretServiceProvider {

    public static ISecretService getSecretService(){
        try {
            return new SecretHandler("config.json");
        } catch (IOException e) {
            System.out.println("Failed to initialize secret manager: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }


}
