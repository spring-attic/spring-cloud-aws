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

package org.springframework.cloud.aws.core.io.s3;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link S3Client} client factory that create clients for other regions based on the
 * source client and a endpoint url. Caches clients per region to enable re-use on a
 * region base.
 *
 * @author Agim Emruli
 * @since 1.2
 */
public class AmazonS3ClientFactory {

	private static final String CLIENT_CONFIGURATION_FIELD_NAME = "clientConfiguration";

	private final ConcurrentHashMap<String, S3Client> clientCache = new ConcurrentHashMap<>(
			Region.regions().size());

	private final Field clientConfigurationField;

	public AmazonS3ClientFactory() {
		Class<?> s3ClientClass = ClassUtils.resolveClassName(
				"software.amazon.awssdk.services.s3.DefaultS3Client", null);
		this.clientConfigurationField = ReflectionUtils.findField(s3ClientClass,
				CLIENT_CONFIGURATION_FIELD_NAME);
		Assert.notNull(this.clientConfigurationField,
				"Client Configuration field not found, this class does not work with the current "
						+ "AWS SDK release");
		ReflectionUtils.makeAccessible(this.clientConfigurationField);
	}

	private static S3Client getAmazonS3ClientFromProxy(S3Client amazonS3) {
		if (AopUtils.isAopProxy(amazonS3)) {
			Advised advised = (Advised) amazonS3;
			Object target = null;
			try {
				target = advised.getTargetSource().getTarget();
			}
			catch (Exception e) {
				return null;
			}
			return target instanceof S3Client ? (S3Client) target : null;
		}
		else {
			return amazonS3 instanceof S3Client ? (S3Client) amazonS3 : null;
		}
	}

	public S3Client createClientForRegion(S3Client prototype, String region) {
		Assert.notNull(prototype, "AmazonS3 must not be null");
		Assert.notNull(region, "Region must not be null");

		if (!this.clientCache.containsKey(region)) {
			S3ClientBuilder amazonS3ClientBuilder = buildAmazonS3ForRegion(prototype,
					Region.of(region));
			this.clientCache.putIfAbsent(region, amazonS3ClientBuilder.build());
		}

		return this.clientCache.get(region);
	}

	private S3ClientBuilder buildAmazonS3ForRegion(S3Client prototype, Region region) {
		S3ClientBuilder clientBuilder = S3Client.builder();

		S3Client target = getAmazonS3ClientFromProxy(prototype);
		if (target != null) {
			SdkClientConfiguration sdkClientConfiguration = (SdkClientConfiguration) ReflectionUtils
					.getField(this.clientConfigurationField, target);
			AwsCredentialsProvider awsCredentialsProvider = sdkClientConfiguration
					.option(AwsClientOption.CREDENTIALS_PROVIDER);
			clientBuilder.credentialsProvider(awsCredentialsProvider);
		}

		return clientBuilder.region(region);
	}

}
