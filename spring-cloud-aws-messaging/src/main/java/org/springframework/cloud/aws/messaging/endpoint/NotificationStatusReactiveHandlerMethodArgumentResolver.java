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

import com.amazonaws.services.sns.AmazonSNS;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.core.MethodParameter;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * @author Isabek Tashiev
 */
public class NotificationStatusReactiveHandlerMethodArgumentResolver
		extends AbstractNotificationMessageReactiveHandlerMethodArgumentResolver {

	private final AmazonSNS amazonSns;

	public NotificationStatusReactiveHandlerMethodArgumentResolver(AmazonSNS amazonSns) {
		this.amazonSns = amazonSns;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return NotificationStatus.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	protected Object doResolveArgumentFromNotificationMessage(JsonNode body,
			MethodParameter methodParameter, ServerHttpRequest request) {
		final String type = body.get("Type").asText();

		if ("SubscriptionConfirmation".equals(type)
				|| "UnsubscribeConfirmation".equals(type)) {
			final String topicArn = body.get("TopicArn").asText();
			final String token = body.get("Token").asText();

			return new AmazonSnsNotificationStatus(this.amazonSns, topicArn, token);
		}

		throw new IllegalArgumentException(
				"NotificationStatus is only available for subscription and unsubscription requests");
	}

}
