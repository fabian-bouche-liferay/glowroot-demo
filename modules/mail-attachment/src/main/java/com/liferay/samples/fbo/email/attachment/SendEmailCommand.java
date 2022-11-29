package com.liferay.samples.fbo.email.attachment;

import com.liferay.mail.kernel.model.MailMessage;
import com.liferay.mail.kernel.service.MailService;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.activation.MailcapCommandMap;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.internet.InternetAddress;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
		property = {
				"osgi.command.function=sendMail",
				"osgi.command.scope=fbo"
		},
		service = SendEmailCommand.class
		)
public class SendEmailCommand {

	public void sendMail() {
		String from = "fabian@bouche.org";
		String to = "fabian.bouche@liferay.com";
		String subject="This is email title";
		String body="Hello World, this is my test email";

		
		try {
			MailMessage mailMessage = new MailMessage(
				    new InternetAddress(from),
				    new InternetAddress(to),
				    subject,
				    body,
				    true);
			
			InputStream is = this.getClass().getClassLoader().getResourceAsStream("META-INF/resources/sample.ics");
			
			Path path = Files.createTempFile("sample", ".ics");
			File file = path.toFile();
			
			System.out.println(file.getName());

			mailMessage.addFileAttachment(file, "sample.ics");

	        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
	        try {
	            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
	            
				MimetypesFileTypeMap mimetypes = (MimetypesFileTypeMap) MimetypesFileTypeMap.getDefaultFileTypeMap();
				mimetypes.addMimeTypes("text/calendar ics");
				
				System.out.println("Content type: " + mimetypes.getContentType(file));

				
				MailcapCommandMap mailcap = (MailcapCommandMap) MailcapCommandMap.getDefaultCommandMap();
				mailcap.addMailcap("text/calendar;; x-java-content-handler=com.sun.mail.handlers.text_plain");	            

				_mailService.sendEmail(mailMessage);
				
	        } finally {
	            Thread.currentThread().setContextClassLoader(tccl);
	        }
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Reference
	private MailService _mailService;
	
}
