package dk.sdu.subscription.ServiceLayer.InvoiceService;

import dk.sdu.subscription.Interfaces.IInvoiceService;

public class InvoiceServiceProvider {
    public static IInvoiceService getInvoiceService(){
        return new InvoiceService();
    }
}
