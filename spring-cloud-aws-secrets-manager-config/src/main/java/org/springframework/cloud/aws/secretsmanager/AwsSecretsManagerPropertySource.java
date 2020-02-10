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

package org.springframework.cloud.aws.secretsmanager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import org.springframework.core.env.EnumerablePropertySource;

/**
 * Retrieves secret value under the given context / path from the AWS Secrets Manager
 * using the provided SM client.
 *
 * @author Fabio Maia
 * @since 2.0.0
 */
public class AwsSecretsManagerPropertySource
		extends EnumerablePropertySource<SecretsManagerClient> {

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private String context;

	private Map<String, Object> properties = new LinkedHashMap<>();

	public AwsSecretsManagerPropertySource(String context,
			SecretsManagerClient smClient) {
		super(context, smClient);
		this.context = context;
	}

	public void init() {
		GetSecretValueRequest secretValueRequest = GetSecretValueRequest.builder()
				.secretId(context).build();
		readSecretValue(secretValueRequest);
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = properties.keySet();
		return strings.toArray(new String[strings.size()]);
	}

	@Override
	public Object getProperty(String name) {
		return properties.get(name);
	}

	private void readSecretValue(GetSecretValueRequest secretValueRequest) {
		try {
			GetSecretValueResponse secretValueResult = source
					.getSecretValue(secretValueRequest);
			Map<String, Object> secretMap = jsonMapper.readValue(
					secretValueResult.secretString(),
					new TypeReference<Map<String, Object>>() {
					});

			for (Map.Entry<String, Object> secretEntry : secretMap.entrySet()) {
				properties.put(secretEntry.getKey(), secretEntry.getValue());
			}
		}
		catch (ResourceNotFoundException e) {
			logger.debug("AWS secret not found from " + secretValueRequest.secretId());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
