package dk.sdu.subscription.ServiceLayer.PaymentService;

import dk.sdu.subscription.Interfaces.IPaymentService;
import dk.sdu.subscription.Interfaces.ISecretService;
import dk.sdu.subscription.ServiceLayer.PaymentService.StripeService.StripeService;

public class PaymentServiceProvider {
    public static IPaymentService getPaymentService(ISecretService secretService){
        return new StripePaymentService(new StripeService(secretService.getStringSecret("stripe_secret_key")));
    }
}
