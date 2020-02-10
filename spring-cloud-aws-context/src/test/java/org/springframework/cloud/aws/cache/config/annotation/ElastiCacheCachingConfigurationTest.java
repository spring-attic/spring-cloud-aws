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

package org.springframework.cloud.aws.cache.config.annotation;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticache.model.CacheCluster;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheClustersRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheClustersResponse;
import software.amazon.awssdk.services.elasticache.model.Endpoint;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cloud.aws.cache.config.TestMemcacheServer;
import org.springframework.cloud.aws.cache.memcached.SimpleSpringMemcached;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ElastiCacheCachingConfigurationTest {

	private AnnotationConfigApplicationContext context;

	private static int getExpirationFromCache(Cache cache) throws IllegalAccessException {
		assertThat(cache instanceof SimpleSpringMemcached).isTrue();
		Field expiration = ReflectionUtils.findField(SimpleSpringMemcached.class,
				"expiration");
		assertThat(expiration).isNotNull();
		ReflectionUtils.makeAccessible(expiration);
		return expiration.getInt(cache);
	}

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void enableElasticache_configuredWithExplicitCluster_configuresExplicitlyConfiguredCaches()
			throws Exception {
		// Arrange

		// Act
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithExplicitStackConfiguration.class);

		// Assert
		CacheManager cacheManager = this.context.getBean(CachingConfigurer.class)
				.cacheManager();
		assertThat(cacheManager.getCacheNames().size()).isEqualTo(2);
		Cache firstCache = cacheManager.getCache("firstCache");
		assertThat(firstCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(firstCache)).isEqualTo(0);

		Cache secondCache = cacheManager.getCache("secondCache");
		assertThat(secondCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(secondCache)).isEqualTo(0);
	}

	// @checkstyle:off
	@Test
	public void enableElasticache_configuredWithExplicitClusterAndExpiration_configuresExplicitlyConfiguredCachesWithCustomExpirationTimes()
			// @checkstyle:on
			throws Exception {
		// Arrange

		// Act
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithExplicitStackConfigurationAndExpiryTime.class);

		// Assert
		CacheManager cacheManager = this.context.getBean(CachingConfigurer.class)
				.cacheManager();
		assertThat(cacheManager.getCacheNames().size()).isEqualTo(2);
		Cache firstCache = cacheManager.getCache("firstCache");
		assertThat(firstCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(firstCache)).isEqualTo(23);

		Cache secondCache = cacheManager.getCache("secondCache");
		assertThat(secondCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(secondCache)).isEqualTo(42);
	}

	// @checkstyle:off
	@Test
	public void enableElasticache_configuredWithExplicitClusterAndExpiration_configuresExplicitlyConfiguredCachesWithMixedExpirationTimes()
			throws Exception {
		// @checkstyle:on
		// Arrange

		// Act
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithExplicitStackConfigurationAndMixedExpiryTime.class);

		// Assert
		CacheManager cacheManager = this.context.getBean(CachingConfigurer.class)
				.cacheManager();
		assertThat(cacheManager.getCacheNames().size()).isEqualTo(2);
		Cache firstCache = cacheManager.getCache("firstCache");
		assertThat(firstCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(firstCache)).isEqualTo(12);

		Cache secondCache = cacheManager.getCache("secondCache");
		assertThat(secondCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(secondCache)).isEqualTo(42);
	}

	@Test
	public void enableElasticache_configuredWithoutExplicitCluster_configuresImplicitlyConfiguredCaches()
			throws Exception {
		// Arrange

		// Act
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithNoExplicitStackConfiguration.class);

		// Assert
		CacheManager cacheManager = this.context.getBean(CachingConfigurer.class)
				.cacheManager();
		assertThat(cacheManager.getCacheNames().size()).isEqualTo(2);
		Cache firstCache = cacheManager.getCache("sampleCacheOneLogical");
		assertThat(firstCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(firstCache)).isEqualTo(0);

		Cache secondCache = cacheManager.getCache("sampleCacheTwoLogical");
		assertThat(secondCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(secondCache)).isEqualTo(0);
	}

	// @checkstyle:off
	@Test
	public void enableElasticache_configuredWithoutExplicitClusterButDefaultExpiryTime_configuresImplicitlyConfiguredCachesWithDefaultExpiryTimeOnAllCaches()
			throws Exception {
		// @checkstyle:on
		// Arrange

		// Act
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithNoExplicitStackConfigurationAndDefaultExpiration.class);

		// Assert
		CacheManager cacheManager = this.context.getBean(CachingConfigurer.class)
				.cacheManager();
		assertThat(cacheManager.getCacheNames().size()).isEqualTo(2);
		Cache firstCache = cacheManager.getCache("sampleCacheOneLogical");
		assertThat(firstCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(firstCache)).isEqualTo(23);

		Cache secondCache = cacheManager.getCache("sampleCacheTwoLogical");
		assertThat(secondCache.getName()).isNotNull();
		assertThat(getExpirationFromCache(secondCache)).isEqualTo(23);
	}

	@EnableElastiCache({ @CacheClusterConfig(name = "firstCache"),
			@CacheClusterConfig(name = "secondCache") })
	public static class ApplicationConfigurationWithExplicitStackConfiguration {

		@Bean
		public ElastiCacheClient amazonElastiCache() {
			ElastiCacheClient amazonElastiCache = Mockito.mock(ElastiCacheClient.class);
			int port = TestMemcacheServer.startServer();
			DescribeCacheClustersRequest describeCacheClustersRequest = DescribeCacheClustersRequest
					.builder().cacheClusterId("firstCache").showCacheNodeInfo(true)
					.build();
			Mockito.when(
					amazonElastiCache.describeCacheClusters(describeCacheClustersRequest))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());
			DescribeCacheClustersRequest secondCache = DescribeCacheClustersRequest
					.builder().cacheClusterId("secondCache").showCacheNodeInfo(true)
					.build();
			Mockito.when(amazonElastiCache.describeCacheClusters(secondCache))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());
			return amazonElastiCache;
		}

	}

	@EnableElastiCache({ @CacheClusterConfig(name = "firstCache", expiration = 23),
			@CacheClusterConfig(name = "secondCache", expiration = 42) })
	public static class ApplicationConfigurationWithExplicitStackConfigurationAndExpiryTime {

		@Bean
		public ElastiCacheClient amazonElastiCache() {
			ElastiCacheClient amazonElastiCache = Mockito.mock(ElastiCacheClient.class);
			int port = TestMemcacheServer.startServer();
			DescribeCacheClustersRequest firstCache = DescribeCacheClustersRequest
					.builder().cacheClusterId("firstCache").showCacheNodeInfo(true)
					.build();

			Mockito.when(amazonElastiCache.describeCacheClusters(firstCache))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());
			DescribeCacheClustersRequest secondCache = DescribeCacheClustersRequest
					.builder().cacheClusterId("secondCache").showCacheNodeInfo(true)
					.build();

			Mockito.when(amazonElastiCache.describeCacheClusters(secondCache))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());
			return amazonElastiCache;
		}

	}

	@EnableElastiCache(
			value = { @CacheClusterConfig(name = "firstCache"),
					@CacheClusterConfig(name = "secondCache", expiration = 42) },
			defaultExpiration = 12)
	public static class ApplicationConfigurationWithExplicitStackConfigurationAndMixedExpiryTime {

		@Bean
		public ElastiCacheClient amazonElastiCache() {
			ElastiCacheClient amazonElastiCache = Mockito.mock(ElastiCacheClient.class);
			int port = TestMemcacheServer.startServer();
			DescribeCacheClustersRequest firstCache = DescribeCacheClustersRequest
					.builder().cacheClusterId("firstCache").showCacheNodeInfo(true)
					.build();

			Mockito.when(amazonElastiCache.describeCacheClusters(firstCache))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());
			DescribeCacheClustersRequest secondCache = DescribeCacheClustersRequest
					.builder().cacheClusterId("secondCache").showCacheNodeInfo(true)
					.build();

			Mockito.when(amazonElastiCache.describeCacheClusters(secondCache))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());
			return amazonElastiCache;
		}

	}

	@EnableElastiCache
	public static class ApplicationConfigurationWithNoExplicitStackConfiguration {

		@Bean
		public ElastiCacheClient amazonElastiCache() {
			ElastiCacheClient amazonElastiCache = Mockito.mock(ElastiCacheClient.class);
			int port = TestMemcacheServer.startServer();
			DescribeCacheClustersRequest sampleCacheOneLogical = DescribeCacheClustersRequest
					.builder().cacheClusterId("sampleCacheOneLogical")
					.showCacheNodeInfo(Boolean.TRUE).build();

			Mockito.when(amazonElastiCache.describeCacheClusters(sampleCacheOneLogical))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());

			DescribeCacheClustersRequest sampleCacheTwoLogical = DescribeCacheClustersRequest
					.builder().cacheClusterId("sampleCacheTwoLogical")
					.showCacheNodeInfo(Boolean.TRUE).build();

			Mockito.when(amazonElastiCache.describeCacheClusters(sampleCacheTwoLogical))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());
			return amazonElastiCache;
		}

		@Bean
		public ListableStackResourceFactory stackResourceFactory() {
			ListableStackResourceFactory resourceFactory = Mockito
					.mock(ListableStackResourceFactory.class);
			Mockito.when(
					resourceFactory.resourcesByType("AWS::ElastiCache::CacheCluster"))
					.thenReturn(Arrays.asList(
							new StackResource("sampleCacheOneLogical", "sampleCacheOne",
									"AWS::ElastiCache::CacheCluster"),
							new StackResource("sampleCacheTwoLogical", "sampleCacheTwo",
									"AWS::ElastiCache::CacheCluster")));
			return resourceFactory;
		}

	}

	@EnableElastiCache(defaultExpiration = 23)
	public static class ApplicationConfigurationWithNoExplicitStackConfigurationAndDefaultExpiration {

		@Bean
		public ElastiCacheClient amazonElastiCache() {
			ElastiCacheClient amazonElastiCache = Mockito.mock(ElastiCacheClient.class);
			int port = TestMemcacheServer.startServer();
			DescribeCacheClustersRequest sampleCacheOneLogical = DescribeCacheClustersRequest
					.builder().cacheClusterId("sampleCacheOneLogical")
					.showCacheNodeInfo(Boolean.TRUE).build();

			Mockito.when(amazonElastiCache.describeCacheClusters(sampleCacheOneLogical))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());

			DescribeCacheClustersRequest sampleCacheTwoLogical = DescribeCacheClustersRequest
					.builder().cacheClusterId("sampleCacheTwoLogical")
					.showCacheNodeInfo(Boolean.TRUE).build();

			Mockito.when(amazonElastiCache.describeCacheClusters(sampleCacheTwoLogical))
					.thenReturn(DescribeCacheClustersResponse.builder()
							.cacheClusters(CacheCluster.builder()
									.configurationEndpoint(Endpoint.builder()
											.address("localhost").port(port).build())
									.engine("memcached").build())
							.build());
			return amazonElastiCache;
		}

		@Bean
		public ListableStackResourceFactory stackResourceFactory() {
			ListableStackResourceFactory resourceFactory = Mockito
					.mock(ListableStackResourceFactory.class);
			Mockito.when(
					resourceFactory.resourcesByType("AWS::ElastiCache::CacheCluster"))
					.thenReturn(Arrays.asList(
							new StackResource("sampleCacheOneLogical", "sampleCacheOne",
									"AWS::ElastiCache::CacheCluster"),
							new StackResource("sampleCacheTwoLogical", "sampleCacheTwo",
									"AWS::ElastiCache::CacheCluster")));
			return resourceFactory;
		}

	}

}
