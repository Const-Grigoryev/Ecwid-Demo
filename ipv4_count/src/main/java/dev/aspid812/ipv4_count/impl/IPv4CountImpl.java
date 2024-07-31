package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.OptionalLong;

import dev.aspid812.ipv4_count.IPv4Count.FailureException;
import dev.aspid812.ipv4_count.IPv4Count.ErrorHandler;


public interface IPv4CountImpl {

	OptionalLong uniqueAddresses();
	void account(ReadableByteChannel input, ErrorHandler errorHandler) throws IOException;

	static IPv4CountImpl forHealthyState() {
		return new DefaultIPv4CountImpl();
	}

	static IPv4CountImpl forFailedState() {
		return DummyIPv4CountImpl.INSTANCE;
	}
}


final class DefaultIPv4CountImpl implements IPv4CountImpl {

	static final int DEFAULT_BUFFER_SIZE = 8192;

	static final long IPv4_SPACE_SIZE = 1L << IPv4Address.SIZE;

	private static final byte[] NEWLINE = new byte[] { '\n' };
	private static final byte[] NOTHING = new byte[0];

	private final BitScale addressSet = new BitScale(IPv4_SPACE_SIZE);

	@Override
	public OptionalLong uniqueAddresses() {
		return OptionalLong.of(addressSet.count());
	}

	private void accountLine(IPv4LineParser parser, ErrorHandler errorHandler) throws FailureException {
		switch (parser.classify()) {
			case VALID_ADDRESS:
				var address = parser.getAddress();
				addressSet.witness(Integer.toUnsignedLong(address));
				break;

			case IRRELEVANT_CONTENT:
				var message = parser.getErrorMessage();
				errorHandler.onError(message);
				break;
		}
	}

	@Override
	public void account(ReadableByteChannel input, ErrorHandler errorHandler) throws IOException {
		var parser = new IPv4LineParser();
		var buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE).limit(0);
		var eof = false;
		while (!eof) {
			if (!buffer.hasRemaining()) {
				buffer.clear();
				eof = input.read(buffer) == -1;
				buffer.put(eof ? NEWLINE : NOTHING).flip();
			}

			parser.parseLine(buffer);
			if (parser.ready()) {
				accountLine(parser, errorHandler);
			}
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
	public void account(ReadableByteChannel input, ErrorHandler errorHandler) {}
}
