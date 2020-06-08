package org.springframework.cloud.aws.secretsmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:application-validation.properties")
public class AwsSecretsManagerPropertiesValidationTest {

	public static void main(String[] args) {
		SpringApplication.run(AwsSecretsManagerPropertiesValidationTest.class, args);
	}
}
