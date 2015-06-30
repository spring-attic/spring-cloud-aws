package org.springframework.cloud.aws.messaging.listener;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * This class provides methods which are executed before or after the processing of a
 * message. The messages are not converted yet. This can be useful for example to
 * introduce central logging. Implementations must be thread-safe.
 * @author Ruwen Schwedewsky
 *
 */
public interface MessagePrePostProcessor {
	/**
	 * This method will be executed before the message is consumed. If this message throws
	 * an MessagingException, the execution stops and the message will not be processed.
	 * But the failure is handled.
	 * @param message
	 * @throws MessagingException
	 */
	public void beforeMessageProcessing(Message<String> message)
			throws MessagingException;

	/**
	 * This method will be executed after a message has been successfully consumed. If
	 * this method throws an MessagingException, the processing will be considered as
	 * failed. But the failure is handled.
	 * @param message
	 * @throws MessagingException
	 */
	public void afterSuccessfulMessageProcessing(Message<String> message)
			throws MessagingException;

	/**
	 * This method will always be executed, no matter if the processing of the message
	 * failed or not. If this method throws an MessagingException, the processing will be
	 * considered as failed. But the failure is handled.
	 * @param message
	 * @throws MessagingException
	 */
	public void afterEveryMessageProcessing(Message<String> message)
			throws MessagingException;
}
