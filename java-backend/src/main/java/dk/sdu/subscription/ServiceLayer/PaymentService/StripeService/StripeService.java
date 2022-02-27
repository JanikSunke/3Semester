package dk.sdu.subscription.ServiceLayer.PaymentService.StripeService;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.RequestOptions;
import com.stripe.param.*;
import dk.sdu.subscription.Interfaces.IAdsService;
import dk.sdu.subscription.Interfaces.IUser;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.SubscriptionType;
import dk.sdu.subscription.ServiceLayer.PaymentService.AdsService;
import dk.sdu.subscription.ServiceLayer.PaymentService.StripePaymentMethod;
import dk.sdu.subscription.ServiceLayer.PaymentService.StripeSubscription;

import java.time.LocalDate;
import java.util.*;

public class StripeService {
    private final static long USER_ID_CACHE_INVALIDATION_MILLIS = (15 * 60 * 1000); // 15 Minutes

    private final HashMap<String, String> userIdToCustomerIdMap;
    private final HashMap<String, String> customerIdToUserIdMap;
    private long userIdCacheLastRefresh = -1;
    private final RequestOptions requestOptions;

    //CACHES
    private final Cache<Customer> customerCache = new Cache<>(60*60*1000); // 1 hour
    private final Cache<Subscription> subscriptionCache = new Cache<>(); // 1 hour
    private final Cache<List<StripeSubscription>> activeSubscriptionsCache = new Cache<>();
    private final Cache<Double> cashflowCache = new Cache<>(60*60*12*1000); // 12 hours
    private final Cache<Double> adsIncomeCache = new Cache<>(60*60*12*1000); // 12 hours
    private final Cache<PaymentIntent> paymentIntentCache = new Cache<>(60*60*1000); // 1 hour

    public StripeService(String apiKey) {
        this.requestOptions = RequestOptions.builder().setApiKey(apiKey).build();
        this.userIdToCustomerIdMap = new HashMap<>();
        this.customerIdToUserIdMap = new HashMap<>();
        try {
            this.refreshUserCache();
        } catch (StripeException e) {
            System.out.println("Failed to make a user cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    ///////// CASHFLOW
    // <editor-fold defaultstate="collapsed" desc="CASHFLOW">
    public double getBalance() throws StripeException {
        return cashflowCache.getOrSetCache("balance", () -> {
            Balance balance = Balance.retrieve(this.requestOptions);
            return balance.getAvailable().get(0).getAmount().doubleValue() / 100;
        });
    }

    public double getPendingIncome() throws StripeException {
        return cashflowCache.getOrSetCache("pending_income", () -> {
            Balance balance = Balance.retrieve(this.requestOptions);
            return balance.getPending().get(0).getAmount().doubleValue() / 100;
        });
    }

    public double getAmountFromAdsService() throws StripeException {
        return adsIncomeCache.getOrSetCache("balance_from_ads", () -> {
            IAdsService adsService = new AdsService();
            LocalDate date = LocalDate.now();
            Calendar calendar = Calendar.getInstance();
            calendar.set(date.getYear(), date.getMonthValue() - 1, 1);
            String startDate = String.valueOf(calendar.getTimeInMillis() / 1000L);
            String today = String.valueOf(new Date().getTime() / 1000L);
            System.out.println(today);
            System.out.println(startDate);
            double amount = 0;
            try {
                amount = adsService.getAmountFromAdsTeam(startDate, today);
            } catch (Exception e) {
                System.out.println("[Ads Service] FAILING fetching amount from ads-service. Setting zero as workaround");
            }
            return amount;
        });
    }

    private BalanceTransactionCollection getAllTransactions() throws StripeException {
        //How much is earned up to the current day from the 1st
        //Sets the date to the first in the month
        LocalDate date = LocalDate.now();
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.getYear(), date.getMonthValue() - 1, 1);

        //Create parameters for the search of PaymentIntents
        Map<String, Object> createdParams = new HashMap<>();
        createdParams.put("gte", calendar.getTimeInMillis() / 1000); //Return results where the created field is greater than or equal to this value.
        Map<String, Object> params = new HashMap<>();
        params.put("created", createdParams);

        return BalanceTransaction.list(params, this.requestOptions);
    }

    public double getLastMonthIncomeSum() throws StripeException {
        return cashflowCache.getOrSetCache("last_month_income_sum", () -> {
            //How much is earned up to the current day from the 1st
            //Sets the date to the first in the month
            BalanceTransactionCollection balanceTransactions = getAllTransactions();
            double all = 0;
            for (BalanceTransaction balanceT : balanceTransactions.autoPagingIterable()) {
                all += balanceT.getNet().doubleValue();
            }
            all = all / 100;
            all += getAmountFromAdsService();
            return all;
        });
    }

    public double getLastMonthIncomeFromSubscriptions() throws StripeException {
        return cashflowCache.getOrSetCache("last_month_income_subscriptions", () -> {
            double all = 0;
            BalanceTransactionCollection balanceTransactions = getAllTransactions();
            for (BalanceTransaction balanceT : balanceTransactions.autoPagingIterable()) {
                if (balanceT.getDescription() != null && balanceT.getDescription().equals("Subscription creation")) {
                    all += balanceT.getNet().doubleValue();
                }
            }
            return all / 100;
        });
    }

    public double getExpectedIncomeFromSubscriptions() throws StripeException {
        return cashflowCache.getOrSetCache("expected_income_from_subscriptions", () -> {
            SubscriptionCollection subscriptions = Subscription.list(new HashMap<>(), this.requestOptions);

            double all = 0;
            for (Subscription sub : subscriptions.autoPagingIterable()) {
                all += sub.getItems().getData().get(0).getPrice().getUnitAmount();
            }

            // Divide by 100 to change from Øre to Kroner
            return all / 100;
        });
    }
    // </editor-fold>

    ///////// CUSTOMERS
    // <editor-fold defaultstate="collapsed" desc="CUSTOMERS">
    public String getUserFromStripe(String id) throws StripeException {
        checkUserCache();
        System.out.println(customerIdToUserIdMap);
        return this.customerIdToUserIdMap.get(id);
    }

    public Customer getOrCreateCustomer(IUser user) throws StripeException {
        checkUserCache();
        Customer customer = null;
        if (this.userIdToCustomerIdMap.containsKey(user.getId())) {
            customer = customerCache.getOrSetCache(user.getId(), () -> Customer.retrieve(this.userIdToCustomerIdMap.get(user.getId()), this.requestOptions));
        } else {
            //User does not exist as a Stripe Customer. Creating
            System.out.println("Creating new Stripe Customer from: " + user);
            Map<String, String> newCustomerMetadata = new HashMap<>();
            newCustomerMetadata.put(StripeConstants.USER_ID_METADATA, user.getId());
            customer = Customer.create(CustomerCreateParams.builder()
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .setMetadata(newCustomerMetadata)
                    .build(), this.requestOptions);
            this.userIdToCustomerIdMap.put(user.getId(), customer.getId());
            this.customerIdToUserIdMap.put(customer.getId(), user.getId());
        }
        return customer;
    }

    public SubscriptionType getSubscriptionTypeForUser(IUser user) throws StripeException {
        return getSubscriptionTypeForCustomer(getOrCreateCustomer(user));
    }

    /**
     * Finds the highest type of subscription a customer should have.
     *
     * @param customer
     * @return
     * @throws StripeException
     */
    public SubscriptionType getSubscriptionTypeForCustomer(Customer customer) throws StripeException {
        List<StripeSubscription> activeSubscriptions = getActiveSubscriptions(customer);
        long highestPrice = -1;
        SubscriptionType highestSubscriptionType = SubscriptionType.FREE;
        for (StripeSubscription stripeSubscription : activeSubscriptions) {
            Subscription subscription = stripeSubscription.getSubscription();
            if (Objects.equals(subscription.getStatus(), StripeConstants.PAYMENT_STATUS_ACTIVE)
                    || Objects.equals(subscription.getStatus(), StripeConstants.PAYMENT_STATUS_PENDING)) {
                Optional<SubscriptionItem> item = subscription.getItems().getData().stream().findFirst();
                if (item.isPresent()) {
                    long price = item.get().getPrice().getUnitAmount();
                    if (price > highestPrice) {
                        highestSubscriptionType = SubscriptionType.getByStripePriceId(item.get().getPrice().getId());
                        highestPrice = price;
                    }
                }
            }
        }
        return highestSubscriptionType;
    }
    // </editor-fold>

    ///////// SUBSCRIPTIONS
    // <editor-fold defaultstate="collapsed" desc="SUBSCRIPTIONS">

    public StripeSubscription getSubscription(String id) throws StripeException {
        Subscription subscription = subscriptionCache.getOrSetCache(id, () -> Subscription.retrieve(id, this.requestOptions));
        return new StripeSubscription(subscription, getPricesFromSubscription(subscription), this.customerIdToUserIdMap.get(subscription.getCustomer()));
    }

    public StripeSubscription createSubscriptionForUser(Customer customer, SubscriptionType subscriptionType) throws StripeException {
        if (subscriptionType == SubscriptionType.FREE) {
            throw new IllegalArgumentException("You cannot create a FREE subscription, as that is the default.");
        }
        List<Object> items = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("price", subscriptionType.getStripePriceId());
        items.add(item1);
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customer.getId());
        params.put("payment_behavior", "error_if_incomplete");
        params.put("items", items);

        Subscription subscription = Subscription.create(params, this.requestOptions);
        customerUpdated(customer);
        return new StripeSubscription(subscription, this.getPricesFromSubscription(subscription), this.customerIdToUserIdMap.get(customer.getId()));
    }

    public List<StripeSubscription> getActiveSubscriptions(Customer customer) throws StripeException {
        return activeSubscriptionsCache.getOrSetCache(customer.getId(), () -> {
            List<StripeSubscription> subscriptionList = new ArrayList<>();
            SubscriptionCollection subscriptions = Subscription.list(new HashMap<>(), requestOptions);
            if (subscriptions == null) {
                return subscriptionList;
            }

            for (Subscription subscription : subscriptions.autoPagingIterable()) {
                if (subscription.getCustomer().equals(customer.getId()))
                    subscriptionList.add(new StripeSubscription(subscription, getPricesFromSubscription(subscription), customerIdToUserIdMap.get(subscription.getCustomer())));
            }
            return subscriptionList;
        });
    }

    public void updateSubscription(Subscription subscription, SubscriptionType subscriptionType) throws StripeException {
        if (subscriptionType == SubscriptionType.FREE) {
            throw new IllegalArgumentException("You cannot update the subscription to FREE. Cancel the subscription instead.");
        }
        Optional<SubscriptionItem> subscriptionItem = subscription.getItems().getData().stream().findFirst();

        if (subscriptionItem.isEmpty()) {
            throw new IllegalArgumentException("The subscription provided does not have an any subscription items. Cancel the subscription and create a new one.");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("price", subscriptionType.getStripePriceId());
        subscriptionItem.get().update(params, this.requestOptions);
        subscriptionUpdated(subscription);
        customerUpdated(subscription.getCustomerObject());
    }

    public void cancelCancellation(Subscription subscription) throws StripeException {
        if (subscription.getCancelAtPeriodEnd()) {
            subscription.update(SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(false).build(), this.requestOptions);
            subscriptionUpdated(subscription);
        }
    }

    public void cancelSubscription(Subscription subscription) throws StripeException {
        if (!subscription.getCancelAtPeriodEnd()) {
            subscription.update(SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build(), this.requestOptions);
            subscriptionUpdated(subscription);
        }
    }

    public void forceCancelSubscription(Subscription subscription) throws StripeException {
        subscription.cancel((HashMap<String, Object>) null, this.requestOptions);
        subscriptionUpdated(subscription);
    }

    public void cancelAllSubscriptions(Customer customer) throws StripeException {
        for (StripeSubscription subscription : getActiveSubscriptions(customer)) {
            this.cancelSubscription(subscription.getSubscription());
        }
        customerUpdated(customer);
    }

    public void forceCancelAllSubscriptions(Customer customer) throws StripeException {
        for (StripeSubscription subscription : getActiveSubscriptions(customer)) {
            this.forceCancelSubscription(subscription.getSubscription());
        }
        customerUpdated(customer);
    }

    public List<StripeSubscription> getAllSubscriptions() throws StripeException {
        return activeSubscriptionsCache.getOrSetCache("ALL_SUBSCRIPTIONS", () -> {
            PriceCollection prices = Price.list(PriceListParams.builder().setProduct(StripeConstants.SUBSCRTIPTION_PRODUCT_ID).build(), requestOptions);
            List<StripeSubscription> stripeSubscriptionList = new ArrayList<>();
            for (Price price : prices.autoPagingIterable()) {
                stripeSubscriptionList.add(new StripeSubscription(null, List.of(price), ""));
            }
            return stripeSubscriptionList;
        });
    }

    public List<StripeSubscription> getAllSubscriptionsForDatabaseResync() throws StripeException {
        //Caching is not implemented here on purpose.
        userIdCacheLastRefresh = -1; //Force refresh
        refreshUserCache();
        SubscriptionCollection subscriptions = Subscription.list(new HashMap<>(), this.requestOptions);
        List<StripeSubscription> stripeSubscriptionList = new ArrayList<>();
        for (Subscription subscription : subscriptions.autoPagingIterable()) {
            if (!customerIdToUserIdMap.containsKey(subscription.getCustomer())) {
                continue;
            }
            String userId = customerIdToUserIdMap.get(subscription.getCustomer());
            List<Price> priceList = getPricesFromSubscription(subscription);
            stripeSubscriptionList.add(new StripeSubscription(subscription, priceList, userId));
        }
        return stripeSubscriptionList;
    }

    public List<Price> getPricesFromSubscription(Subscription subscription) {
        List<Price> prices = new ArrayList<>();
        for (SubscriptionItem subscriptionItem : subscription.getItems().autoPagingIterable()) {
            prices.add(subscriptionItem.getPrice());
        }
        return prices;
    }
    // </editor-fold>

    ///////// PAYMENT METHODS
    // <editor-fold defaultstate="collapsed" desc="PAYMENT METHODS">

    public List<StripePaymentMethod> getPaymentMethodsForUser(Customer customer) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "card");
        PaymentMethodCollection paymentMethods = customer.listPaymentMethods(params, this.requestOptions);
        List<StripePaymentMethod> paymentMethodList = new ArrayList<>();
        for (PaymentMethod paymentMethod : paymentMethods.autoPagingIterable()) {
            paymentMethodList.add(new StripePaymentMethod(paymentMethod));
        }
        return paymentMethodList;
    }

    public StripePaymentMethod getPaymentMethod(String paymentMethodId) throws StripeException {
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId, this.requestOptions);
        return new StripePaymentMethod(paymentMethod);
    }

    public void addPaymentMethod(Customer customer, String cardNumber, int expireMonth, int expireYear, int cvc) throws StripeException {
        Map<String, Object> card = new HashMap<>();
        card.put("number", cardNumber);
        card.put("exp_month", expireMonth);
        card.put("exp_year", expireYear);
        card.put("cvc", cvc);
        Map<String, Object> cardParams = new HashMap<>();
        cardParams.put("type", "card");
        cardParams.put("card", card);
        PaymentMethod paymentMethod = PaymentMethod.create(cardParams, this.requestOptions);

        // Card have been created, attach to the customer
        Map<String, Object> customerAttachParams = new HashMap<>();
        customerAttachParams.put("customer", customer.getId());
        paymentMethod.attach(customerAttachParams, this.requestOptions);
        this.setDefaultPaymentMethodForCustomer(customer, paymentMethod);
    }

    public void setDefaultPaymentMethodForCustomer(Customer customer, PaymentMethod paymentMethod) throws StripeException {
        customer.update(CustomerUpdateParams.builder()
                .setInvoiceSettings(CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethod.getId()).build())
                .build(), this.requestOptions);
    }

    public void detatchPaymentMethod(PaymentMethod paymentMethod) throws StripeException {
        paymentMethod.detach(this.requestOptions);
    }

    public void detatchAllPaymentMethods(Customer customer) throws StripeException {
        for (StripePaymentMethod paymentMethod : getPaymentMethodsForUser(customer)) {
            this.detatchPaymentMethod(paymentMethod.getPaymentMethod());
        }
    }
    // </editor-fold>

    ///////// PAYMENT_INTENTS
    // <editor-fold defaultstate="collapsed" desc="PAYMENT_INTENTS">
    public PaymentIntent getPaymentIntents(String id) throws StripeException {
        return paymentIntentCache.getOrSetCache(id, () -> PaymentIntent.retrieve(id, this.requestOptions));
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Private Methods">

    /**
     * Makes sure that the user cache is valid and up-to-date.
     *
     * @throws StripeException
     */
    private void checkUserCache() throws StripeException {
        if (userIdToCustomerIdMap.isEmpty() || userIdCacheLastRefresh + USER_ID_CACHE_INVALIDATION_MILLIS < System.currentTimeMillis()) {
            refreshUserCache();
        }
    }

    /**
     * Refreshes the user cache that maps our user-ids to Stripes customer-ids
     *
     * @throws StripeException
     */
    private void refreshUserCache() throws StripeException {
        System.out.println("Refreshing User cache...");
        this.userIdToCustomerIdMap.clear();
        this.customerIdToUserIdMap.clear();
        CustomerCollection customers = Customer.list(CustomerListParams.builder().build(), this.requestOptions);
        for (Customer customer : customers.autoPagingIterable()) {
            if (!customer.getMetadata().containsKey(StripeConstants.USER_ID_METADATA)) {
                //Customer does not have a user_id metadata attached. Skipping
                continue;
            }
            String customerId = customer.getMetadata().get(StripeConstants.USER_ID_METADATA);
            this.userIdToCustomerIdMap.put(customerId, customer.getId());
            this.customerIdToUserIdMap.put(customer.getId(), customerId);
        }
        this.userIdCacheLastRefresh = System.currentTimeMillis();
    }

    /**
     * Can be called to signal that a subscription has been updated, and the cache for set subscription should thus be reset.
     * @param subscription Subscription updated
     */
    private void subscriptionUpdated(Subscription subscription){
        if(subscription != null) {
            subscriptionCache.clear(subscription.getId());
            activeSubscriptionsCache.clear(subscription.getCustomer());
            cashflowCache.clear("expected_income_from_subscriptions");
            cashflowCache.clear("pending_income");
        }
    }

    /**
     * Can be called to signal that a customer has been updated, and the cache for set customer should be reset
     * @param customer Customer updated
     */
    private void customerUpdated(Customer customer){
        if(customer != null)
            customerCache.clear(customerIdToUserIdMap.getOrDefault(customer.getId(), ""));
    }
    // </editor-fold>
}
