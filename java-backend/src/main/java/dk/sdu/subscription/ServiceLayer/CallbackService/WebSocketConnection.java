package dk.sdu.subscription.ServiceLayer.CallbackService;

import dk.sdu.subscription.Interfaces.IDatabaseService;
import dk.sdu.subscription.Interfaces.IInvoiceService;
import dk.sdu.subscription.Interfaces.IPaymentService;
import dk.sdu.subscription.Interfaces.ISecretService;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * This example demonstrates how to create a websocket connection to a server. Only the most
 * important callbacks are overloaded.
 */
public class WebSocketConnection extends WebSocketClient {
    private final IInvoiceService invoiceService;
    private final IDatabaseService databaseService;
    private final IPaymentService paymentService;
    private final ISecretService secretService;

    public WebSocketConnection(URI serverUri, IInvoiceService invoiceService, IDatabaseService databaseService, IPaymentService paymentService, ISecretService secretService) throws URISyntaxException {
        super(serverUri);
        this.invoiceService = invoiceService;
        this.databaseService = databaseService;
        this.paymentService = paymentService;
        this.secretService = secretService;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("event", "subscribe");
        String topic = this.secretService.getStringSecret("websocket_topic");
        if(topic == null){
            topic = "subscription"; //Fallback
        }
        jsonObject.put("data", "/"+topic);
        send(String.valueOf(jsonObject));
        System.out.println("[WS] Opened connection");
        // if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
    }

    @Override
    public void onMessage(String message) {
        JSONObject jsonObject = new JSONObject(message);
        if (jsonObject.get("notification").equals(true)) {
            System.out.println("[WS] Received: " + message);
        } else {
            try{
                PaymentSuccess paymentSuccess = new PaymentSuccess(invoiceService, databaseService, paymentService);
                paymentSuccess.SendMailWhenPaymentSuccess(jsonObject.get("data").toString());
            } catch (Exception e) {
                System.out.println("[WS] Failed to process message: " + e.getMessage());
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // The codecodes are documented in class org.java_websocket.framing.CloseFrame
        System.out.println(
                "[WS] Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: "
                        + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        // if the error is fatal then onClose will be called additionally
    }
}