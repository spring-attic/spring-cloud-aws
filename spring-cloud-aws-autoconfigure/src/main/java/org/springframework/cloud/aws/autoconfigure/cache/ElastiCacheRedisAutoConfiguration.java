/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.cache;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for AWS ElastiCache Redis support.
 *
 * @author Eddú Meléndez
 * @since 2.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RedisOperations.class, AmazonElastiCache.class })
@AutoConfigureBefore(RedisAutoConfiguration.class)
@EnableConfigurationProperties(ElasticCacheRedisProperties.class)
@ConditionalOnProperty(prefix = "spring.cloud.aws.redis", name = "enabled",
		matchIfMissing = true)
public class ElastiCacheRedisAutoConfiguration {

	private final AWSCredentialsProvider credentialsProvider;

	private final RegionProvider regionProvider;

	private final ElasticCacheRedisProperties properties;

	public ElastiCacheRedisAutoConfiguration(
			ObjectProvider<AWSCredentialsProvider> credentialsProvider,
			ObjectProvider<RegionProvider> regionProvider,
			ElasticCacheRedisProperties properties) {
		this.credentialsProvider = credentialsProvider.getIfAvailable();
		this.regionProvider = properties.getRegion() == null
				? regionProvider.getIfAvailable()
				: new StaticRegionProvider(properties.getRegion());
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(AmazonElastiCache.class)
	public AmazonWebserviceClientFactoryBean<AmazonElastiCacheClient> amazonElastiCache() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonElastiCacheClient.class,
				this.credentialsProvider, this.regionProvider);
	}

	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.aws.redis.names")
	@Primary
	@Bean
	public RedisProperties redisProperties(AmazonElastiCacheClient elastiCacheClient) {
		List<String> clusterNames = this.properties.getNames();

		List<CacheCluster> clusters = clusterNames.stream().flatMap(name -> {
			DescribeCacheClustersRequest describeCacheClustersRequest = new DescribeCacheClustersRequest()
					.withCacheClusterId(name).withShowCacheNodeInfo(true);

			DescribeCacheClustersResult cacheClustersResult = elastiCacheClient
					.describeCacheClusters(describeCacheClustersRequest);
			return cacheClustersResult.getCacheClusters().stream();
		}).collect(Collectors.toList());

		CacheCluster cacheCluster = clusters.get(0); // The first result is taken to set
														// ssl and password

		RedisProperties redisProperties = new RedisProperties();

		boolean isCluster = clusters.size() > 1;
		if (isCluster) {
			RedisProperties.Cluster cluster = resolveCluster(clusters);
			redisProperties.setCluster(cluster);
		}
		else {
			String address = cacheCluster.getCacheNodes().get(0).getEndpoint()
					.getAddress();
			int port = cacheCluster.getCacheNodes().get(0).getEndpoint().getPort();

			redisProperties.setHost(address);
			redisProperties.setPort(port);
		}

		boolean tlsEnabled = cacheCluster.getTransitEncryptionEnabled();
		redisProperties.setSsl(tlsEnabled);

		if (cacheCluster.isAuthTokenEnabled()) {
			String password = this.properties.getToken();
			redisProperties.setPassword(password);
		}

		return redisProperties;
	}

	private RedisProperties.Cluster resolveCluster(List<CacheCluster> clusters) {
		RedisProperties.Cluster cluster = new RedisProperties.Cluster();
		List<String> nodes = clusters.stream().map(cacheCluster -> {
			String address = cacheCluster.getConfigurationEndpoint().getAddress();
			int port = cacheCluster.getConfigurationEndpoint().getPort();
			return address + ":" + port;
		}).collect(Collectors.toList());
		cluster.setNodes(nodes);
		return cluster;
	}

}
