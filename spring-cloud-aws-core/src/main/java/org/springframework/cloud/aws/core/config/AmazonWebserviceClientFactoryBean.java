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

package org.springframework.cloud.aws.core.config;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.regions.Region;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} class to create {@link SdkClient}
 * instances. This class is responsible to create the respective AmazonWebServiceClient
 * classes because the configuration through Springs's BeanFactory fails due to invalid
 * properties inside the Webservice client classes (see
 * https://github.com/aws/aws-sdk-java/issues/325)
 *
 * @param <T> implementation of the {@link SdkClient}
 * @author Agim Emruli
 */
public class AmazonWebserviceClientFactoryBean<T extends SdkClient>
		extends AbstractFactoryBean<T> {

	private final Class<? extends SdkClient> clientClass;

	private final AwsCredentialsProvider credentialsProvider;

	private RegionProvider regionProvider;

	private Region customRegion;

	private ExecutorService executor;

	public AmazonWebserviceClientFactoryBean(Class<T> clientClass,
			AwsCredentialsProvider credentialsProvider) {
		this.clientClass = clientClass;
		this.credentialsProvider = credentialsProvider;
	}

	public AmazonWebserviceClientFactoryBean(Class<T> clientClass,
			AwsCredentialsProvider credentialsProvider, RegionProvider regionProvider) {
		this(clientClass, credentialsProvider);
		setRegionProvider(regionProvider);
	}

	@Override
	public Class<?> getObjectType() {
		return this.clientClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T createInstance() throws Exception {

		Method builderMethod = ClassUtils.getStaticMethod(clientClass, "builder");
		Assert.notNull(builderMethod, "Could not find builder() method in class:'"
				+ clientClass.getName() + "'");

		AwsClientBuilder<?, T> builder = (AwsClientBuilder<?, T>) ReflectionUtils
				.invokeMethod(builderMethod, null);

		if (this.executor != null) {
			Assert.isAssignable(AwsAsyncClientBuilder.class, builder.getClass(), "Client must be async if executor is set.");
			((AwsAsyncClientBuilder<?, T>) builder).asyncConfiguration(ClientAsyncConfiguration.builder()
					.advancedOption(
							SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
							executor)
					.build());
		}

		if (this.credentialsProvider != null) {
			builder.credentialsProvider(this.credentialsProvider);
		}

		if (this.customRegion != null) {
			builder.region(this.customRegion);
		}
		else if (this.regionProvider != null) {
			builder.region(this.regionProvider.getRegion());
		}
		else {
			builder.region(Region.US_WEST_2);
		}
		return builder.build();
	}

	public void setRegionProvider(RegionProvider regionProvider) {
		this.regionProvider = regionProvider;
	}

	public void setCustomRegion(String customRegionName) {
		this.customRegion = Region.of(customRegionName);
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	@Override
	protected void destroyInstance(T instance) throws Exception {
		instance.close();
	}

}
