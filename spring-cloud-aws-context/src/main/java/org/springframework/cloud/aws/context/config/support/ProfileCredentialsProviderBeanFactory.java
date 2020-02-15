package org.springframework.cloud.aws.context.config.support;

import java.nio.file.Paths;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;

import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ProfileCredentialsProviderBeanFactory extends AbstractFactoryBean<ProfileCredentialsProvider> {

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
			builder.profileFile(ProfileFile.builder().content(Paths.get(profilePath)).type(ProfileFile.Type.CREDENTIALS).build());
		}
		return builder.build();
	}
}
