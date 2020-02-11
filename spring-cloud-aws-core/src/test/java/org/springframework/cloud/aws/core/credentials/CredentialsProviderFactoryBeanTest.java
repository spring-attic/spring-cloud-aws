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

package org.springframework.cloud.aws.core.credentials;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for
 * {@link org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean}.
 *
 * @author Agim Emruli
 */
public class CredentialsProviderFactoryBeanTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCreateWithNullCredentialsProvider() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("not be null");
		// noinspection ResultOfObjectAllocationIgnored
		new CredentialsProviderFactoryBean(null);
	}

	@Test
	public void getObject_withZeroConfiguredProviders_returnsDefaultAwsCredentialsProviderChain()
			throws Exception {
		// Arrange
		CredentialsProviderFactoryBean credentialsProviderFactoryBean = new CredentialsProviderFactoryBean();
		credentialsProviderFactoryBean.afterPropertiesSet();

		// Act
		AwsCredentialsProvider credentialsProvider = credentialsProviderFactoryBean
				.getObject();

		// Assert
		assertThat(credentialsProvider).isNotNull();
		assertThat(DefaultCredentialsProvider.class.isInstance(credentialsProvider))
				.isTrue();
	}

	@Test
	public void testCreateWithMultiple() throws Exception {
		AwsCredentialsProvider first = mock(AwsCredentialsProvider.class);
		AwsCredentialsProvider second = mock(AwsCredentialsProvider.class);

		CredentialsProviderFactoryBean credentialsProviderFactoryBean = new CredentialsProviderFactoryBean(
				Arrays.asList(first, second));
		credentialsProviderFactoryBean.afterPropertiesSet();

		AwsCredentialsProvider provider = credentialsProviderFactoryBean.getObject();

		AwsBasicCredentials foo = AwsBasicCredentials.create("foo", "foo");
		AwsBasicCredentials bar = AwsBasicCredentials.create("bar", "bar");

		when(first.resolveCredentials())
				.thenThrow(new RuntimeException("first call fails")).thenReturn(foo);
		when(second.resolveCredentials()).thenReturn(bar);

		assertThat(provider.resolveCredentials()).isEqualTo(bar);
		assertThat(provider.resolveCredentials()).isEqualTo(foo);
	}

}
