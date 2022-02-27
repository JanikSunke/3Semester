package dk.sdu.subscription.Interfaces.PaymentInterfaces;

import java.util.Arrays;
import java.util.Optional;

public enum SubscriptionType {
    FREE(0, ""),
    PREMIUM(2, "price_1JoSgrEuxKCgxj5tk6eRdjfw"),
    FAMILY(3, "price_1JoShREuxKCgxj5tyDtRV6Ap"),
    STUDENT(1, "price_1JoShhEuxKCgxj5t2utDn9si"),
    //TESTING(4,"price_1JuFEyEuxKCgxj5tgmaRg88u");
    UNKNOWN(5, "UNKNOWN_ID");

    private int subTypeId;
    private String stripePriceId;

    SubscriptionType(int subTypeId, String stripePriceId){
        this.subTypeId = subTypeId;
        this.stripePriceId = stripePriceId;
    }

    public static SubscriptionType getByStripePriceId(String stripePriceId){
        Optional<SubscriptionType> subscriptionType = Arrays.stream(SubscriptionType.class.getEnumConstants()).filter(x -> x.getStripePriceId().equals(stripePriceId)).findFirst();
        return subscriptionType.orElse(UNKNOWN);
    }

    public int getSubTypeId() {
        return subTypeId;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }
}
