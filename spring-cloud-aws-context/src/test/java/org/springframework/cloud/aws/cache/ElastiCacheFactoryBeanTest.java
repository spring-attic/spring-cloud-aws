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

package org.springframework.cloud.aws.cache;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticache.model.CacheCluster;
import software.amazon.awssdk.services.elasticache.model.CacheNode;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheClustersRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheClustersResponse;
import software.amazon.awssdk.services.elasticache.model.Endpoint;

import org.springframework.cache.Cache;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElastiCacheFactoryBeanTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getObject_availableCluster_returnsConfiguredMemcachedClient()
			throws Exception {
		// Arrange
		ElastiCacheClient amazonElastiCache = mock(ElastiCacheClient.class);

		DescribeCacheClustersRequest testCache = DescribeCacheClustersRequest.builder()
				.cacheClusterId("testCache").showCacheNodeInfo(true).build();

		when(amazonElastiCache.describeCacheClusters(testCache))
				.thenReturn(DescribeCacheClustersResponse.builder()
						.cacheClusters(CacheCluster.builder()
								.configurationEndpoint(Endpoint.builder()
										.address("localhost").port(45678).build())
								.cacheClusterStatus("available").engine("memcached")
								.build())
						.build());
		ElastiCacheFactoryBean elasticCacheFactoryBean = new ElastiCacheFactoryBean(
				amazonElastiCache, "testCache", Collections.singletonList(
						new TestCacheFactory("testCache", "localhost", 45678)));

		// Act
		elasticCacheFactoryBean.afterPropertiesSet();
		Cache cache = elasticCacheFactoryBean.getObject();

		// Assert
		assertThat(cache).isNotNull();
	}

	@Test
	public void getObject_availableClusterWithLogicalName_returnsConfigurationMemcachedClientWithPhysicalName()
			throws Exception {
		// Arrange
		ElastiCacheClient amazonElastiCache = mock(ElastiCacheClient.class);
		DescribeCacheClustersRequest testCache = DescribeCacheClustersRequest.builder()
				.cacheClusterId("testCache").showCacheNodeInfo(true).build();

		when(amazonElastiCache.describeCacheClusters(testCache))
				.thenReturn(DescribeCacheClustersResponse.builder()
						.cacheClusters(CacheCluster.builder()
								.configurationEndpoint(Endpoint.builder()
										.address("localhost").port(45678).build())
								.cacheClusterStatus("available").engine("memcached")
								.build())
						.build());

		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId("test"))
				.thenReturn("testCache");

		ElastiCacheFactoryBean elastiCacheFactoryBean = new ElastiCacheFactoryBean(
				amazonElastiCache, "test", resourceIdResolver,
				Collections.<CacheFactory>singletonList(
						new TestCacheFactory("test", "localhost", 45678)));

		// Act
		elastiCacheFactoryBean.afterPropertiesSet();
		Cache cache = elastiCacheFactoryBean.getObject();

		// Assert
		assertThat(cache).isNotNull();
	}

	@Test
	public void getObject_clusterWithRedisEngineConfigured_reportsError()
			throws Exception {
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("engine");

		ElastiCacheClient amazonElastiCache = mock(ElastiCacheClient.class);
		DescribeCacheClustersRequest memcached = DescribeCacheClustersRequest.builder()
				.cacheClusterId("memcached").showCacheNodeInfo(true).build();

		when(amazonElastiCache.describeCacheClusters(memcached))
				.thenReturn(DescribeCacheClustersResponse.builder()
						.cacheClusters(CacheCluster.builder().engine("redis")
								.cacheNodes(CacheNode.builder()
										.endpoint(Endpoint.builder().address("localhost")
												.port(45678).build())
										.build())
								.build())
						.build());

		ElastiCacheFactoryBean elastiCacheFactoryBean = new ElastiCacheFactoryBean(
				amazonElastiCache, "memcached", Collections.singletonList(
						new TestCacheFactory("testCache", "localhost", 45678)));

		// Act
		elastiCacheFactoryBean.afterPropertiesSet();

		// Assert
	}

	private static final class TestCacheFactory implements CacheFactory {

		private final String expectedCacheName;

		private final String expectedHostName;

		private final int expectedPort;

		private TestCacheFactory(String expectedCacheName, String expectedHostName,
				int expectedPort) {
			this.expectedCacheName = expectedCacheName;
			this.expectedHostName = expectedHostName;
			this.expectedPort = expectedPort;
		}

		@Override
		public boolean isSupportingCacheArchitecture(String architecture) {
			return "memcached".equals(architecture);
		}

		@Override
		public Cache createCache(String cacheName, String host, int port) {
			assertThat(this.expectedCacheName).isEqualTo(cacheName);
			assertThat(this.expectedHostName).isEqualTo(host);
			assertThat(this.expectedPort).isEqualTo(port);
			return mock(Cache.class);
		}

	}

}
