package org.springframework.cloud.aws.paramstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:application-validation.properties")
public class AwsParamStorePropertiesValidationTest {

	public static void main(String[] args) {
		SpringApplication.run(AwsParamStorePropertiesValidationTest.class, args);
	}
}
