package dk.sdu.subscription.ServiceLayer.CallbackService;

import dk.sdu.subscription.Interfaces.*;

public class CallbackServiceProvider {

    public static ICallbackService getCallbackService(ISecretService secretService, IInvoiceService invoiceService, IDatabaseService databaseService, IPaymentService paymentService){
        return new WebSocketCallback(secretService, invoiceService, databaseService, paymentService);
    }

}
