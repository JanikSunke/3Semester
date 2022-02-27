package dk.sdu.subscription.Interfaces.PaymentInterfaces;

public interface IPaymentMethod {
    int getExpireMonth();
    int getExpireYear();
    int getLastFourDigits();
    String getCardType();

    String getPaymentId();

}
