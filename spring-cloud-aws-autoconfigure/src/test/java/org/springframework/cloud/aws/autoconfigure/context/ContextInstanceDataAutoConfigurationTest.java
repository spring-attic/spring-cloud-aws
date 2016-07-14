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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.aws.autoconfigure.context.MetaData.Context;
import org.springframework.cloud.aws.context.support.env.AwsCloudEnvironmentCheckUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Agim Emruli
 */
public class ContextInstanceDataAutoConfigurationTest {

	@Rule
	public MetaDataServer metaDataServer = new MetaDataServer();

	private AnnotationConfigApplicationContext context;

	@Before
	public void restContextInstanceDataCondition() throws IllegalAccessException {
		Field field = ReflectionUtils.findField(AwsCloudEnvironmentCheckUtils.class, "isCloudEnvironment");
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
	@MetaData(@Context(path = "/latest/meta-data/instance-id", value = "testInstanceId"))
	public void placeHolder_noExplicitConfiguration_createInstanceDataResolverForAwsEnvironment() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextInstanceDataAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		assertTrue(this.context.containsBean("AmazonEc2InstanceDataPropertySourcePostProcessor"));
	}

	@Test
	@MetaData(@Context(path = "/latest/meta-data/instance-id", nullValue = true))
	public void placeHolder_noExplicitConfiguration_missingInstanceDataResolverForNotAwsEnvironment() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextInstanceDataAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		assertFalse(this.context.containsBean("AmazonEc2InstanceDataPropertySourcePostProcessor"));
	}

	@Test
	@MetaData({
			@Context(path = "/latest/meta-data/instance-id", value = "testInstanceId"),
			@Context(path = "/latest/user-data", value = "a:b;c:d") })
	public void placeHolder_noExplicitConfiguration_createInstanceDataResolverThatResolvesWithDefaultAttributes() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextInstanceDataAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		assertEquals("b", this.context.getEnvironment().getProperty("a"));
		assertEquals("d", this.context.getEnvironment().getProperty("c"));
	}

	@Test
	@MetaData({
		@Context(path = "/latest/meta-data/instance-id", value = "testInstanceId"),
		@Context(path = "/latest/user-data", value = "a=b;c=d") })
	public void placeHolder_customValueSeparator_createInstanceDataResolverThatResolvesWithCustomValueSeparator() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();

		EnvironmentTestUtils.addEnvironment(this.context, "cloud.aws.instance.data.valueSeparator:=");

		this.context.register(ContextInstanceDataAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		assertEquals("b", this.context.getEnvironment().getProperty("a"));
		assertEquals("d", this.context.getEnvironment().getProperty("c"));
	}

	@Test
	@MetaData({
		@Context(path = "/latest/meta-data/instance-id", value = "testInstanceId"),
		@Context(path = "/latest/user-data", value = "a:b/c:d") })
	public void placeHolder_customAttributeSeparator_createInstanceDataResolverThatResolvesWithCustomAttribute() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();

		EnvironmentTestUtils.addEnvironment(this.context, "cloud.aws.instance.data.attributeSeparator:/");

		this.context.register(ContextInstanceDataAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		assertEquals("b", this.context.getEnvironment().getProperty("a"));
		assertEquals("d", this.context.getEnvironment().getProperty("c"));
	}
}
