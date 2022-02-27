package dk.sdu.subscription.ServiceLayer.PaymentService;


import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import dk.sdu.subscription.Interfaces.Exceptions.CashflowException;
import dk.sdu.subscription.Interfaces.Exceptions.PaymentMethodException;
import dk.sdu.subscription.Interfaces.Exceptions.SubscriptionException;
import dk.sdu.subscription.Interfaces.IPaymentService;
import dk.sdu.subscription.Interfaces.IUser;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.IPaymentMethod;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.ISubscription;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.SubscriptionType;
import dk.sdu.subscription.ServiceLayer.PaymentService.StripeService.StripeService;

import java.util.ArrayList;
import java.util.List;

public class StripePaymentService implements IPaymentService {

    private final StripeService stripeService;

    public StripePaymentService(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @Override
    public SubscriptionType getSubscriptionTypeForUser(IUser user) throws SubscriptionException {
        try {
            return this.stripeService.getSubscriptionTypeForUser(user);
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public ISubscription createSubscriptionForUser(IUser user, SubscriptionType subscriptionType) throws SubscriptionException {
        try {
            return this.stripeService.createSubscriptionForUser(getUser(user), subscriptionType);
        } catch (IllegalArgumentException e) {
            throw new SubscriptionException(e.getMessage());
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public void updateSubscription(ISubscription subscription, SubscriptionType subscriptionType) throws SubscriptionException {
        try {
            this.stripeService.updateSubscription(((StripeSubscription) subscription).getSubscription(), subscriptionType);
        } catch (IllegalArgumentException e) {
            throw new SubscriptionException(e.getMessage());
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public void cancelCancellation(ISubscription subscription) throws SubscriptionException {
        try {
            this.stripeService.cancelCancellation(((StripeSubscription) subscription).getSubscription());
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public void forceCancelSubscription(ISubscription subscription) throws SubscriptionException {
        try {
            this.stripeService.forceCancelSubscription(((StripeSubscription) subscription).getSubscription());
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public void cancelSubscription(ISubscription subscription) throws SubscriptionException {
        try {
            this.stripeService.cancelSubscription(((StripeSubscription) subscription).getSubscription());
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public void addPaymentMethod(IUser user, String cardNumber, int expireMonth, int expireYear, int cvc) throws PaymentMethodException {
        try {
            this.stripeService.addPaymentMethod(getUser(user), cardNumber, expireMonth, expireYear, cvc);
        } catch (StripeException e) {
            throw new PaymentMethodException("Message: " + e.getMessage());
        }
    }

    @Override
    public void setDefaultPaymentMethod(IUser user, IPaymentMethod paymentMethod) throws PaymentMethodException {
        try {
            this.stripeService.setDefaultPaymentMethodForCustomer(getUser(user), ((StripePaymentMethod) paymentMethod).getPaymentMethod());
        } catch (StripeException e) {
            throw new PaymentMethodException("Message: " + e.getMessage());
        }
    }

    @Override
    public void removePaymentMethod(IPaymentMethod paymentMethod) throws PaymentMethodException {
        try {
            this.stripeService.detatchPaymentMethod(((StripePaymentMethod) paymentMethod).getPaymentMethod());
        } catch (StripeException e) {
            throw new PaymentMethodException("Message: " + e.getMessage());
        }
    }

    @Override
    public void removeAllPaymentMethods(IUser user) throws PaymentMethodException {
        try {
            this.stripeService.detatchAllPaymentMethods(getUser(user));
        } catch (StripeException e) {
            throw new PaymentMethodException("Message: " + e.getMessage());
        }
    }

    @Override
    public ISubscription getSubscription(String id) throws SubscriptionException {
        try {
            return this.stripeService.getSubscription(id);
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public List<ISubscription> getAllSubscriptions() throws SubscriptionException {
        try {
            return new ArrayList<>(this.stripeService.getAllSubscriptions());
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public IPaymentMethod getPaymentMethod(String id) throws PaymentMethodException {
        try {
            return this.stripeService.getPaymentMethod(id);
        } catch (StripeException e) {
            throw new PaymentMethodException("Message: " + e.getMessage());
        }
    }

    @Override
    public List<ISubscription> getActiveSubscriptions(IUser user) throws SubscriptionException {
            try {
            return new ArrayList<>(this.stripeService.getActiveSubscriptions(getUser(user)));
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public List<IPaymentMethod> getPaymentMethodsForUser(IUser user) throws PaymentMethodException {
        try {
            return new ArrayList<>(this.stripeService.getPaymentMethodsForUser(getUser(user)));
        } catch (StripeException e) {
            throw new PaymentMethodException("Message: " + e.getMessage());
        }
    }

    @Override
    public double getBalance() throws CashflowException {
        try {
            return this.stripeService.getBalance();
        } catch (StripeException e) {
            throw new CashflowException("Message: " + e.getMessage());
        }
    }

    @Override
    public double getPendingIncome() throws CashflowException {
        try {
            return this.stripeService.getPendingIncome();
        } catch (StripeException e) {
            throw new CashflowException("Message: " + e.getMessage());
        }
    }

    @Override
    public double getLastMonthIncomeFromSubscriptions() throws CashflowException {
        try {
            return this.stripeService.getLastMonthIncomeFromSubscriptions();
        } catch (StripeException e) {
            throw new CashflowException("Message: " + e.getMessage());
        }
    }

    @Override
    public double getLastMonthIncomeSum() throws CashflowException, Exception{
        try {
            return this.stripeService.getLastMonthIncomeSum();
        } catch (StripeException e) {
            throw new CashflowException("Message: " + e.getMessage());
        }
    }

    @Override
    public double getLastMonthIncomeFromAds() throws CashflowException, Exception {
        try {
            return this.stripeService.getAmountFromAdsService();
        } catch(StripeException e) {
            throw new CashflowException("Message: " + e.getMessage());
        }
    }

    @Override
    public double getExpectedIncomeFromSubscriptions() throws CashflowException {
        try {
            return this.stripeService.getExpectedIncomeFromSubscriptions();
        } catch (StripeException e) {
            throw new CashflowException("Message: " + e.getMessage());
        }
    }

    @Override
    public String getUserFromStripe(String id) throws SubscriptionException {
        try {
            return this.stripeService.getUserFromStripe(id);
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    @Override
    public PaymentIntent getPaymentIntent(String id) throws SubscriptionException {
        try {
            return this.stripeService.getPaymentIntents(id);
        } catch (StripeException e) {
            throw new SubscriptionException("Message: " + e.getMessage());
        }
    }

    public List<ISubscription> getAllSubscriptionsForDatabaseResync() throws SubscriptionException{
        try{
            return new ArrayList<>(this.stripeService.getAllSubscriptionsForDatabaseResync());
        }catch(StripeException e) {
            throw new SubscriptionException(e.getMessage());
        }
    }

    //PRIVATE METHODS
    private Customer getUser(IUser user) throws StripeException {
        return this.stripeService.getOrCreateCustomer(user);
    }
}
