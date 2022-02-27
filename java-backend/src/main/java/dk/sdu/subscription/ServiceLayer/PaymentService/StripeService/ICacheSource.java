package dk.sdu.subscription.ServiceLayer.PaymentService.StripeService;

import com.stripe.exception.StripeException;

public interface ICacheSource<E> {
    E refreshCache() throws StripeException;
}
