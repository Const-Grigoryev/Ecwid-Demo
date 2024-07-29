package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.OptionalLong;

import dev.aspid812.ipv4_count.IPv4Count.ControlFlag;
import dev.aspid812.ipv4_count.IPv4Count.ErrorHandler;
import dev.aspid812.ipv4_count.impl.MutableIPv4Line.LineToken;


public interface IPv4CountImpl {

	OptionalLong uniqueAddresses();
	ControlFlag account(LightweightReader input, ErrorHandler errorHandler) throws IOException;

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

	@Override
	//TODO: Adjust the signature, since this method is not `Lightweight`-specific anymore
	public ControlFlag account(LightweightReader input, ErrorHandler errorHandler) throws IOException {
		var line = new MutableIPv4Line();
		var flag = ControlFlag.go();
		while (flag.allowsProceeding()) {
			var lineToken = (LineToken) null;
			try {
				lineToken = line.parseLine(input);
			}
			catch (UncheckedIOException ex) {
				throw ex.getCause();
			}
			if (lineToken == null)
				break;

			flag = switch (lineToken) {
				case VALID_ADDRESS:
					var address = line.getAddress();
					addressSet.witness(Integer.toUnsignedLong(address));
					yield flag;

				case IRRELEVANT_CONTENT:
					yield errorHandler.onError(line.getErrorMessage());

				case NOTHING:
					yield flag;
			};
		}

		return flag;
	}
}


enum DummyIPv4CountImpl implements IPv4CountImpl {
	INSTANCE;

	@Override
	public OptionalLong uniqueAddresses() {
		return OptionalLong.empty();
	}

	@Override
	public ControlFlag account(LightweightReader input, ErrorHandler errorHandler) {
		return ControlFlag.FAIL;
	}
}
