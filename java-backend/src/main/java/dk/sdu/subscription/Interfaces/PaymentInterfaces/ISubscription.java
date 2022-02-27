package dk.sdu.subscription.Interfaces.PaymentInterfaces;

public interface ISubscription {
    String getId();
    String getName();
    long getPrice();
    String getCurrency();
    String getStatus();
    SubscriptionType getSubscriptionType();
    long getNextRenewal();
    String getSubscriptionOwner();
    boolean isCanceled();
}
