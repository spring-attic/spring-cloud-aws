/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.autoconfigure.context;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.TagDescription;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.aws.context.annotation.OnAwsCloudEnvironmentCondition;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContextStackAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@Before
	public void restContextInstanceDataCondition() throws IllegalAccessException {
		Field field = ReflectionUtils.findField(OnAwsCloudEnvironmentCondition.class, "isCloudEnvironment");
		assertNotNull(field);
		ReflectionUtils.makeAccessible(field);
		field.set(null, null);
	}

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void stackRegistry_autoConfigurationEnabled_returnsAutoConfiguredStackRegistry() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(AutoConfigurationStackRegistryTestConfiguration.class);
		this.context.register(ContextStackAutoConfiguration.class);
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext httpContext = httpServer.createContext("/latest/meta-data/instance-id", new MetaDataServer.HttpResponseWriterHandler("test"));

		//Act
		this.context.refresh();

		//Assert
		assertNotNull(this.context.getBean(StackResourceRegistry.class));

		httpServer.removeContext(httpContext);
		MetaDataServer.shutdownHttpServer();
	}

	@Test
	public void stackRegistry_manualConfigurationEnabled_returnsAutoConfiguredStackRegistry() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ManualConfigurationStackRegistryTestConfiguration.class);
		this.context.register(ContextStackAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "cloud.aws.stack.name:manualStackName");

		//Act
		this.context.refresh();

		//Assert
		assertNotNull(this.context.getBean(StackResourceRegistry.class));
	}

	@Test
	public void resourceIdResolver_withoutAnyStackConfiguration_availableAsConfiguredBean() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextStackAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "cloud.aws.stack.auto:false");
		//Act
		this.context.refresh();

		//Assert
		assertNotNull(this.context.getBean(ResourceIdResolver.class));
		assertTrue(this.context.getBeansOfType(StackResourceRegistry.class).isEmpty());
	}

	@Configuration
	static class AutoConfigurationStackRegistryTestConfiguration {

		@Bean
		public AmazonCloudFormation amazonCloudFormation() {
			AmazonCloudFormation amazonCloudFormation = mock(AmazonCloudFormation.class);
			when(amazonCloudFormation.describeStackResources(new DescribeStackResourcesRequest().withPhysicalResourceId("test"))).
					thenReturn(new DescribeStackResourcesResult().withStackResources(new StackResource().withStackName("testStack")));
			when(amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("testStack"))).
					thenReturn(new ListStackResourcesResult().withStackResourceSummaries(Collections.<StackResourceSummary>emptyList()));
			return amazonCloudFormation;
		}

		@Bean
		public AmazonEC2 amazonEC2() {
			AmazonEC2 mockAmazonEC2 = mock(AmazonEC2.class);
			DescribeTagsResult mockDescribeTagResult = mock(DescribeTagsResult.class);
			when(mockAmazonEC2.describeTags(any(DescribeTagsRequest.class))).thenReturn(mockDescribeTagResult);
			when(mockDescribeTagResult.getTags()).thenReturn(Collections.singletonList(
					new TagDescription().withKey("aws:cloudformation:stack-name").withValue("testStack")));
			return mockAmazonEC2;
		}
	}

	@Configuration
	static class ManualConfigurationStackRegistryTestConfiguration {

		@Bean
		public AmazonCloudFormation amazonCloudFormation() {
			AmazonCloudFormation amazonCloudFormation = mock(AmazonCloudFormation.class);
			when(amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("manualStackName"))).
					thenReturn(new ListStackResourcesResult().withStackResourceSummaries(Collections.<StackResourceSummary>emptyList()));
			return amazonCloudFormation;
		}
	}
}
