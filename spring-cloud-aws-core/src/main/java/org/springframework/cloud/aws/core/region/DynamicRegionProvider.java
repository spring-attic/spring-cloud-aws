package org.springframework.cloud.aws.core.region;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;

/**
 * Dynamic {@link RegionProvider} implementation that retrieves the region using {@link DefaultAwsRegionProviderChain}.
 *
 * @author Oleksandr Hladun
 * @since 2.2.3
 */
public class DynamicRegionProvider implements RegionProvider {

	private final AwsRegionProvider awsRegionProvider;

	public DynamicRegionProvider() {
		this.awsRegionProvider = new DefaultAwsRegionProviderChain();
	}

	@Override
	public Region getRegion() {
		try {
			return RegionUtils.getRegion(awsRegionProvider.getRegion());
		}
		catch (SdkClientException e) {
			throw new IllegalStateException("Unable to retrieve the region", e);
		}
	}
}
