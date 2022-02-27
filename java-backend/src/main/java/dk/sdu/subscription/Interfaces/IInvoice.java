package dk.sdu.subscription.Interfaces;

import dk.sdu.subscription.Interfaces.Exceptions.InvoiceException;

import java.io.ByteArrayOutputStream;
import java.util.Date;

public interface IInvoice {
    int getId();

    Date getDate();

    long getTotalAmount();

    double getTax();

    String getSubtype();

    String getUserId();

    String getUserName();

    String getUserEmail();

    void saveToFile(String path) throws InvoiceException;

    ByteArrayOutputStream saveToStream() throws InvoiceException;
}
