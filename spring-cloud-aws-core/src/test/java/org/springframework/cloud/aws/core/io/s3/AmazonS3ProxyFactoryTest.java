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

import java.util.ArrayList;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Greg Turnquist
 */
public class AmazonS3ProxyFactoryTest {

	@Test
	public void verifyBasicAdvice() throws Exception {

		S3Client amazonS3 = mock(S3Client.class);
		assertThat(AopUtils.isAopProxy(amazonS3)).isFalse();

		S3Client proxy = AmazonS3ProxyFactory.createProxy(amazonS3);
		assertThat(AopUtils.isAopProxy(proxy)).isTrue();

		Advised advised = (Advised) proxy;
		assertThat(advised.getAdvisors().length).isEqualTo(1);
		assertThat(advised.getAdvisors()[0].getAdvice()).isInstanceOf(
				AmazonS3ProxyFactory.SimpleStorageRedirectInterceptor.class);
		assertThat(AopUtils.isAopProxy(advised.getTargetSource().getTarget())).isFalse();
	}

	@Test
	public void verifyDoubleWrappingHandled() throws Exception {

		S3Client amazonS3 = mock(S3Client.class);

		S3Client proxy = AmazonS3ProxyFactory
				.createProxy(AmazonS3ProxyFactory.createProxy(amazonS3));
		assertThat(AopUtils.isAopProxy(proxy)).isTrue();

		Advised advised = (Advised) proxy;
		assertThat(advised.getAdvisors().length).isEqualTo(1);
		assertThat(advised.getAdvisors()[0].getAdvice()).isInstanceOf(
				AmazonS3ProxyFactory.SimpleStorageRedirectInterceptor.class);
		assertThat(AopUtils.isAopProxy(advised.getTargetSource().getTarget())).isFalse();
	}

	@Test
	public void verifyPolymorphicHandling() {

		S3Client amazonS3 = mock(S3Client.class);
		S3Client proxy1 = AmazonS3ProxyFactory.createProxy(amazonS3);

		assertThat(S3Client.class.isAssignableFrom(proxy1.getClass())).isTrue();
		assertThat(S3Client.class.isAssignableFrom(proxy1.getClass())).isFalse();

		S3Client amazonS3Client = S3Client.builder().region(Region.US_WEST_2).build();
		S3Client proxy2 = AmazonS3ProxyFactory.createProxy(amazonS3Client);

		assertThat(S3Client.class.isAssignableFrom(proxy2.getClass())).isTrue();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void verifyAddingRedirectAdviceToExistingProxy() {

		S3Client amazonS3 = mock(S3Client.class);

		ProxyFactory factory = new ProxyFactory(amazonS3);
		factory.addAdvice(new TestAdvice());
		S3Client proxy1 = (S3Client) factory.getProxy();

		assertThat(((Advised) proxy1).getAdvisors().length).isEqualTo(1);

		S3Client proxy2 = AmazonS3ProxyFactory.createProxy(proxy1);
		Advised advised = (Advised) proxy2;

		assertThat(advised.getAdvisors().length).isEqualTo(2);

		List<Class<? extends MethodInterceptor>> advisorClasses = new ArrayList<>();
		for (Advisor advisor : advised.getAdvisors()) {
			advisorClasses.add(((MethodInterceptor) advisor.getAdvice()).getClass());
		}
		assertThat(advisorClasses).contains(TestAdvice.class,
				AmazonS3ProxyFactory.SimpleStorageRedirectInterceptor.class);

	}

	static class TestAdvice implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation methodInvocation) throws Throwable {
			return methodInvocation.proceed();
		}

	}

}
