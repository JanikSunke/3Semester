package dk.sdu.subscription.ServiceLayer.DatabaseService;

import dk.sdu.subscription.Interfaces.IDatabaseService;
import dk.sdu.subscription.Interfaces.ISecretService;

public class DatabaseServiceProvider {
    public static IDatabaseService getDatabaseService(ISecretService secretService){
        return new DatabaseService(secretService);
    }
}
