package dk.sdu.subscription.ServiceLayer.CallbackService;

import com.stripe.model.PaymentIntent;
import dk.sdu.subscription.Interfaces.*;
import dk.sdu.subscription.Fulfillment.RequestUtils;
import org.json.JSONObject;

import java.util.Date;

public class PaymentSuccess implements IPaymentSuccess{
    private final IInvoiceService invoiceService;
    private final IDatabaseService databaseService;
    private final IPaymentService paymentService;

    public PaymentSuccess(IInvoiceService invoiceService, IDatabaseService databaseService, IPaymentService paymentService){
        this.invoiceService = invoiceService;
        this.databaseService = databaseService;
        this.paymentService = paymentService;
    }

    public void SendMailWhenPaymentSuccess(String payload) throws Exception {
        if (payload.contains("payment_intent.succeeded")) {

            String start = payload.substring(payload.lastIndexOf("payment_intent\":") + 17);
            String paymentIntentId = start.substring(0, start.indexOf("\""));

            PaymentIntent paymentIntent = paymentService.getPaymentIntent(paymentIntentId);
            IUser user = databaseService.getUserFromId(paymentService.getUserFromStripe(paymentIntent.getCustomer()));

            //Saves the invoice in the invoices table
            IInvoice invoice = this.invoiceService.createInvoice(paymentIntent.getAmount(), paymentIntent.getDescription(), new Date(), user);
            databaseService.saveInvoice((IInvoiceData) invoice, user, user.getSubtype());

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("mailTo", user.getEmail());
            jsonObject.put("subject","Successful Payment: " + invoice.getId());
            jsonObject.put("body","Your payment to Music Stream Aps was successful on " + new Date());
            jsonObject.put("invoiceId",Integer.toString(invoice.getId()));

            //Sends mail with the invoice to the users email
            RequestUtils rq = new RequestUtils();
            rq.sendMail(jsonObject);
            //rq.sendMailWithInvoice(user.getEmail(), Integer.toString(invoice.getId()));
        }
    }
}
