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

package org.springframework.cloud.aws.jdbc.rds;

import java.util.Date;
import java.util.Map;

import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.GetUserResponse;
import software.amazon.awssdk.services.iam.model.User;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.rds.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.rds.model.Tag;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class AmazonRdsDataSourceUserTagsFactoryBeanTest {

	@Test
	public void getObject_instanceWithTagsConfiguredWithCustomResourceResolverAndCustomRegion_mapWithTagsReturned()
			throws Exception {
		// Arrange
		RdsClient rdsClient = mock(RdsClient.class);
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		IamClient amazonIdentityManagement = mock(IamClient.class);
		AmazonRdsDataSourceUserTagsFactoryBean factoryBean = new AmazonRdsDataSourceUserTagsFactoryBean(
				rdsClient, "test", amazonIdentityManagement);
		factoryBean.setResourceIdResolver(resourceIdResolver);
		factoryBean.setRegion(Region.EU_WEST_1);

		when(resourceIdResolver.resolveToPhysicalResourceId("test"))
				.thenReturn("stack-test");
		when(amazonIdentityManagement.getUser()).thenReturn(GetUserResponse.builder()
				.user(User.builder().path("/").userName("aemruli").userId("123456789012")
						.arn("arn:aws:iam::1234567890:user/aemruli")
						.createDate(new Date().toInstant()).build())
				.build());
		when(rdsClient.listTagsForResource(ListTagsForResourceRequest.builder()
				.resourceName("arn:aws:rds:eu-west-1:1234567890:db:stack-test").build()))
						.thenReturn(ListTagsForResourceResponse.builder()
								.tagList(
										Tag.builder().key("key1").value("value1").build(),
										Tag.builder().key("key2").value("value2").build())
								.build());

		// Act
		factoryBean.afterPropertiesSet();
		Map<String, String> userTagMap = factoryBean.getObject();

		// Assert
		assertThat(userTagMap.get("key1")).isEqualTo("value1");
		assertThat(userTagMap.get("key2")).isEqualTo("value2");
	}

	@Test
	public void getObject_instanceWithOutTags_emptyMapReturned() throws Exception {
		// Arrange
		RdsClient rdsClient = mock(RdsClient.class);
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		IamClient amazonIdentityManagement = mock(IamClient.class);
		AmazonRdsDataSourceUserTagsFactoryBean factoryBean = new AmazonRdsDataSourceUserTagsFactoryBean(
				rdsClient, "test", amazonIdentityManagement);
		factoryBean.setResourceIdResolver(resourceIdResolver);
		factoryBean.setResourceIdResolver(resourceIdResolver);
		factoryBean.setRegion(Region.EU_WEST_1);

		when(resourceIdResolver.resolveToPhysicalResourceId("test"))
				.thenReturn("stack-test");
		when(amazonIdentityManagement.getUser()).thenReturn(GetUserResponse.builder()
				.user(User.builder().path("/").userName("aemruli").userId("123456789012")
						.arn("arn:aws:iam::1234567890:user/aemruli")
						.createDate(new Date().toInstant()).build())
				.build());
		when(rdsClient.listTagsForResource(ListTagsForResourceRequest.builder()
				.resourceName("arn:aws:rds:eu-west-1:1234567890:db:stack-test").build()))
						.thenReturn(ListTagsForResourceResponse.builder().build());

		// Act
		factoryBean.afterPropertiesSet();
		Map<String, String> userTagMap = factoryBean.getObject();

		// Assert
		assertThat(userTagMap.isEmpty()).isTrue();
	}

	@Test
	public void getObject_instanceWithTagsAndNoResourceIdResolverAndDefaultRegion_mapWithTagsReturned()
			throws Exception {
		// Arrange
		RdsClient rdsClient = mock(RdsClient.class);
		IamClient amazonIdentityManagement = mock(IamClient.class);

		AmazonRdsDataSourceUserTagsFactoryBean factoryBean = new AmazonRdsDataSourceUserTagsFactoryBean(
				rdsClient, "test", amazonIdentityManagement);

		when(amazonIdentityManagement.getUser()).thenReturn(GetUserResponse.builder()
				.user(User.builder().path("/").userName("aemruli").userId("123456789012")
						.arn("arn:aws:iam::1234567890:user/aemruli")
						.createDate(new Date().toInstant()).build())
				.build());
		when(rdsClient.listTagsForResource(ListTagsForResourceRequest.builder()
				.resourceName("arn:aws:rds:us-west-2:1234567890:db:test").build()))
						.thenReturn(ListTagsForResourceResponse.builder()
								.tagList(
										Tag.builder().key("key1").value("value1").build(),
										Tag.builder().key("key2").value("value2").build())
								.build());

		// Act
		factoryBean.afterPropertiesSet();
		Map<String, String> userTagMap = factoryBean.getObject();

		// Assert
		assertThat(userTagMap.get("key1")).isEqualTo("value1");
		assertThat(userTagMap.get("key2")).isEqualTo("value2");
	}

}
