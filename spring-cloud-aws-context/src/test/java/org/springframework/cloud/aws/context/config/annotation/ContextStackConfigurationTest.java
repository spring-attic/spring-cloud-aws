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

package org.springframework.cloud.aws.context.config.annotation;

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
import org.junit.Test;
import org.springframework.cloud.aws.context.MetaDataServer;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContextStackConfigurationTest {


	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}

		MetaDataServer.shutdownHttpServer();
	}

	@Test
	public void stackRegistry_noStackNameConfigured_returnsAutoConfiguredStackRegistry() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithEmptyStackName.class);
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext httpContext = httpServer.createContext("/latest/meta-data/instance-id", new MetaDataServer.HttpResponseWriterHandler("test"));

		//Act
		this.context.refresh();

		//Assert
		StackResourceRegistry stackResourceRegistry = this.context.getBean(StackResourceRegistry.class);
		assertNotNull(stackResourceRegistry);
		assertEquals("testStack", stackResourceRegistry.getStackName());

		httpServer.removeContext(httpContext);
	}

	@Test
	public void stackRegistry_stackNameConfigured_returnsConfiguredStackRegistryForName() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ManualConfigurationStackRegistryTestConfiguration.class);

		//Act
		this.context.refresh();

		//Assert
		StackResourceRegistry stackResourceRegistry = this.context.getBean(StackResourceRegistry.class);
		assertNotNull(stackResourceRegistry);
		assertEquals("manualStackName",stackResourceRegistry.getStackName());
	}

	@Configuration
	@EnableStackConfiguration
	static class ApplicationConfigurationWithEmptyStackName {

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
	@EnableStackConfiguration(stackName = "manualStackName")
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
