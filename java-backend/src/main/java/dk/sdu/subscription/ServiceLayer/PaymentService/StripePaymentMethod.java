package dk.sdu.subscription.ServiceLayer.PaymentService;

import com.stripe.model.PaymentMethod;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.IPaymentMethod;

public class StripePaymentMethod implements IPaymentMethod {

    private final PaymentMethod paymentMethod;
    public StripePaymentMethod(PaymentMethod paymentMethod){
        this.paymentMethod = paymentMethod;
    }

    public PaymentMethod getPaymentMethod() {
        return this.paymentMethod;
    }

    @Override
    public int getExpireMonth() {
        return this.paymentMethod.getCard().getExpMonth().intValue();
    }

    @Override
    public int getExpireYear() {
        return this.paymentMethod.getCard().getExpYear().intValue();
    }

    @Override
    public int getLastFourDigits() {
        return Integer.parseInt(this.paymentMethod.getCard().getLast4());
    }

    @Override
    public String getCardType() {
        return this.paymentMethod.getCard().getBrand();
    }

    @Override
    public String getPaymentId() { return this.paymentMethod.getId();}
}
