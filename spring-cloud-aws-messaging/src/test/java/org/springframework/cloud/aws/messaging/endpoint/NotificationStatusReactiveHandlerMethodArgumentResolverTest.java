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

package org.springframework.cloud.aws.messaging.endpoint;

import java.io.InputStreamReader;
import java.lang.reflect.Method;

import com.amazonaws.services.sns.AmazonSNS;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.BindingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Isabek Tashiev
 */
class NotificationStatusReactiveHandlerMethodArgumentResolverTest {

	@Test
	void supportsParameter_wrongMethodParameter_returnsFalse() {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationStatusReactiveHandlerMethodArgumentResolver resolver = createResolver(
				amazonSns);

		Method method = ReflectionUtils.findMethod(NotificationMethods.class,
				"handleMethod", (Class<?>[]) null);
		MethodParameter parameter = new MethodParameter(method, 0);

		// Act
		final boolean actual = resolver.supportsParameter(parameter);

		// Assert
		assertThat(actual).isFalse();
	}

	@Test
	void supportsParameter_rightMethodParameter_returnsTrue() {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationStatusReactiveHandlerMethodArgumentResolver resolver = createResolver(
				amazonSns);

		Method method = ReflectionUtils.findMethod(NotificationMethods.class,
				"subscriptionMethod", NotificationStatus.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		// Act
		final boolean actual = resolver.supportsParameter(parameter);

		// Assert
		assertThat(actual).isTrue();
	}

	@Test
	void resolveArgument_wrongMessageType_reportsErrors() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationStatusReactiveHandlerMethodArgumentResolver resolver = createResolver(
				amazonSns);

		Method method = ReflectionUtils.findMethod(NotificationMethods.class,
				"subscriptionMethod", NotificationStatus.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		final String body = FileCopyUtils.copyToString(new InputStreamReader(
				new ClassPathResource("notificationMessage.json", getClass())
						.getInputStream()));

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.post("/topic").contentType(MediaType.TEXT_PLAIN).body(body));

		// Act
		Mono<Object> mono = resolver.resolveArgument(parameter, new BindingContext(),
				exchange);

		// Assert
		assertThatThrownBy(mono::block).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(
						"NotificationStatus is only available for subscription and unsubscription requests");
	}

	@Test
	void resolveArgument_subscriptionRequest_createsValidSubscriptionStatus()
			throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationStatusReactiveHandlerMethodArgumentResolver resolver = createResolver(
				amazonSns);

		Method method = ReflectionUtils.findMethod(NotificationMethods.class,
				"subscriptionMethod", NotificationStatus.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		final String body = FileCopyUtils.copyToString(new InputStreamReader(
				new ClassPathResource("subscriptionConfirmation.json", getClass())
						.getInputStream()));

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.post("/topic").contentType(MediaType.TEXT_PLAIN).body(body));

		// Act
		Mono<Object> mono = resolver.resolveArgument(parameter, new BindingContext(),
				exchange);
		Object resolvedArgument = mono.block();

		// Assert
		assertThat(resolvedArgument).isInstanceOf(NotificationStatus.class);

		((NotificationStatus) resolvedArgument).confirmSubscription();

		verify(amazonSns, times(1)).confirmSubscription(
				"arn:aws:sns:eu-west-1:111111111111:mySampleTopic",
				"1111111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111"
						+ "11111111111111111111111111111111111");

	}

	private NotificationStatusReactiveHandlerMethodArgumentResolver createResolver(
			AmazonSNS amazonSns) {
		return new NotificationStatusReactiveHandlerMethodArgumentResolver(amazonSns);
	}

}
