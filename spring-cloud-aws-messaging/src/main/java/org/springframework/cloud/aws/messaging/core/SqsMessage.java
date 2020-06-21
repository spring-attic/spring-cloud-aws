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

package org.springframework.cloud.aws.messaging.core;

import com.amazonaws.services.sqs.model.Message;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

public class SqsMessage extends GenericMessage<String> {

	private final Message originalMessage;

	public SqsMessage(String payload, MessageHeaders headers, Message originalMessage) {
		super(payload, headers);
		this.originalMessage = originalMessage;
	}

	public Message getOriginalMessage() {
		return originalMessage;
	}

}
