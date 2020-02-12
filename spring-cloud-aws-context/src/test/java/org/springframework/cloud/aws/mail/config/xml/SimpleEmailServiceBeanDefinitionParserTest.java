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

package org.springframework.cloud.aws.mail.config.xml;

import java.net.URI;

import org.junit.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.ses.SesClient;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.getBeanName;

/**
 * @author Agim Emruli
 */
public class SimpleEmailServiceBeanDefinitionParserTest {

	private static String getEndpointUrlFromWebserviceClient(SesClient client)
			throws Exception {
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
				.getField(client, "clientConfiguration");
		URI endpointUri = clientConfiguration.option(SdkClientOption.ENDPOINT);
		return endpointUri.toASCIIString();
	}

	@Test
	public void parse_MailSenderWithMinimalConfiguration_createMailSenderWithJavaMail()
			throws Exception {
		// Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-context.xml", getClass());

		// Act
		SesClient emailService = context.getBean(getBeanName(SesClient.class.getName()),
				SesClient.class);

		MailSender mailSender = context.getBean(MailSender.class);

		// Assert
		assertThat(getEndpointUrlFromWebserviceClient(emailService))
				.isEqualTo("https://email.us-west-2.amazonaws.com");

		assertThat(mailSender instanceof JavaMailSender).isTrue();
	}

	@Test
	public void parse_MailSenderWithRegionConfiguration_createMailSenderWithJavaMailAndRegion()
			throws Exception {
		// Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-region.xml", getClass());

		// Act
		SesClient emailService = context.getBean(getBeanName(SesClient.class.getName()),
				SesClient.class);

		MailSender mailSender = context.getBean(MailSender.class);

		// Assert
		assertThat(getEndpointUrlFromWebserviceClient(emailService))
				.isEqualTo("https://email.eu-west-1.amazonaws.com");

		assertThat(mailSender instanceof JavaMailSender).isTrue();
	}

	@Test
	public void parse_MailSenderWithRegionProviderConfiguration_createMailSenderWithJavaMailAndRegionFromRegionProvider()
			throws Exception {
		// Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-regionProvider.xml", getClass());

		// Act
		SesClient emailService = context.getBean(getBeanName(SesClient.class.getName()),
				SesClient.class);

		MailSender mailSender = context.getBean(MailSender.class);

		// Assert
		assertThat(getEndpointUrlFromWebserviceClient(emailService))
				.isEqualTo("https://email.ap-southeast-2.amazonaws.com");

		assertThat(mailSender instanceof JavaMailSender).isTrue();
	}

	@Test
	public void parse_MailSenderWithCustomSesClient_createMailSenderWithCustomSesClient()
			throws Exception {
		// Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-ses-client.xml", getClass());

		// Act
		SesClient emailService = context.getBean("emailServiceClient", SesClient.class);

		MailSender mailSender = context.getBean(MailSender.class);

		// Assert
		assertThat(ReflectionTestUtils.getField(mailSender, "emailService"))
				.isSameAs(emailService);
	}

}
