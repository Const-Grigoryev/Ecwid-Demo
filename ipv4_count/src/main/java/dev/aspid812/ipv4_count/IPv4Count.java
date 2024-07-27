package dev.aspid812.ipv4_count;

import java.io.InputStream;
import java.io.IOException;
import java.util.OptionalLong;

import dev.aspid812.ipv4_count.impl.*;


public class IPv4Count {

	private static final long IPv4_SPACE_SIZE = 1L << IPv4Address.SIZE;

	public enum ControlFlag {
		TERMINATE,
		PROCEED;

		static ControlFlag go() {
			return PROCEED;
		}

		boolean allowsProceeding() {
			return this != TERMINATE;
		}
	}

	@FunctionalInterface
	public interface ErrorHandler {
		ControlFlag onError(String error);
	}

	private static IPv4CountImpl newImplementor() {
		return new IPv4CountImpl() {
			@Override
			public OptionalLong uniqueAddresses() {
				return OptionalLong.of(addressSet.count());
			}

			@Override
			public void account(LightweightReader input, ErrorHandler errorHandler) throws IOException {
				var line = new MutableIPv4Line();
				var flag = ControlFlag.go();
				while (flag.allowsProceeding()) {
					var lineToken = line.parseLine(input);
					flag = switch (lineToken) {
						case VALID_ADDRESS:
							var address = line.getAddress();
							addressSet.witness(Integer.toUnsignedLong(address));
							yield flag;

						case IRRELEVANT_CONTENT:
							yield errorHandler.onError(line.getErrorMessage());

						case NOTHING:
							yield input.eof() ? ControlFlag.TERMINATE : ControlFlag.PROCEED;
					};
				}
			}

			private final BitScale addressSet = new BitScale(IPv4_SPACE_SIZE);
		};
	}

	final ErrorHandler errorHandler;

	private final IPv4CountImpl implementor = newImplementor();

	public IPv4Count(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public OptionalLong uniqueAddresses() {
		return implementor.uniqueAddresses();
	}

	public void account(InputStream input) throws IOException {
		account(new LightweightInputStreamReader(input));
	}

	public void account(LightweightReader input) throws IOException {
		implementor.account(input, errorHandler);
	}
}
