package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.util.OptionalLong;

import dev.aspid812.ipv4_count.IPv4Count.ErrorHandler;


public interface IPv4CountImpl {
	OptionalLong uniqueAddresses();
	void account(LightweightReader input, ErrorHandler errorHandler) throws IOException;
}
