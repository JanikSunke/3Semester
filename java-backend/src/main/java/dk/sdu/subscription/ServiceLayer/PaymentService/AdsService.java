package dk.sdu.subscription.ServiceLayer.PaymentService;

import com.stripe.exception.StripeException;
import dk.sdu.subscription.Interfaces.IAdsService;
import dk.sdu.subscription.Interfaces.ISecretService;
import dk.sdu.subscription.ServiceLayer.SecretService.SecretServiceProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public class AdsService implements IAdsService {

    ISecretService secretService = SecretServiceProvider.getSecretService();

    public AdsService() {

    }

    @Override
    public double getAmountFromAdsTeam(String startDate, String endDate) throws Exception {
        // Replace "mock_host" with "user_host" if we want to connect to Data Collection
        URL url = new URL("http://" + secretService.getStringSecret("mock_host") + "/ads/");
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            String jsonInputString = "{\"from\":" + startDate + ", \"to\":" + endDate+"}";
            try(OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            System.out.println(jsonInputString);
            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            //print in String
            System.out.println(response.toString());
            //Read JSON response and print
            JSONObject jsonObject = new JSONObject(response.toString());
            System.out.println("result after Reading JSON Response");
            JSONArray invoiceList = jsonObject.getJSONArray("invoices");
            double amount = 0;
            for(int i = 0; i < invoiceList.length(); i++) {
                JSONObject objectInArray = invoiceList.getJSONObject(i);
                amount += objectInArray.getDouble("amount");

            }
            double USDtoDKK = 6.57;
            amount += amount*USDtoDKK;
            return amount;
        } catch(Exception e)  {
            throw new Exception("Could not connect to Ads Team for external money: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws Exception {
        AdsService ads = new AdsService();
        double money = ads.getAmountFromAdsTeam("1636456223", "1639048223");
        System.out.println(money);
    }
}
