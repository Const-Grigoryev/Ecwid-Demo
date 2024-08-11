package dev.aspid812.ipv4_count.impl;


// The API below was partly inspired by `java.lang.Integer`
public enum IPv4Address {;

	public static final int OCTETS_PER_ADDRESS = 4;
	public static final int BITS_PER_OCTET = 8;
	public static final int OCTET_MASK = ~(-1 << BITS_PER_OCTET);

	public static final int SIZE = BITS_PER_OCTET * OCTETS_PER_ADDRESS;

	// The next two methods are purposed for debug and testing only. They are intentionally kept simple
	// by the cost of optimization and proper error handling.
	public static int parseInt(String str) {
		var octets = str.split("\\.", OCTETS_PER_ADDRESS);
		return
			(Integer.parseInt(octets[0]) << BITS_PER_OCTET * 3) |
			(Integer.parseInt(octets[1]) << BITS_PER_OCTET * 2) |
			(Integer.parseInt(octets[2]) << BITS_PER_OCTET) |
			(Integer.parseInt(octets[3]));
	}

	public static String toString(int value) {
		return String.join(".",
			Integer.toString(OCTET_MASK & (value >> BITS_PER_OCTET * 3)),
			Integer.toString(OCTET_MASK & (value >> BITS_PER_OCTET * 2)),
			Integer.toString(OCTET_MASK & (value >> BITS_PER_OCTET)),
			Integer.toString(OCTET_MASK & (value))
		);
	}
}
