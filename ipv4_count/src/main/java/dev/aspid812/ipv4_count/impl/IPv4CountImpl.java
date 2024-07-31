package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.OptionalLong;

import dev.aspid812.ipv4_count.IPv4Count.FailureException;
import dev.aspid812.ipv4_count.IPv4Count.ErrorHandler;


public interface IPv4CountImpl {

	OptionalLong uniqueAddresses();
	void account(Readable input, ErrorHandler errorHandler) throws IOException;

	static IPv4CountImpl forHealthyState() {
		return new DefaultIPv4CountImpl();
	}

	static IPv4CountImpl forFailedState() {
		return DummyIPv4CountImpl.INSTANCE;
	}
}


final class DefaultIPv4CountImpl implements IPv4CountImpl {

	private static final long IPv4_SPACE_SIZE = 1L << IPv4Address.SIZE;

	private final BitScale addressSet = new BitScale(IPv4_SPACE_SIZE);

	@Override
	public OptionalLong uniqueAddresses() {
		return OptionalLong.of(addressSet.count());
	}

	private void account(IPv4LineParser parser, CharBuffer buffer, ErrorHandler errorHandler) throws FailureException {
		while (true) {
			var lineToken = parser.parseLine(buffer, IPv4LineClassifier.INSTANCE);
			if (lineToken == null)
				break;

			switch (lineToken) {
				case VALID_ADDRESS:
					var address = parser.getAddress();
					addressSet.witness(Integer.toUnsignedLong(address));
					break;

				case IRRELEVANT_CONTENT:
					errorHandler.onError(parser.getErrorMessage());
					break;
			}
		}
	}

	@Override
	public void account(Readable input, ErrorHandler errorHandler) throws IOException {
		var parser = new IPv4LineParser();
		var buffer = CharBuffer.allocate(8192);
		var eof = false;
		while (!eof) {
			buffer.clear();
			eof = input.read(buffer) == -1;

			buffer.put(eof ? "\n" : "").flip();
			account(parser, buffer, errorHandler);
		}
	}
}


enum DummyIPv4CountImpl implements IPv4CountImpl {
	INSTANCE;

	@Override
	public OptionalLong uniqueAddresses() {
		return OptionalLong.empty();
	}

	@Override
	public void account(Readable input, ErrorHandler errorHandler) {}
}
