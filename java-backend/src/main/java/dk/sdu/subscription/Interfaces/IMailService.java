package dk.sdu.subscription.Interfaces;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

public interface IMailService {

    void sendMailWithHtml(String toEmail, String subject, String htmlBody) throws MessagingException, UnsupportedEncodingException;
    void sendMailWithAttachment(String toEmail, String subject, String body, byte[] file) throws MessagingException, UnsupportedEncodingException;
}
