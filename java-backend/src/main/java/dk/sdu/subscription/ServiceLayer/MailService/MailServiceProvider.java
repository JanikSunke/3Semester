package dk.sdu.subscription.ServiceLayer.MailService;

import dk.sdu.subscription.Interfaces.IMailService;
import dk.sdu.subscription.Interfaces.ISecretService;

public class MailServiceProvider {
    public static IMailService getMailService(ISecretService secretService){
        return new MailService(secretService);
    }
}
