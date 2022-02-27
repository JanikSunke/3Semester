package dk.sdu.subscription.Fulfillment;

import dk.sdu.subscription.ServiceLayer.MailService.MailServiceProvider;
import dk.sdu.subscription.Interfaces.IMailService;
import dk.sdu.subscription.Interfaces.ISecretService;
import dk.sdu.subscription.ServiceLayer.SecretService.SecretServiceProvider;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static spark.Spark.port;
import static spark.Spark.post;

public class MailServiceMain {
    public static void main(String[] args) {
        ISecretService secretService = SecretServiceProvider.getSecretService();
        IMailService mailService = MailServiceProvider.getMailService(secretService);

        port(5001);

        post("/mail", (req,res) -> {
            JSONObject reqBody;
            try {
                reqBody = new JSONObject(req.body());
            } catch (JSONException ex) {
                return RequestUtils.badRequest(res, "Request body is not parseable as a JSON object");
            }

            if (!reqBody.has("mailTo") || !reqBody.has("subject") || !reqBody.has("body")) {
                return RequestUtils.badRequest(res, "Request body missing parameters: `mailTo`, `subject` or `body`");
            }

            String mailTo = reqBody.getString("mailTo");
            String subject = reqBody.getString("subject");
            String body = reqBody.getString("body");
            int invoiceId = -1;
            byte[] attachmentData = null;

            try{
                invoiceId = Integer.parseInt(reqBody.getString("invoiceId"));
            }catch (Exception ignored){
            }

            if(invoiceId != -1){
                URL url = new URL(secretService.getStringSecret("api_host") + "/invoice/" + reqBody.getString("invoiceId") + "/view?user=" + secretService.getStringSecret("admin_user_id"));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
				
				con.setUseCaches(false);
                con.setRequestProperty("x-user-id", secretService.getStringSecret("admin_user_id"));
                con.setRequestMethod("GET");

                con.setDoInput(true);

                System.out.println(con.getResponseCode());
                System.out.println(con.getResponseMessage());

                InputStream inputStream = con.getInputStream();

                attachmentData = inputStream.readAllBytes();
            }

            try{
                if(invoiceId != -1 && attachmentData != null){
                    mailService.sendMailWithAttachment(mailTo, subject, body, attachmentData);
                }else{
                    mailService.sendMailWithHtml(mailTo, subject, body);
                }
            }catch (Exception ex){
                return RequestUtils.badRequest(res, "Failed with " + ex);
            }

            reqBody = new JSONObject();
            reqBody.append("status", "Mail sent successfully");
            return reqBody;
        });
    }
}
