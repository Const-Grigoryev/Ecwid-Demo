package dev.aspid812.ipv4_count;

import dev.aspid812.ipv4_count.impl.BitScale;
import dev.aspid812.ipv4_count.impl.IPv4Address;
import dev.aspid812.ipv4_count.impl.MutableIPv4Line;

import java.io.*;
import java.nio.charset.Charset;


public class IPv4Count {

	private static final long IPv4_SPACE_SIZE = 1L << IPv4Address.SIZE;

	private static final int READER_BUFFER_LENGTH = 0x100000;

	public enum ControlFlag {
		TERMINATE,
		PROCEED
	}

	@FunctionalInterface
	public interface ErrorHandler {
		ControlFlag onError(String error);
	}

	static BitScale newAddressSet() {
		return new BitScale(IPv4_SPACE_SIZE);
	}

	static BitScale accumulate(BitScale addressSet, Reader input, ErrorHandler errorHandler) throws IOException {
		var line = new MutableIPv4Line();
		var flag = ControlFlag.PROCEED;
		while (flag != ControlFlag.TERMINATE) {
			var lineToken = line.parseLine(input);
			flag = switch (lineToken) {
				case VALID_ADDRESS:
					var address = line.getAddress();
					addressSet.witness(Integer.toUnsignedLong(address));
					yield flag;

				case IRRELEVANT_CONTENT:
					yield errorHandler.onError(line.getErrorMessage());

				case NOTHING:
					yield ControlFlag.TERMINATE;
			};
		}

		return addressSet;
	}

	public long countUnique(InputStream input, Charset charset, ErrorHandler errorHandler) throws IOException {
		return countUnique(new InputStreamReader(input, charset), errorHandler);
	}

	public long countUnique(Reader input, ErrorHandler errorHandler) throws IOException {
		var linewiseInput = input instanceof LineNumberReader
				? (LineNumberReader) input
				: new LineNumberReader(input, READER_BUFFER_LENGTH);
		return accumulate(newAddressSet(), linewiseInput, errorHandler).count();
	}
}
