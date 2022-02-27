package dk.sdu.subscription.Fulfillment;

import dk.sdu.subscription.Interfaces.*;
import dk.sdu.subscription.ServiceLayer.CallbackService.CallbackServiceProvider;
import dk.sdu.subscription.ServiceLayer.InvoiceService.InvoiceServiceProvider;
import dk.sdu.subscription.ServiceLayer.DatabaseService.DatabaseServiceProvider;
import dk.sdu.subscription.ServiceLayer.PaymentService.PaymentServiceProvider;
import dk.sdu.subscription.ServiceLayer.SecretService.SecretServiceProvider;
import dk.sdu.subscription.ServiceLayer.CallbackService.WebSocket;
import dk.sdu.subscription.ServiceLayer.UserService.UserServiceProvider;
import spark.Filter;

import java.util.Arrays;
import java.util.List;

import static spark.Spark.*;

public class SubscriptionMain {
    private static final String USER_ID_HEADER = "x-user-id";

    public static void main(String[] args) {
        ISecretService secretService = SecretServiceProvider.getSecretService();
        IPaymentService paymentService = PaymentServiceProvider.getPaymentService(secretService);
        IDatabaseService databaseService = DatabaseServiceProvider.getDatabaseService(secretService);
        IInvoiceService invoiceService = InvoiceServiceProvider.getInvoiceService();
        ICallbackService callbackService = CallbackServiceProvider.getCallbackService(secretService, invoiceService, databaseService, paymentService);
        IUserService userService = UserServiceProvider.getUserService(databaseService);
        callbackService.startService();
        port(5000);
        staticFileLocation("/public");

        before(checkPreconditions());
        get("/", (req, res) -> "<h1>Subscription Team API</h1>See the documentation at: <a href='https://docs.google.com/document/d/1bNwJncgJM2UhxJpg3mMy3KbwKhWtYMMKhEPPwkBwb5c/edit'>https://docs.google.com/document/d/1bNwJncgJM2UhxJpg3mMy3KbwKhWtYMMKhEPPwkBwb5c/edit</a>");
        get("/hello", (req, res) -> "Spark virker");
        path("/payments", new PaymentController(paymentService, databaseService, userService));
        path("/family", new FamilyController(databaseService, userService));
        path("/subscription", new SubscriptionController(paymentService, databaseService, invoiceService, userService));
        path("/invoice", new InvoiceController(invoiceService,databaseService, paymentService, userService));
        path("/cashflow", new CashFlowController(paymentService, databaseService, userService));
    }

    private static final List<String> EXCLUDED_PRECONDITION_ENDPOINTS = Arrays.asList("\\/invoice\\/\\d+?\\/view", "\\/hello", "^\\/$");

    private static Filter checkPreconditions() {
        return (request, response) -> {
            String uri = request.uri();
            for (String pattern : EXCLUDED_PRECONDITION_ENDPOINTS) {
                if (uri.matches(pattern)) {
                    return;
                }
            }

            String userId = request.headers(USER_ID_HEADER);
            if (userId == null || userId.isBlank()) {
                halt(412, RequestUtils.preconditionFailed(response, "No " + USER_ID_HEADER + " header found."));
            }

            request.attribute("userId", userId);
        };
    }
}
