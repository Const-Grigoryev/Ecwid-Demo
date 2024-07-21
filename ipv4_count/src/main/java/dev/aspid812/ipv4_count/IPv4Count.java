package dev.aspid812.ipv4_count;

import java.io.InputStream;
import java.io.IOException;

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

	static BitScale newAddressSet() {
		return new BitScale(IPv4_SPACE_SIZE);
	}

	static BitScale accumulate(BitScale addressSet, LightweightReader input, ErrorHandler errorHandler) throws IOException {
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

		return addressSet;
	}

	public long countUnique(InputStream input, ErrorHandler errorHandler) throws IOException {
		return countUnique(new LightweightInputStreamReader(input), errorHandler);
	}

	public long countUnique(LightweightReader input, ErrorHandler errorHandler) throws IOException {
		return accumulate(newAddressSet(), input, errorHandler).count();
	}
}
