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

/**
 * @author Agim Emruli
 */
public class AmazonSnsNotificationStatus implements NotificationStatus {

	private final AmazonSNS amazonSns;

	private final String topicArn;

	private final String token;

	public AmazonSnsNotificationStatus(AmazonSNS amazonSns, String topicArn,
			String token) {
		this.amazonSns = amazonSns;
		this.topicArn = topicArn;
		this.token = token;
	}

	@Override
	public void confirmSubscription() {
		this.amazonSns.confirmSubscription(this.topicArn, this.token);
	}

}
