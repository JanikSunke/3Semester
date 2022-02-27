package dk.sdu.subscription.ServiceLayer.CallbackService;

import dk.sdu.subscription.Interfaces.*;

import java.net.URI;

/**
 * This example demonstrates how to create a websocket connection to a server. Only the most
 * important callbacks are overloaded.
 */
public class WebSocket {
    private final String uri;
    private final ISecretService secretService;
    private final IInvoiceService invoiceService;
    private final IDatabaseService databaseService;
    private final IPaymentService paymentService;
    private WebSocketConnection webSocketConnection;
    private boolean reConnecting = false;

    public WebSocket(ISecretService secretService, IInvoiceService invoiceService, IDatabaseService databaseService, IPaymentService paymentService) throws Exception {
        this.invoiceService = invoiceService;
        this.secretService = secretService;
        this.databaseService = databaseService;
        this.paymentService = paymentService;
        this.uri = "ws://" + secretService.getStringSecret("websocket_host") + ":" + secretService.getStringSecret("websocket_port");
        connect();
        monitor();
    }

    private void monitor() throws Exception {
        reConnecting = false;
        while(!Thread.interrupted()) {
            heartBeat();
        }
        webSocketConnection.close();
    }

    private void connect() throws Exception {
        System.out.println("[WS] Connecting to websocket callback: " + this.uri);
        webSocketConnection = new WebSocketConnection(
                new URI(uri), invoiceService ,databaseService, paymentService,secretService);
        webSocketConnection.connect();
    }

    private void heartBeat() throws Exception {
        try {
            Thread.sleep(1000*60*1);
        } catch (InterruptedException e) {
            //Interrupted: How rude!
        }
        if (!reConnecting) {
            try {
                System.out.println("[WS] Sending ping...");
                webSocketConnection.sendPing();
            } catch (Exception e) {
                reConnecting = true;
                reconnect();
            }
        }
    }

    private void reconnect() throws Exception{
        if (reConnecting) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //Interrupted: How rude!
            }
            System.out.println("[WS] Reconnecting...");
            webSocketConnection = null;
            connect();
        }
    }


}