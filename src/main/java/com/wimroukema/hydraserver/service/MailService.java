package com.wimroukema.hydraserver.service;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class MailService {
	
	@Autowired
    private static JavaMailSender emailSender;
	
	@Value("${spring.mail.host}")
	private String host;
	@Value("${spring.mail.port}")
	private String port;		
	@Value("${spring.mail.username}")
	private String serverUsername;	
	@Value("${spring.mail.password}")
	private String serverPassword;		
	@Value("${spring.mail.transport.protocol}")
	private String protocol;	
	@Value("${spring.mail.smtp.auth}")
	private boolean auth;	
	@Value("${spring.mail.smtp.starttls.enable}")
	private boolean starttls;
	@Value("${mailsender.mail.debug}")
	private boolean debug;	


	@Bean
	public JavaMailSender getJavaMailSender() {
	    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
	    mailSender.setHost(host);
	    mailSender.setPort(Integer.valueOf(port));
	    
	    mailSender.setUsername(serverUsername);
	    mailSender.setPassword(serverPassword);
	    
	    Properties props = mailSender.getJavaMailProperties();
	    props.put("mail.transport.protocol", protocol);
	    props.put("mail.smtp.auth", auth);
	    props.put("mail.smtp.starttls.enable", starttls);
	    props.put("mail.debug", debug);
	    
	    return mailSender;
	}
}
