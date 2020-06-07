/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.messaging.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.aws.messaging.core.SqsMessageHeaders;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver;

/**
 * @author Wojciech Mąka
 * @since 2.2.3
 */
public class SqsHeadersMethodArgumentResolver extends HeadersMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return super.supportsParameter(parameter) || SqsMessageHeaders.class == parameter.getParameterType();
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		final Class<? extends MessageHeaders> messageHeadersClass = message.getHeaders().getClass();
		if (messageHeadersClass.getName().equals("org.springframework.messaging.support.MessageHeaderAccessor$MutableMessageHeaders")) {
			final Map<String, Object> headers = new HashMap<>();
			for (String key : message.getHeaders().keySet()) {
				headers.put(key, message.getHeaders().get(key));
			}
			return new SqsMessageHeaders(headers);
		}
		else {
			return super.resolveArgument(parameter, message);
		}
	}
}
