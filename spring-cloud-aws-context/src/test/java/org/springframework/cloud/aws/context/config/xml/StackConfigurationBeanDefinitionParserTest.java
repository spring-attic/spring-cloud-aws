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

package org.springframework.cloud.aws.context.config.xml;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.TagDescription;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.context.MetaDataServer;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.getBeanName;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 */
public class StackConfigurationBeanDefinitionParserTest {

	@Test
	public void parseInternal_stackConfigurationWithExternallyConfiguredCloudFormationClient_returnsConfiguredStackWithExternallyConfiguredClient() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-withCustomCloudFormationClient.xml", getClass()));

		AmazonCloudFormation amazonCloudFormationMock = beanFactory.getBean(AmazonCloudFormation.class);
		when(amazonCloudFormationMock.listStackResources(new ListStackResourcesRequest().withStackName("test"))).
				thenReturn(new ListStackResourcesResult().withStackResourceSummaries(new StackResourceSummary()));
		when(amazonCloudFormationMock.describeStacks(new DescribeStacksRequest().withStackName("test"))).
				thenReturn(new DescribeStacksResult().withStacks(new Stack()));


		//Act
		StackResourceRegistry stackResourceRegistry = beanFactory.getBean(StackResourceRegistry.class);

		//Assert
		assertNotNull(stackResourceRegistry);
		assertFalse(beanFactory.containsBeanDefinition(getBeanName(AmazonCloudFormationClient.class.getName())));
		verify(amazonCloudFormationMock, times(1)).listStackResources(new ListStackResourcesRequest().withStackName("test"));
		beanFactory.getBean("customStackTags");
		verify(amazonCloudFormationMock, times(1)).describeStacks(new DescribeStacksRequest().withStackName("test"));
	}

	@Test
	public void parseInternal_stackConfigurationWithExternallyConfiguredAmazonEC2_returnsConfiguredStackWithExternallyConfiguredClient() throws Exception {
		//Arrange
		HttpServer server = MetaDataServer.setupHttpServer();
		HttpContext httpContext = server.createContext("/latest/meta-data/instance-id", new MetaDataServer.HttpResponseWriterHandler("foo"));
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-withCustomAmazonEC2.xml", getClass()));

		AmazonEC2 amazonEC2 = beanFactory.getBean("customAmazonEC2", AmazonEC2.class);
		DescribeTagsResult mockDescribeTagResult = Mockito.mock(DescribeTagsResult.class);
		when(amazonEC2.describeTags(Mockito.any(DescribeTagsRequest.class))).thenReturn(mockDescribeTagResult);
		when(mockDescribeTagResult.getTags()).thenReturn(Collections.singletonList(
				new TagDescription().withKey("aws:cloudformation:stack-name").withValue("test")));

		AmazonCloudFormation amazonCloudFormation = Mockito.mock(AmazonCloudFormation.class);
		when(amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("test"))).
				thenReturn(new ListStackResourcesResult().withStackResourceSummaries(new StackResourceSummary()));

		beanFactory.registerSingleton(getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);

		//Act
		StackResourceRegistry stackResourceRegistry = beanFactory.getBean(StackResourceRegistry.class);

		//Assert
		assertNotNull(stackResourceRegistry);
		assertFalse(beanFactory.containsBeanDefinition(getBeanName(AmazonEC2.class.getName())));
		verify(amazonEC2, times(1)).describeTags(new DescribeTagsRequest().withFilters(
				new Filter("resource-id", Collections.singletonList("foo")),
				new Filter("resource-type", Collections.singletonList("instance"))));
		verify(amazonCloudFormation, times(1)).listStackResources(new ListStackResourcesRequest().withStackName("test"));
		server.removeContext(httpContext);
	}

	@Test
	public void parseInternal_withCustomRegion_shouldConfigureDefaultClientWithCustomRegion() throws Exception {
		//Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-region.xml", getClass()));

		// Assert
		AmazonCloudFormationClient amazonCloudFormation = registry.getBean(AmazonCloudFormationClient.class);
		assertEquals("https://" + Region.getRegion(Regions.SA_EAST_1).getServiceEndpoint("cloudformation"), ReflectionTestUtils.getField(amazonCloudFormation, "endpoint").toString());
	}

	@Test
	public void parseInternal_withCustomRegionProvider_shouldConfigureDefaultClientWithCustomRegionReturnedByProvider() throws Exception {
		//Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-region-provider.xml", getClass()));

		// Assert
		AmazonCloudFormationClient amazonCloudFormation = registry.getBean(AmazonCloudFormationClient.class);
		assertEquals("https://" + Region.getRegion(Regions.AP_SOUTHEAST_2).getServiceEndpoint("cloudformation"), ReflectionTestUtils.getField(amazonCloudFormation, "endpoint").toString());
	}

	@Test
	public void resourceIdResolver_stackConfiguration_resourceIdResolverBeanExposed() {
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito.mock(AmazonCloudFormation.class);
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);

		when(amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("IntegrationTestStack"))).
				thenReturn(new ListStackResourcesResult().withStackResourceSummaries(new StackResourceSummary()));

		applicationContext.load(new ClassPathResource(getClass().getSimpleName() + "-staticStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonEC2.class.getName()), amazonEC2);

		applicationContext.refresh();

		// Act
		ResourceIdResolver resourceIdResolver = applicationContext.getBean(ResourceIdResolver.class);

		// Assert
		assertThat(resourceIdResolver, is(not(nullValue())));
	}

	@Test
	public void stackResourceRegistry_stackConfigurationWithStaticName_stackResourceRegistryBeanExposedUnderStaticStackName() throws Exception {
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito.mock(AmazonCloudFormation.class);
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);

		when(amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("IntegrationTestStack"))).
				thenReturn(new ListStackResourcesResult().withStackResourceSummaries(new StackResourceSummary()));

		applicationContext.load(new ClassPathResource(getClass().getSimpleName() + "-staticStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonEC2.class.getName()), amazonEC2);

		applicationContext.refresh();

		// Act
		StackResourceRegistry staticStackNameProviderBasedStackResourceRegistry = applicationContext.getBean("IntegrationTestStack", StackResourceRegistry.class);

		// Assert
		assertThat(staticStackNameProviderBasedStackResourceRegistry, is(not(nullValue())));
	}

	@Test
	public void stackResourceRegistry_stackConfigurationWithoutStaticName_stackResourceRegistryBeanExposedUnderGeneratedName() throws Exception {
		// Arrange
		HttpServer server = MetaDataServer.setupHttpServer();
		HttpContext httpContext = server.createContext("/latest/meta-data/instance-id", new MetaDataServer.HttpResponseWriterHandler("foo"));

		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito.mock(AmazonCloudFormation.class);

		when(amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("test"))).
				thenReturn(new ListStackResourcesResult().withStackResourceSummaries(new StackResourceSummary()));

		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		DescribeTagsResult mockDescribeTagResult = Mockito.mock(DescribeTagsResult.class);
		when(amazonEC2.describeTags(Mockito.any(DescribeTagsRequest.class))).thenReturn(mockDescribeTagResult);
		when(mockDescribeTagResult.getTags()).thenReturn(Collections.singletonList(
				new TagDescription().withKey("aws:cloudformation:stack-name").withValue("test")));


		applicationContext.load(new ClassPathResource(getClass().getSimpleName() + "-autoDetectStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonEC2.class.getName()), amazonEC2);

		applicationContext.refresh();

		// Act
		StackResourceRegistry autoDetectingStackNameProviderBasedStackResourceRegistry = applicationContext.getBean("org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean#0", StackResourceRegistry.class);

		// Assert
		assertThat(autoDetectingStackNameProviderBasedStackResourceRegistry, is(not(nullValue())));

		server.removeContext(httpContext);
	}

	@Test
	public void resourceIdResolverResolveToPhysicalResourceId_stackConfigurationWithStaticNameAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId() {
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito.mock(AmazonCloudFormation.class);
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);

		when(amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("IntegrationTestStack"))).
				thenReturn(new ListStackResourcesResult().withStackResourceSummaries(
						new StackResourceSummary().withLogicalResourceId("EmptyBucket").withPhysicalResourceId("integrationteststack-emptybucket-foo")));

		applicationContext.load(new ClassPathResource(getClass().getSimpleName() + "-staticStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonEC2.class.getName()), amazonEC2);

		applicationContext.refresh();

		ResourceIdResolver resourceIdResolver = applicationContext.getBean(ResourceIdResolver.class);

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("EmptyBucket");

		// Assert
		assertThat(physicalResourceId, startsWith("integrationteststack-emptybucket-"));
	}

	@Test
	public void resourceIdResolverResolveToPhysicalResourceId_stackConfigurationWithoutStaticNameAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId() throws Exception {
		// Arrange
		HttpServer server = MetaDataServer.setupHttpServer();
		HttpContext httpContext = server.createContext("/latest/meta-data/instance-id", new MetaDataServer.HttpResponseWriterHandler("foo"));

		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		DescribeTagsResult mockDescribeTagResult = Mockito.mock(DescribeTagsResult.class);
		when(amazonEC2.describeTags(Mockito.any(DescribeTagsRequest.class))).thenReturn(mockDescribeTagResult);
		when(mockDescribeTagResult.getTags()).thenReturn(Collections.singletonList(
				new TagDescription().withKey("aws:cloudformation:stack-name").withValue("test")));

		AmazonCloudFormation amazonCloudFormation = Mockito.mock(AmazonCloudFormation.class);
		when(amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("test"))).
				thenReturn(new ListStackResourcesResult().withStackResourceSummaries(
						new StackResourceSummary().withLogicalResourceId("EmptyBucket").withPhysicalResourceId("integrationteststack-emptybucket-foo")));


		applicationContext.load(new ClassPathResource(getClass().getSimpleName() + "-autoDetectStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonEC2.class.getName()), amazonEC2);
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);

		applicationContext.refresh();

		ResourceIdResolver resourceIdResolver = applicationContext.getBean(ResourceIdResolver.class);

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("EmptyBucket");

		// Assert
		assertThat(physicalResourceId, startsWith("integrationteststack-emptybucket-"));

		server.removeContext(httpContext);
	}

	@Test
	public void resourceIdResolverResolveToPhysicalResourceId_logicalResourceIdOfNonExistingResourceProvided_returnsLogicalResourceIdAsPhysicalResourceId() {
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito.mock(AmazonCloudFormation.class);
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);

		when(amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("IntegrationTestStack"))).
				thenReturn(new ListStackResourcesResult().withStackResourceSummaries(new StackResourceSummary()));

		applicationContext.load(new ClassPathResource(getClass().getSimpleName() + "-staticStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);
		applicationContext.getBeanFactory().registerSingleton(getBeanName(AmazonEC2.class.getName()), amazonEC2);

		applicationContext.refresh();
		ResourceIdResolver resourceIdResolver = applicationContext.getBean(ResourceIdResolver.class);

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("nonExistingLogicalResourceId");

		// Assert
		assertThat(physicalResourceId, is("nonExistingLogicalResourceId"));
	}

	@After
	public void destroyMetaDataServer() throws Exception {
		MetaDataServer.shutdownHttpServer();

	}
}
