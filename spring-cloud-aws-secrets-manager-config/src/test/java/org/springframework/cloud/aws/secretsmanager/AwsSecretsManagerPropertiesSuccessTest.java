package org.springframework.cloud.aws.secretsmanager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { AwsSecretsManagerProperties.CONFIG_PREFIX + ".prefix=/sec",
		AwsSecretsManagerProperties.CONFIG_PREFIX + ".default-context=app",
		AwsSecretsManagerProperties.CONFIG_PREFIX + ".profile-separator=." }, classes = {
				AwsSecretsManagerConfiguration.class })
public class AwsSecretsManagerPropertiesSuccessTest {

	@Autowired
	AwsSecretsManagerProperties awsSecretsManagerProperties;

	@Test
	void awsSecretsManagerPropertiesAreLoaded() {
		assertThat(awsSecretsManagerProperties).isNotNull();
		assertThat(awsSecretsManagerProperties.getPrefix()).isEqualTo("/sec");
		assertThat(awsSecretsManagerProperties.getPrefix()).isNotNull();
		assertThat(awsSecretsManagerProperties.getDefaultContext()).isEqualTo("app");
		assertThat(awsSecretsManagerProperties.getDefaultContext()).isNotNull();
		assertThat(awsSecretsManagerProperties.getProfileSeparator()).isEqualTo(".");
		assertThat(awsSecretsManagerProperties.getProfileSeparator()).isNotNull();
	}
}
