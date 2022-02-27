package dk.sdu.subscription.ServiceLayer.InvoiceService;

import dk.sdu.subscription.Interfaces.IInvoice;
import dk.sdu.subscription.Interfaces.IInvoiceData;
import dk.sdu.subscription.Interfaces.IInvoiceService;
import dk.sdu.subscription.Interfaces.IUser;

import java.util.Date;

public class InvoiceService implements IInvoiceService {
    @Override
    public IInvoice createInvoice(int id, long totalAmount, String subtype, Date date, IUser user) {
        return new Invoice(id, totalAmount, subtype, date, user);
    }

    @Override
    public IInvoice createInvoice(long totalAmount, String subtype, Date date, IUser user) {
        return new Invoice(-1, totalAmount, subtype, date, user);
    }

    @Override
    public IInvoice createInvoice(IInvoiceData invoiceData, IUser user) {
        return new Invoice(invoiceData.getId(), invoiceData.getTotalAmount(), invoiceData.getSubtype(),invoiceData.getDate(), user);
    }
}
