package org.bch.c3pro.server.util;

import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Created by ipinyol on 9/11/15.
 */
public class Mail {

    /**
     * Send an email if 'msg' contains 'error', with subject: prefix + error and body:msg
     * @param prefix
     * @param msg
     * @param error
     */
    public static void emailIfError(String prefix, String msg, String error) {
        if (msg.contains(error)) {
            send(prefix + error, msg);
        }
    }

    /**
     * Sends an email with subject and body text to the recipient in the config.properties
     * @param subject
     * @param text
     */
    public static void send(String subject, String text) {
        Properties props = new Properties();
        InternetAddress fromAddress = null;
        InternetAddress []toAddress = null;
        try {
            props.put("mail.smtp.host", AppConfig.getProp(AppConfig.HOST_SMTP));
            props.put("mail.smtp.port", AppConfig.getProp(AppConfig.PORT_SMTP));
            fromAddress = new InternetAddress("no-reply@c3pro.chip.org.edu");
            toAddress = InternetAddress.parse(AppConfig.getProp(AppConfig.MAIL_RECEIPTIENT));
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (C3PROException e) {
            e.printStackTrace();
        }
        Session mailSession = Session.getDefaultInstance(props);
        Message simpleMessage = new MimeMessage(mailSession);

        try {
            simpleMessage.setFrom(fromAddress);
            simpleMessage.setRecipients(RecipientType.TO, toAddress);
            simpleMessage.setSubject(subject);
            simpleMessage.setText(text);
            Transport.send(simpleMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
