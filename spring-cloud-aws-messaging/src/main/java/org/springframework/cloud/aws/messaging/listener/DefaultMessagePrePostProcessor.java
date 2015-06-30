package org.springframework.cloud.aws.messaging.listener;

import org.springframework.messaging.Message;

/**
 * This DefaultMessagePrePostProcessor does nothing. 
 * @author Ruwen Schwedewsky
 *
 */
public class DefaultMessagePrePostProcessor implements MessagePrePostProcessor{

	@Override
	public void beforeMessageProcessing(Message<String> message) {
	}

	@Override
	public void afterSuccessfulMessageProcessing(Message<String> message) {
	}

	@Override
	public void afterEveryMessageProcessing(Message<String> message) {
	}

}
