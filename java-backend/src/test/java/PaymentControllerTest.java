
import static org.junit.jupiter.api.Assertions.*;

import dk.sdu.subscription.Interfaces.IDatabaseService;
import dk.sdu.subscription.Interfaces.IPaymentService;
import dk.sdu.subscription.Interfaces.ISecretService;
import dk.sdu.subscription.Interfaces.IUser;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.IPaymentMethod;
import dk.sdu.subscription.ServiceLayer.SecretService.SecretServiceProvider;
import dk.sdu.subscription.ServiceLayer.DatabaseService.DatabaseServiceProvider;
import dk.sdu.subscription.ServiceLayer.PaymentService.PaymentServiceProvider;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class PaymentControllerTest {

    private ISecretService secretService;
    private IPaymentService paymentService;
    private IDatabaseService databaseService;
    private IPaymentMethod paymentMethod;

    @BeforeEach
    void setUp(){
        secretService = SecretServiceProvider.getSecretService();
        paymentService = PaymentServiceProvider.getPaymentService(secretService);
        databaseService = DatabaseServiceProvider.getDatabaseService(secretService);
        //paymentMethod = StripePaymentMethod.getPaymentMethod();
    }

    @Test
    @DisplayName("Create Payment Method")
    void testCreatePaymentMethod() throws Exception {
        IUser user = databaseService.getUserFromId("unit-test");
        String cardNumber = "4242424242424242";
        String expireMonth = "5";
        String expireYear = "2025";
        String cvc = "123";
        String paymentMethod = "card";
        try {


            paymentService.removeAllPaymentMethods(user);


            JSONObject testJason = new JSONObject();
            testJason.put("cardNumber", cardNumber);
            testJason.put("expireMonth", expireMonth);
            testJason.put("expireYear", expireYear);
            testJason.put("cvc", cvc);
            testJason.put("card", "card");


            URL url = new URL("http://" + secretService.getStringSecret("api_host") + ":" + secretService.getStringSecret("api_port") + "/payments/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("X-USER-ID", "unit-test");
            con.setRequestProperty("X-TEST-DB", "1");
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            OutputStreamWriter dataOutputStream = new OutputStreamWriter(con.getOutputStream());
            dataOutputStream.write(testJason.toString());
            dataOutputStream.flush();
            dataOutputStream.close();
            con.getOutputStream().close();
            con.connect();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'POST' request to URL : " + url);
            System.out.println("Post Data : " + testJason);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String output;
            StringBuffer response = new StringBuffer();

            while ((output = in.readLine()) != null) {
                response.append(output);
            }
            in.close();

            //printing result from response
            System.out.println(response.toString());

            //paymentService.addPaymentMethod(user, cardNumber, expireMonth, expireYear, cvc);

            List<IPaymentMethod> testList = paymentService.getPaymentMethodsForUser(user);
            assertEquals(Integer.parseInt(cardNumber.substring(12, 16)), testList.get(0).getLastFourDigits());
            assertEquals(Integer.parseInt(expireMonth), testList.get(0).getExpireMonth());
            assertEquals(Integer.parseInt(expireYear), testList.get(0).getExpireYear());
            System.out.println("Payment method test successful");


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            paymentService.removeAllPaymentMethods(user);
        }


    }
}