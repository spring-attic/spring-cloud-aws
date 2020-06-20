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

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Isabek Tashiev
 */
public abstract class AbstractNotificationMessageReactiveHandlerMethodArgumentResolver
		implements HandlerMethodArgumentResolver {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter,
			BindingContext bindingContext, ServerWebExchange exchange) {

		return DataBufferUtils.join(exchange.getRequest().getBody()).map(
				dataBuffer -> parseMessage(dataBuffer, parameter, exchange.getRequest()));
	}

	protected Object parseMessage(DataBuffer dataBuffer, MethodParameter methodParameter,
			ServerHttpRequest request) {
		final InputStream inputStream = dataBuffer.asInputStream();
		try {
			final JsonNode jsonNode = objectMapper.readTree(inputStream);
			return doResolveArgumentFromNotificationMessage(jsonNode, methodParameter,
					request);
		}
		catch (IOException e) {
			throw new RuntimeException("Error occurred during parsing message");
		}
	}

	protected abstract Object doResolveArgumentFromNotificationMessage(JsonNode jsonNode,
			MethodParameter methodParameter, ServerHttpRequest request);

}
