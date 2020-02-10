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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.S3Exception;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Proxy to wrap an {@link S3Client} handler and handle redirects wrapped inside
 * {@link S3Exception}.
 *
 * @author Greg Turnquist
 * @author Agim Emruli
 * @since 1.1
 */
public final class AmazonS3ProxyFactory {

	private AmazonS3ProxyFactory() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	/**
	 * Factory-method to create a proxy using the {@link SimpleStorageRedirectInterceptor}
	 * that supports redirects for buckets which are in a different region. This proxy
	 * uses the amazonS3 parameter as a "prototype" and re-uses the credentials from the
	 * passed in {@link S3Client} instance. Proxy implementations uses the
	 * {@link AmazonS3ClientFactory} to create region specific clients, which are cached
	 * by the implementation on a region basis to avoid unnecessary object creation.
	 * @param amazonS3 Fully configured AmazonS3 client, the client can be an immutable
	 * instance (created by the {@link S3ClientBuilder#build()} ) as this proxy will not
	 * change the underlying implementation.
	 * @return AOP-Proxy that intercepts all method calls using the
	 * {@link SimpleStorageRedirectInterceptor}
	 */
	public static S3Client createProxy(S3Client amazonS3) {
		Assert.notNull(amazonS3, "AmazonS3 client must not be null");

		if (AopUtils.isAopProxy(amazonS3)) {

			Advised advised = (Advised) amazonS3;
			for (Advisor advisor : advised.getAdvisors()) {
				if (ClassUtils.isAssignableValue(SimpleStorageRedirectInterceptor.class,
						advisor.getAdvice())) {
					return amazonS3;
				}
			}

			try {
				advised.addAdvice(new SimpleStorageRedirectInterceptor(
						(S3Client) advised.getTargetSource().getTarget()));
			}
			catch (Exception e) {
				throw new RuntimeException(
						"Error adding advice for class amazonS3 instance", e);
			}

			return amazonS3;
		}

		ProxyFactory factory = new ProxyFactory(amazonS3);
		factory.setInterfaces(S3Client.class);
		factory.addAdvice(new SimpleStorageRedirectInterceptor(amazonS3));

		return (S3Client) factory.getProxy();
	}

	/**
	 * {@link MethodInterceptor} implementation that is handles redirect which are
	 * {@link S3Exception} with a return code of 301. This class creates a region specific
	 * client for the redirected endpoint.
	 *
	 * @author Greg Turnquist
	 * @author Agim Emruli
	 * @since 1.1
	 */
	static final class SimpleStorageRedirectInterceptor implements MethodInterceptor {

		private static final Logger LOGGER = LoggerFactory
				.getLogger(SimpleStorageRedirectInterceptor.class);

		private final S3Client amazonS3;

		private final AmazonS3ClientFactory amazonS3ClientFactory;

		private SimpleStorageRedirectInterceptor(S3Client amazonS3) {
			this.amazonS3 = amazonS3;
			this.amazonS3ClientFactory = new AmazonS3ClientFactory();
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			try {
				return invocation.proceed();
			}
			catch (S3Exception e) {
				if (301 == e.statusCode()) {
					S3Client redirectClient = buildAmazonS3ForRedirectLocation(
							this.amazonS3, e);
					return ReflectionUtils.invokeMethod(invocation.getMethod(),
							redirectClient, invocation.getArguments());
				}
				else {
					throw e;
				}
			}
		}

		private S3Client buildAmazonS3ForRedirectLocation(S3Client prototype,
				S3Exception e) {
			try {
				final String region = e.awsErrorDetails().sdkHttpResponse()
						.firstMatchingHeader("x-amx-bucket-region")
						.orElseThrow(() -> new RuntimeException(
								"Error getting new Amazon S3 for redirect. Region header missing."));
				return this.amazonS3ClientFactory.createClientForRegion(prototype,
						region);
			}
			catch (Exception ex) {
				LOGGER.error("Error getting new Amazon S3 for redirect", ex);
				throw new RuntimeException(e);
			}
		}

	}

}
