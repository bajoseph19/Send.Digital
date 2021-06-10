package com.varscon.sendcorp.SendCorp.notifications;

import javax.mail.MessagingException;
import java.io.IOException;

public interface EmailSender {
    void sendEmail(MailModel mail);
}
