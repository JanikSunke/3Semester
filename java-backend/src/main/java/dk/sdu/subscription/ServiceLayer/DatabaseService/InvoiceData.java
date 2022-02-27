package dk.sdu.subscription.ServiceLayer.DatabaseService;

import dk.sdu.subscription.Interfaces.IInvoiceData;

import java.util.Date;

public class InvoiceData implements IInvoiceData {

    private int id;
    private Date date;
    private long totalAmount;
    private String subtype;
    private String userId;

    // <editor-fold defaulstate="collapsed" desc="Getters / Setters">
    public int getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

// </editor-fold>

    public InvoiceData(int id, long totalAmount, String subtype, Date date, String userId) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.subtype = subtype;
        this.date = date;
        this.userId = userId;
    }
}
