/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.mail.simplemail;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.StringUtils;

/**
 * Simple MailSender implementation to send E-Mails with the Amazon Simple Email Service.
 * This implementation has no dependencies to the Java Mail API. It can be used to send
 * simple mail messages that doesn't have any attachment and therefore only consist of a
 * text body.
 *
 * @author Agim Emruli
 */
public class SimpleEmailServiceMailSender implements MailSender, DisposableBean {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SimpleEmailServiceMailSender.class);

	private final SesClient emailService;

	public SimpleEmailServiceMailSender(SesClient amazonSimpleEmailService) {
		this.emailService = amazonSimpleEmailService;
	}

	@Override
	public void send(SimpleMailMessage simpleMailMessage) throws MailException {
		send(new SimpleMailMessage[] { simpleMailMessage });
	}

	@SuppressWarnings("OverloadedVarargsMethod")
	@Override
	public void send(SimpleMailMessage... simpleMailMessages) throws MailException {

		Map<Object, Exception> failedMessages = new HashMap<>();

		for (SimpleMailMessage simpleMessage : simpleMailMessages) {
			try {
				SendEmailResponse sendEmailResult = getEmailService()
						.sendEmail(prepareMessage(simpleMessage));
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Message with id: {} successfully send",
							sendEmailResult.messageId());
				}
			}
			catch (SdkClientException e) {
				// Ignore Exception because we are collecting and throwing all if any
				// noinspection ThrowableResultOfMethodCallIgnored
				failedMessages.put(simpleMessage, e);
			}
		}

		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	@Override
	public final void destroy() throws Exception {
		getEmailService().close();
	}

	protected SesClient getEmailService() {
		return this.emailService;
	}

	private SendEmailRequest prepareMessage(SimpleMailMessage simpleMailMessage) {
		Destination.Builder destination = Destination.builder();
		destination.toAddresses(simpleMailMessage.getTo());

		if (simpleMailMessage.getCc() != null) {
			destination.ccAddresses(simpleMailMessage.getCc());
		}

		if (simpleMailMessage.getBcc() != null) {
			destination.bccAddresses(simpleMailMessage.getBcc());
		}

		Content subject = Content.builder().data(simpleMailMessage.getSubject()).build();
		Body body = Body.builder()
				.text(Content.builder().data(simpleMailMessage.getText()).build())
				.build();

		SendEmailRequest.Builder emailRequest = SendEmailRequest.builder()
				.destination(destination.build())
				.message(Message.builder().subject(subject).body(body).build())
				.source(simpleMailMessage.getFrom());

		if (StringUtils.hasText(simpleMailMessage.getReplyTo())) {
			emailRequest.replyToAddresses(simpleMailMessage.getReplyTo());
		}

		return emailRequest.build();
	}

}
