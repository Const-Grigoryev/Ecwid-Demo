package dev.aspid812.ipv4_count;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Objects;
import java.util.OptionalLong;

import dev.aspid812.ipv4_count.impl.*;


public final class IPv4Count {

	public static class FailureException extends IOException {
		public FailureException(String message) {
			super(message);
		}
	}

	@FunctionalInterface
	public interface ErrorHandler {

		// Inheritors may throw this instance of the exception from within `onError` method to signal about a user's
		// decision to terminate the counting process entirely.
		FailureException FAILURE = new FailureException("Aborted due to invalid input data");

		void onError(String error) throws FailureException;
	}

	final ErrorHandler errorHandler;

	private IPv4CountImpl implementor = IPv4CountImpl.forHealthyState();

	public IPv4Count(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public OptionalLong uniqueAddresses() {
		return implementor.uniqueAddresses();
	}

	public void account(Path inputFile) throws IOException {
		try {
			implementor.account(inputFile, errorHandler);
		}
		catch (Exception ex) {
			implementor = IPv4CountImpl.forFailedState();
			if (!(ex instanceof FailureException))
				throw ex;
		}
	}

	public void account(ReadableByteChannel input) throws IOException {
		try {
			implementor.account(input, errorHandler);
		}
		catch (Exception ex) {
			implementor = IPv4CountImpl.forFailedState();
			if (!(ex instanceof FailureException))
				throw ex;
		}
	}
}
