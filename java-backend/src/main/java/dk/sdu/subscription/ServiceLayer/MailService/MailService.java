package dk.sdu.subscription.ServiceLayer.MailService;

import dk.sdu.subscription.Interfaces.IMailService;
import dk.sdu.subscription.Interfaces.ISecretService;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class MailService implements IMailService {

    private Properties props = new Properties();
    private String sendingMail;
    private String password;
    private Session session;


    public MailService(ISecretService secretService) {
        sendingMail = secretService.getStringSecret("mail_sender");
        password = secretService.getStringSecret("mail_password");
        props.put("mail.smtp.host", secretService.getStringSecret("smtp_host"));
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        Authenticator auth = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sendingMail, password);
            }
        };
        session = Session.getInstance(props, auth);
    }

    public void sendMailWithHtml(String toEmail, String subject, String htmlBody) throws MessagingException, UnsupportedEncodingException {
        Multipart tempMultiPart = new MimeMultipart("alternative");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("", "UTF-8");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=utf-8");

        tempMultiPart.addBodyPart(textPart);
        tempMultiPart.addBodyPart(htmlPart);
        EmailUtil.sendEmailWithMultipart(session, sendingMail, toEmail, subject, tempMultiPart);
    }

    public void sendMailWithAttachment(String toEmail, String subject, String body, byte[] file) throws MessagingException, UnsupportedEncodingException {
        Multipart tempMultiPart = new MimeMultipart("alternative");
        MimeBodyPart tempPart = new MimeBodyPart();
        tempPart.setText("", "UTF-8");
        tempMultiPart.addBodyPart(tempPart);
        tempPart = new MimeBodyPart();
        tempPart.setContent(body, "text/html; charset=utf-8");
        tempMultiPart.addBodyPart(tempPart);
        tempPart = new MimeBodyPart();
        DataSource source = new ByteArrayDataSource(file, "application/pdf");
        tempPart.setDataHandler(new DataHandler(source));
        tempPart.setFileName("invoice.pdf");
        tempMultiPart.addBodyPart(tempPart);

        EmailUtil.sendEmailWithMultipart(session, sendingMail, toEmail, subject, tempMultiPart);
    }
}
