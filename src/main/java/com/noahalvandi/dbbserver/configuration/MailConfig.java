package com.noahalvandi.dbbserver.configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    private final String MAIL_SENDER_USERNAME;
    private final String MAIL_SENDER_PASSWORD;

    public MailConfig() {
        Dotenv dotenv = Dotenv.configure().load();
        this.MAIL_SENDER_USERNAME = dotenv.get("MAIL_SENDER_USERNAME");
        this.MAIL_SENDER_PASSWORD = dotenv.get("MAIL_SENDER_PASSWORD");
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(MAIL_SENDER_USERNAME);
        mailSender.setPassword(MAIL_SENDER_PASSWORD);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }
}

