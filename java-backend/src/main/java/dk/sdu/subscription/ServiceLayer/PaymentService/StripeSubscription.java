package dk.sdu.subscription.ServiceLayer.PaymentService;

import com.stripe.model.Price;
import com.stripe.model.Subscription;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.ISubscription;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.SubscriptionType;

import java.util.List;
import java.util.Optional;

public class StripeSubscription implements ISubscription {
    private final Subscription subscription;
    private final List<Price> prices;
    private final String subscriptionOwner;
    public StripeSubscription(Subscription subscription, List<Price> prices, String subscriptionOwner){
        this.subscription = subscription;
        this.prices = prices;
        this.subscriptionOwner = subscriptionOwner;
    }

    public Subscription getSubscription() {
        return this.subscription;
    }

    @Override
    public String getId() {
        return this.subscription.getId();
    }

    @Override
    public String getName() {
        String name = "Nothing";
        Optional<Price> price = this.prices.stream().findFirst();
        if(price.isPresent()){
            name = price.get().getNickname();
        }
        return name;
    }

    @Override
    public long getPrice() {
        long priceValue = 0;
        Optional<Price> price = this.prices.stream().findFirst();
        if(price.isPresent()){
            priceValue = price.get().getUnitAmount();
        }
        return priceValue;
    }

    @Override
    public String getCurrency() {
        String name = "DKK";
        Optional<Price> price = this.prices.stream().findFirst();
        if(price.isPresent()){
            name = price.get().getCurrency();
        }
        return name;
    }

    @Override
    public String getStatus() {
        return this.subscription.getStatus();
    }

    @Override
    public SubscriptionType getSubscriptionType() {
        SubscriptionType subscriptionType = SubscriptionType.FREE;
        Optional<Price> price = this.prices.stream().findFirst();
        if(price.isPresent()){
            subscriptionType = SubscriptionType.getByStripePriceId(price.get().getId());
        }
        return subscriptionType;
    }

    @Override
    public long getNextRenewal() {
        return this.subscription.getCurrentPeriodEnd();
    }

    @Override
    public String getSubscriptionOwner() {
        return this.subscriptionOwner;
    }

    @Override
    public boolean isCanceled(){
        return this.subscription.getCancelAtPeriodEnd();
    }
}
