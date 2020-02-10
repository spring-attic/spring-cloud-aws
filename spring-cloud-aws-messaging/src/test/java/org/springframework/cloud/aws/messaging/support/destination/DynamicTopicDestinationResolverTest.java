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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicTopicDestinationResolverTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	// @checkstyle:off
	@Test
	public void resolveDestination_withNonExistentTopicAndWithoutMarkerReturnedOnListTopics_shouldThrowIllegalArgumentException()
			throws Exception {
		// @checkstyle:on
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No topic found for name :'test'");

		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().nextToken(null).build()))
				.thenReturn(ListTopicsResponse.builder().build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		resolver.resolveDestination("test");
	}

	// @checkstyle:off
	@Test
	public void resolveDestination_withNonExistentTopicAndWithMarkerReturnedOnListTopics_shouldCallListMultipleTimeWithMarkerAndThrowIllegalArgumentException()
			// @checkstyle:on
			throws Exception {
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No topic found for name :'test'");

		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().nextToken(null).build()))
				.thenReturn(ListTopicsResponse.builder().nextToken("foo").build());
		when(sns.listTopics(ListTopicsRequest.builder().nextToken("foo").build()))
				.thenReturn(ListTopicsResponse.builder().build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		resolver.resolveDestination("test");
	}

	@Test
	public void resolveDestination_withExistentTopic_returnsTopicArnFoundWhileListingTopic()
			throws Exception {
		// Arrange
		String topicArn = "arn:aws:sns:eu-west:123456789012:test";

		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().nextToken(null).build()))
				.thenReturn(ListTopicsResponse.builder()
						.topics(Topic.builder().topicArn(topicArn).build()).build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	public void resolveDestination_withExistentTopicAndMarker_returnsTopicArnFoundWhileListingTopic()
			throws Exception {
		// Arrange

		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().nextToken(null).build()))
				.thenReturn(ListTopicsResponse.builder().nextToken("mark").build());

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		when(sns.listTopics(ListTopicsRequest.builder().nextToken("mark").build()))
				.thenReturn(ListTopicsResponse.builder()
						.topics(Topic.builder().topicArn(topicArn).build()).build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	public void resolveDestination_withAlreadyExistingArn_returnsArnWithoutValidatingIt()
			throws Exception {
		// Arrange
		String topicArn = "arn:aws:sns:eu-west:123456789012:test";

		SnsClient sns = mock(SnsClient.class);
		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		String resolvedDestinationName = resolver.resolveDestination(topicArn);

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	public void resolveDestination_withAutoCreateEnabled_shouldCreateTopicDirectly()
			throws Exception {
		// Arrange
		String topicArn = "arn:aws:sns:eu-west:123456789012:test";

		SnsClient sns = mock(SnsClient.class);
		when(sns.createTopic(CreateTopicRequest.builder().name("test").build()))
				.thenReturn(CreateTopicResponse.builder().topicArn(topicArn).build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);
		resolver.setAutoCreate(true);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	public void resolveDestination_withResourceIdResolver_shouldCallIt()
			throws Exception {
		// Arrange
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:myTopic";
		String logicalTopicName = "myTopic";

		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(logicalTopicName))
				.thenReturn(physicalTopicName);

		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().nextToken(null).build()))
				.thenReturn(ListTopicsResponse.builder()
						.topics(Topic.builder().topicArn(physicalTopicName).build())
						.build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns, resourceIdResolver);

		// Assert
		String resolvedDestinationName = resolver.resolveDestination(logicalTopicName);

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(physicalTopicName);
	}

}
