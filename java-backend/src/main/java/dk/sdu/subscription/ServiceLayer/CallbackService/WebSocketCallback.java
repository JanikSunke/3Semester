package dk.sdu.subscription.ServiceLayer.CallbackService;

import dk.sdu.subscription.Interfaces.*;

public class WebSocketCallback implements ICallbackService {

    private final Thread callbackThread;

    public WebSocketCallback(ISecretService secretService, IInvoiceService invoiceService, IDatabaseService databaseService, IPaymentService paymentService) {
        this.callbackThread = this.createCallbackThread(secretService, invoiceService, databaseService, paymentService);
    }

    public boolean startService(){
        if(this.callbackThread != null) {
            this.callbackThread.start();
            return true;
        }
        return false;
    }

    public boolean stopService(){
        if(this.callbackThread != null) {
            this.callbackThread.interrupt();
            return true;
        }
        return false;
    }

    private Thread createCallbackThread(ISecretService secretService, IInvoiceService invoiceService, IDatabaseService databaseService, IPaymentService paymentService){
        return new Thread(() -> {
            System.out.println("[CallbackService] Thread started.");
            while(!Thread.interrupted()) {
                try {
                    new WebSocket(secretService, invoiceService, databaseService, paymentService);
                } catch (Exception e) {
                    System.out.println("[CallbackService] Opening websocket failed: " + e.getMessage());
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    //Interrupted, how rude!!
                }
            }
            System.out.println("[CallbackService] Thread is dead.");
        });
    }
}
