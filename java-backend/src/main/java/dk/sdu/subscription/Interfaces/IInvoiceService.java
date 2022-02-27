package dk.sdu.subscription.Interfaces;

import java.util.Date;

public interface IInvoiceService {
    IInvoice createInvoice(int id, long totalAmount, String subtype, Date date, IUser user);
    IInvoice createInvoice(long totalAmount, String subtype, Date date, IUser user);
    IInvoice createInvoice(IInvoiceData invoiceData, IUser user);
}
