package dk.sdu.subscription.ServiceLayer.UserService;

import dk.sdu.subscription.Interfaces.IDatabaseService;
import dk.sdu.subscription.Interfaces.ISecretService;
import dk.sdu.subscription.Interfaces.IUserService;
import dk.sdu.subscription.ServiceLayer.DatabaseService.DatabaseService;

public class UserServiceProvider {
    public static IUserService getUserService(IDatabaseService databaseService){
        return new UserService(databaseService);
    }
}
