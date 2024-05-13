package dev.aspid812.ipv4_count.impl;

@FunctionalInterface
public interface IPv4Builder {
	void accept(int address);
}
