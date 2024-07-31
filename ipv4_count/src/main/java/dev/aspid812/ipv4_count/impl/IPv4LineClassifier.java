package dev.aspid812.ipv4_count.impl;


public enum IPv4LineClassifier implements IPv4LineParser.Visitor<IPv4LineClassifier.LineToken> {
	INSTANCE;

	public enum LineToken {
		VALID_ADDRESS,
		IRRELEVANT_CONTENT,
		NOTHING
	}

	@Override
	public LineToken address(int address) {
		return LineToken.VALID_ADDRESS;
	}

	@Override
	public LineToken mistake(String message) {
		return LineToken.IRRELEVANT_CONTENT;
	}

	@Override
	public LineToken nothing() {
		return LineToken.NOTHING;
	}
}
