package org.springframework.cloud.aws.core.region;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DynamicRegionProviderTests {

	private static final String UNABLE_TO_RETRIEVE_THE_REGION = "Unable to retrieve the region";

	private final RegionProvider regionProvider = new DynamicRegionProvider();

	@Test
	void shouldFailWhenTheRegionIsNotProvided() {
		IllegalStateException exception = Assertions
			.assertThrows(IllegalStateException.class, regionProvider::getRegion);

		assertThat(exception.getMessage(), is(UNABLE_TO_RETRIEVE_THE_REGION));
	}

	@Test
	void shouldRetrieveTheRegionWhenItsProvided() {
		System.setProperty(AWS_REGION_SYSTEM_PROPERTY, Regions.EU_WEST_1.getName());

		Region region = regionProvider.getRegion();

		assertThat(region.getName(), is(Regions.EU_WEST_1.getName()));
	}
}
