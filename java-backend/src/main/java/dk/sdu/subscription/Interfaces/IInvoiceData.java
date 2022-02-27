package dk.sdu.subscription.Interfaces;

import java.util.Date;

public interface IInvoiceData {
    int getId();
    long getTotalAmount();
    Date getDate();
    String getSubtype();
    String getUserId();
    void setId(int id);
}
