package dk.sdu.subscription.Interfaces;

import dk.sdu.subscription.Interfaces.PaymentInterfaces.ISubscription;

import java.util.List;

public interface IDatabaseService {

    IUser getUserFromId(String id) throws Exception;

    IInvoiceData getInvoiceFromId(int id) throws Exception;

    List<IInvoiceData> getInvoicesFromUserId(String userid) throws Exception;

    List<IInvoiceData> getAllInvoices() throws Exception;

    List<IUser> getAllUsers() throws Exception;

    void saveInvoice(IInvoiceData invoiceData, IUser user, ISubscription sub) throws Exception;

    void saveInvoice(IInvoiceData invoiceData, IUser user, String desc) throws Exception;

    void saveUserSubscribed(IUser user, ISubscription sub) throws Exception;

    void deleteSubscription(ISubscription subscription) throws Exception;

    void updateSubscription(ISubscription subscription) throws Exception;

    void refreshSubscriptionTable(List<ISubscription> subscriptions) throws Exception;

    boolean userHasPremium(IUser user) throws Exception;

    IFamily createFamily(IUser user) throws Exception;

    boolean joinFamily(String familyId, IUser user) throws Exception;

    boolean leaveFamily(IUser user) throws Exception;

    IFamily getFamily(IUser user) throws Exception;

    void saveUser(IUser user) throws Exception;
}
