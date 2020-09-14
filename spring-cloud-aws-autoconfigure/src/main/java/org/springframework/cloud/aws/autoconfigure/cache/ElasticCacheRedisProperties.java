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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AWS ElastiCache.
 *
 * @author Eddú Meléndez
 * @since 2.3.0
 */
@ConfigurationProperties(prefix = "spring.cloud.aws.redis")
public class ElasticCacheRedisProperties {

	/**
	 * Cluster id.
	 */
	private String name;

	/**
	 * Authentication token.
	 */
	private String token;

	/**
	 * Overrides the default region.
	 */
	private String region;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getToken() {
		return this.token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getRegion() {
		return this.region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

}
