package dk.sdu.subscription.Interfaces;

import com.stripe.model.PaymentIntent;
import dk.sdu.subscription.Interfaces.Exceptions.CashflowException;
import dk.sdu.subscription.Interfaces.Exceptions.PaymentMethodException;
import dk.sdu.subscription.Interfaces.Exceptions.SubscriptionException;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.IPaymentMethod;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.ISubscription;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.SubscriptionType;

import java.util.List;

public interface IPaymentService {
    SubscriptionType getSubscriptionTypeForUser(IUser user) throws SubscriptionException;

    ISubscription createSubscriptionForUser(IUser user, SubscriptionType subscriptionType) throws SubscriptionException;

    void updateSubscription(ISubscription subscription, SubscriptionType subscriptionType) throws SubscriptionException;

    void cancelCancellation(ISubscription subscription) throws SubscriptionException;

    void forceCancelSubscription(ISubscription subscription) throws SubscriptionException;

    void cancelSubscription(ISubscription subscription) throws SubscriptionException;

    void addPaymentMethod(IUser user, String cardNumber, int expireMonth, int expireYear, int cvc) throws PaymentMethodException;

    void setDefaultPaymentMethod(IUser user, IPaymentMethod paymentMethod) throws PaymentMethodException;

    void removePaymentMethod(IPaymentMethod paymentMethod) throws PaymentMethodException;

    void removeAllPaymentMethods(IUser user) throws PaymentMethodException;

    ISubscription getSubscription(String id) throws SubscriptionException;

    List<ISubscription> getAllSubscriptions() throws SubscriptionException;

    IPaymentMethod getPaymentMethod(String id) throws PaymentMethodException;

    List<ISubscription> getActiveSubscriptions(IUser user) throws SubscriptionException;

    List<IPaymentMethod> getPaymentMethodsForUser(IUser user) throws PaymentMethodException;

    double getBalance() throws CashflowException;

    double getPendingIncome() throws CashflowException;

    double getExpectedIncomeFromSubscriptions() throws CashflowException;

    double getLastMonthIncomeFromSubscriptions() throws CashflowException;

    double getLastMonthIncomeSum() throws CashflowException, Exception;

    double getLastMonthIncomeFromAds() throws CashflowException, Exception;

    List<ISubscription> getAllSubscriptionsForDatabaseResync() throws SubscriptionException;

    String getUserFromStripe(String id) throws SubscriptionException;

    PaymentIntent getPaymentIntent(String id) throws SubscriptionException;
}
