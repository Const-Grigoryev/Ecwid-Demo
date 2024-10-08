package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.function.Supplier;

import dev.aspid812.ipv4_count.IPv4Count.FailureException;
import dev.aspid812.ipv4_count.IPv4Count.ErrorHandler;


public interface IPv4CountImpl {

	OptionalLong uniqueAddresses();

	void account(ReadableByteChannel input, ErrorHandler errorHandler) throws IOException;

	default void account(Path inputFile, ErrorHandler errorHandler) throws IOException {
		try (var input = FileChannel.open(inputFile)) {
			account(input, errorHandler);
		}
	}

	static IPv4CountImpl forOnline() {
		return new DefaultIPv4CountImpl();
	}

	static IPv4CountImpl forOffline() {
		return DummyIPv4CountImpl.INSTANCE;
	}
}


final class DefaultIPv4CountImpl implements IPv4CountImpl {

	static final int DEFAULT_BUFFER_SIZE = 1 << 20;     // 1 MB, seems to be optimal

	static final long IPv4_SPACE_SIZE = 1L << IPv4Address.SIZE;

	private static final byte[] NEWLINE = new byte[] { '\n' };
	private static final byte[] NOTHING = new byte[] {};

	final Supplier<ByteBuffer> newByteBuffer = () -> ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);

	private final BitScale addressSet = new BitScale(IPv4_SPACE_SIZE);

	@Override
	public OptionalLong uniqueAddresses() {
		return OptionalLong.of(addressSet.count());
	}

	private void accountLine(IPv4LineParser parser, ErrorHandler errorHandler) throws FailureException {
		var lineToken = parser.classify();
		if (lineToken == null)
			throw new IllegalStateException("Parser is not ready");

		switch (lineToken) {
			case ADDRESS:
				var address = parser.getAddress();
				addressSet.witness(Integer.toUnsignedLong(address));
				break;

			case NONSENSE:
				var message = parser.getErrorMessage();
				errorHandler.onError(message);
				break;
		}
	}

	@Override
	public void account(ReadableByteChannel input, ErrorHandler errorHandler) throws IOException {
		var parser = new IPv4LineParser();
		var buffer = newByteBuffer.get().limit(0);
		var eof = false;
		while (!eof) {
			if (!buffer.hasRemaining()) {
				buffer.clear();
				eof = input.read(buffer) == -1;
				buffer.put(eof ? NEWLINE : NOTHING).flip();
			}

			var ready = parser.parseLine(buffer);
			if (ready) {
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

	@Override
	public void account(Path inputFile, ErrorHandler errorHandler) {}
}
