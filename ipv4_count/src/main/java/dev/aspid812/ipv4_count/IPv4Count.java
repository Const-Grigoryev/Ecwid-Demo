package dev.aspid812.ipv4_count;

import java.io.InputStream;
import java.io.IOException;
import java.util.OptionalLong;

import dev.aspid812.ipv4_count.impl.*;


public class IPv4Count {

	public enum ControlFlag {
		FAIL,
		PROCEED;

		public static ControlFlag go() {
			return PROCEED;
		}

		public boolean allowsProceeding() {
			return this != FAIL;
		}
	}

	@FunctionalInterface
	public interface ErrorHandler {
		ControlFlag onError(String error);
	}

	final ErrorHandler errorHandler;

	private IPv4CountImpl implementor = IPv4CountImpl.forHealthyState();

	public IPv4Count(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	private IPv4CountImpl switchImplementor(ControlFlag flag) {
		return switch (flag) {
			case FAIL -> IPv4CountImpl.forFailedState();
			default -> implementor;
		};
	}

	public OptionalLong uniqueAddresses() {
		return implementor.uniqueAddresses();
	}

	public void account(InputStream input) throws IOException {
		account(new LightweightInputStreamReader(input));
	}

	public void account(LightweightReader input) throws IOException {
		var flag = ControlFlag.FAIL;
		try {
			flag = implementor.account(input, errorHandler);
		}
		finally {
			implementor = switchImplementor(flag);
		}
	}
}
