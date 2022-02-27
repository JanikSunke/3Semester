package dk.sdu.subscription.Interfaces;

public interface IPaymentSuccess {
    void SendMailWhenPaymentSuccess(String payload) throws Exception;
}
