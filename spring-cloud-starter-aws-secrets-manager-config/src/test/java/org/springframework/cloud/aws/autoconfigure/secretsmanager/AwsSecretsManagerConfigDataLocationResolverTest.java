package org.springframework.cloud.aws.autoconfigure.secretsmanager;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsSecretsManagerConfigDataLocationResolverTest {

	@Test
	public void testResolveProfileSpecificWithAutomaticPaths() {
		String location = "aws-secretsmanager:";
		List<AwsSecretsManagerConfigDataResource> locations = testResolveProfileSpecific(location);
		assertThat(locations).hasSize(4);
		assertThat(toContexts(locations)).containsExactly("/secret/testapp_dev", "/secret/testapp",
				"/secret/application_dev", "/secret/application");
	}

	@Test
	public void testResolveProfileSpecificWithCustomPaths() {
		String location = "aws-secretsmanager:/mypath1;/mypath2;/mypath3";
		List<AwsSecretsManagerConfigDataResource> locations = testResolveProfileSpecific(location);
		assertThat(locations).hasSize(3);
		assertThat(toContexts(locations)).containsExactly("/mypath1", "/mypath2", "/mypath3");
	}

	private List<String> toContexts(List<AwsSecretsManagerConfigDataResource> locations) {
		return locations.stream().map(AwsSecretsManagerConfigDataResource::getContext).collect(Collectors.toList());
	}

	private List<AwsSecretsManagerConfigDataResource> testResolveProfileSpecific(String location) {
		AwsSecretsManagerConfigDataLocationResolver resolver = createResolver();
		ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.application.name", "testapp");
		when(context.getBinder()).thenReturn(Binder.get(env));
		Profiles profiles = mock(Profiles.class);
		when(profiles.getAccepted()).thenReturn(Collections.singletonList("dev"));
		return resolver.resolveProfileSpecific(context, ConfigDataLocation.of(location), profiles);
	}

	private AwsSecretsManagerConfigDataLocationResolver createResolver() {
		return new AwsSecretsManagerConfigDataLocationResolver() {
			@Override
			public <T> void registerBean(ConfigDataLocationResolverContext context, Class<T> type, T instance) {
				// do nothing
			}

			@Override
			protected <T> void registerBean(ConfigDataLocationResolverContext context, Class<T> type,
					BootstrapRegistry.InstanceSupplier<T> supplier) {
				// do nothing
			}

			@Override
			protected <T> void registerAndPromoteBean(ConfigDataLocationResolverContext context, Class<T> type,
					BootstrapRegistry.InstanceSupplier<T> supplier) {
				// do nothing
			}
		};
	}

}
