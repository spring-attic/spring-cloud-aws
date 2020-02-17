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

package org.springframework.cloud.aws.messaging.support.destination;

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.CreateEventBusRequest;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.naming.AmazonResourceName;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Jakub Narloch
 * @since 2.3.0
 */
public class DynamicEventBusDestinationResolver implements DestinationResolver<String> {

	private final AmazonCloudWatchEvents amazonEvents;

	private final ResourceIdResolver resourceIdResolver;

	private boolean autoCreate;

	public DynamicEventBusDestinationResolver(AmazonCloudWatchEvents amazonEvents) {
		this(amazonEvents, null);
	}

	public DynamicEventBusDestinationResolver(AmazonCloudWatchEvents amazonEvents,
			ResourceIdResolver resourceIdResolver) {
		this.amazonEvents = amazonEvents;
		this.resourceIdResolver = resourceIdResolver;
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	@Override
	public String resolveDestination(String name) throws DestinationResolutionException {
		if (autoCreate) {
			amazonEvents.createEventBus(new CreateEventBusRequest().withName(name))
					.getEventBusArn();
			return name;
		}

		String eventBusName = name;
		if (resourceIdResolver != null) {
			eventBusName = resourceIdResolver.resolveToPhysicalResourceId(name);
		}

		if (eventBusName != null
				&& AmazonResourceName.isValidAmazonResourceName(eventBusName)) {
			return AmazonResourceName.fromString(eventBusName).getResourceName();
		}

		return eventBusName;
	}

}
