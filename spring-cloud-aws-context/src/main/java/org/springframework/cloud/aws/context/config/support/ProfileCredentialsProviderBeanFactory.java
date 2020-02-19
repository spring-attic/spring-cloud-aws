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

package org.springframework.cloud.aws.context.config.support;

import java.nio.file.Paths;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * @author Kristine Jetzke
 */
public class ProfileCredentialsProviderBeanFactory
		extends AbstractFactoryBean<ProfileCredentialsProvider> {

	private final String profileName;

	private final String profilePath;

	public ProfileCredentialsProviderBeanFactory() {
		this(null, null);
	}

	public ProfileCredentialsProviderBeanFactory(String profileName) {
		this(profileName, null);
	}

	public ProfileCredentialsProviderBeanFactory(String profileName, String profilePath) {
		this.profileName = profileName;
		this.profilePath = profilePath;
	}

	@Override
	public Class<?> getObjectType() {
		return ProfileCredentialsProvider.class;
	}

	@Override
	protected ProfileCredentialsProvider createInstance() throws Exception {
		ProfileCredentialsProvider.Builder builder = ProfileCredentialsProvider.builder();
		if (profileName != null) {
			builder.profileName(profileName);
		}
		if (profilePath != null) {
			builder.profileFile(ProfileFile.builder().content(Paths.get(profilePath))
					.type(ProfileFile.Type.CREDENTIALS).build());
		}
		return builder.build();
	}

}
