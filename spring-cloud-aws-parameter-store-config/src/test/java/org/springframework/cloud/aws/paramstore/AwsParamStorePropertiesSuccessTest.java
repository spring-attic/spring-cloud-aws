package org.springframework.cloud.aws.paramstore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { AwsParamStoreProperties.CONFIG_PREFIX + ".prefix=/con",
		AwsParamStoreProperties.CONFIG_PREFIX + ".default-context=app",
		AwsParamStoreProperties.CONFIG_PREFIX + ".profile-separator=." }, classes = {
				AwsParamStoreConfiguration.class })
public class AwsParamStorePropertiesSuccessTest {

	@Autowired
	AwsParamStoreProperties awsParamStoreProperties;

	@Test
	void awsParamStorePropertiesAreLoaded() {
		assertThat(awsParamStoreProperties).isNotNull();
		assertThat(awsParamStoreProperties.getPrefix()).isEqualTo("/con");
		assertThat(awsParamStoreProperties.getPrefix()).isNotNull();
		assertThat(awsParamStoreProperties.getDefaultContext()).isEqualTo("app");
		assertThat(awsParamStoreProperties.getDefaultContext()).isNotNull();
		assertThat(awsParamStoreProperties.getProfileSeparator()).isEqualTo(".");
		assertThat(awsParamStoreProperties.getProfileSeparator()).isNotNull();
	}
}
