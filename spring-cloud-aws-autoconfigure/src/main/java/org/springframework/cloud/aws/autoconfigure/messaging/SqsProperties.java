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

package org.springframework.cloud.aws.autoconfigure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;

/**
 * Properties related to SQS integration.
 *
 * @author Maciej Walkowiak
 */
@ConfigurationProperties("cloud.aws.sqs")
public class SqsProperties {

	private ListenerProperties listener = new ListenerProperties();

	private HandlerProperties handler = new HandlerProperties();

	public ListenerProperties getListener() {
		return listener;
	}

	public void setListener(ListenerProperties listener) {
		this.listener = listener;
	}

	public HandlerProperties getHandler() {
		return handler;
	}

	public void setHandler(HandlerProperties handler) {
		this.handler = handler;
	}

	public static class ListenerProperties {

		private Integer maxNumberOfMessages = 10;

		private Integer visibilityTimeout;

		private Integer waitTimeOut = 20;

		private Long queueStopTimeout;

		private Long backOffTime;

		private boolean autoStartup = true;

		public Integer getMaxNumberOfMessages() {
			return maxNumberOfMessages;
		}

		public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
			this.maxNumberOfMessages = maxNumberOfMessages;
		}

		public Integer getVisibilityTimeout() {
			return visibilityTimeout;
		}

		public void setVisibilityTimeout(Integer visibilityTimeout) {
			this.visibilityTimeout = visibilityTimeout;
		}

		public Integer getWaitTimeOut() {
			return waitTimeOut;
		}

		public void setWaitTimeOut(Integer waitTimeOut) {
			this.waitTimeOut = waitTimeOut;
		}

		public Long getQueueStopTimeout() {
			return queueStopTimeout;
		}

		public void setQueueStopTimeout(Long queueStopTimeout) {
			this.queueStopTimeout = queueStopTimeout;
		}

		public Long getBackOffTime() {
			return backOffTime;
		}

		public void setBackOffTime(Long backOffTime) {
			this.backOffTime = backOffTime;
		}

		public boolean isAutoStartup() {
			return autoStartup;
		}

		public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

	}

	public static class HandlerProperties {

		private SqsMessageDeletionPolicy defaultDeletionPolicy;

		public SqsMessageDeletionPolicy getDefaultDeletionPolicy() {
			return defaultDeletionPolicy;
		}

		public void setDefaultDeletionPolicy(
				SqsMessageDeletionPolicy defaultDeletionPolicy) {
			this.defaultDeletionPolicy = defaultDeletionPolicy;
		}

	}

}
