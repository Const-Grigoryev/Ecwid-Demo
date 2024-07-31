package dev.aspid812.ipv4_count.impl;


public final class MutableIPv4Line implements IPv4LineParser.Visitor<MutableIPv4Line.LineToken> {

	public enum LineToken {
		VALID_ADDRESS,
		IRRELEVANT_CONTENT,
		NOTHING
	}

	private int address;
	private String message;

	public int getAddress() {
		return address;
	}

	public String getErrorMessage() {
		return message;
	}

	@Override
	public LineToken address(int address) {
		this.address = address;
		this.message = null;
		return LineToken.VALID_ADDRESS;
	}

	@Override
	public LineToken mistake(String message) {
		this.address = 0;
		this.message = message;
		return LineToken.IRRELEVANT_CONTENT;
	}

	@Override
	public LineToken nothing() {
		this.address = 0;
		this.message = null;
		return LineToken.NOTHING;
	}
}
