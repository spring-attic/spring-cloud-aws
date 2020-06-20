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

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.cloud.aws.messaging.config.annotation.NotificationSubject;
import org.springframework.core.MethodParameter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ClassUtils;

/**
 * @author Isabek Tashiev
 */
public class NotificationSubjectReactiveHandlerMethodArgumentResolver
		extends AbstractNotificationMessageReactiveHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(NotificationSubject.class)
				&& ClassUtils.isAssignable(String.class, parameter.getParameterType());
	}

	@Override
	protected Object doResolveArgumentFromNotificationMessage(JsonNode body,
			MethodParameter methodParameter, ServerHttpRequest request) {
		String type = body.get("Type").asText();
		if ("Notification".equals(type)) {
			return body.findPath("Subject").asText();
		}

		throw new IllegalArgumentException(
				"@NotificationMessage annotated parameters are only allowed for method that receive a notification message.");
	}

}
