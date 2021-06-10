package com.varscon.sendcorp.SendCorp.notifications;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@AllArgsConstructor
public class EmailSenderService implements EmailSender {
    private JavaMailSender emailSender;
    private SpringTemplateEngine templateEngine;


    @Override
    @Async
    public void sendEmail(MailModel mail) {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message,
                    true,
                    StandardCharsets.UTF_8.name());

            //        helper.addAttachment("template-cover.png", new ClassPathResource("javabydeveloper-email.PNG"));
            Context context = new Context(Locale.ENGLISH);
            context.setVariables(mail.getProps());

            String html = templateEngine.process(mail.getTemplate(), context);
            helper.setTo(mail.getMailTo());
            helper.setText(html, true);
            helper.setSubject(mail.getSubject());
            helper.setFrom(mail.getFrom());
            emailSender.send(message);


        } catch (MessagingException e) {
            e.printStackTrace();
//            throw new MessagingException("Email failed", e);
        }

    }
}
