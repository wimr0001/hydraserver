package com.wimroukema.hydraserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailSenderImpl {
    @Autowired
    private JavaMailSender emailSender;
    
	@Value("${spring.mail.username}")
	private String serverUsername;
	@Value("${mailsender.to}")
	private String to;		
    
	public void sendMail(String content) {
        SimpleMailMessage message = new SimpleMailMessage(); 
        message.setFrom(serverUsername);
        message.setTo(to); 
        message.setSubject("Starten sproeien"); 
        message.setText(content);
        emailSender.send(message);
	}    
}
