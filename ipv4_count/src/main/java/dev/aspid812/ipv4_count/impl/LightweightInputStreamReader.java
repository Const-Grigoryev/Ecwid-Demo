package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.io.InputStream;


public final class LightweightInputStreamReader extends LightweightReader {

	private static final int BYTESEQ_LF   = 0x0A;
	private static final int BYTESEQ_CR   = 0x0D;
	private static final int BYTESEQ_CRLF = 0x0D0A;

	private static final int BYTESEQ_MASK_XX   = 0xFF;
	private static final int BYTESEQ_MASK_XXXX = 0xFFFF;

	private static final int LOOKBACK_SIZE = 3 * Byte.SIZE;
	private static final int LOOKBACK_WINDOW = ~(-1 << LOOKBACK_SIZE);

	final InputStream input;

	private int lookback = 0;

	public LightweightInputStreamReader(InputStream input) {
		this.input = input;
	}

	int decode(int bytes, int defaultValue) {
		var b2 = bytes & BYTESEQ_MASK_XXXX;
		if (b2 == BYTESEQ_CRLF)
			return defaultValue;

		// Covers the most common cases, but not all of them. See: https://en.wikipedia.org/wiki/Newline#Unicode
		var b1 = bytes & BYTESEQ_MASK_XX;
		if (b1 == BYTESEQ_CR || b1 == BYTESEQ_LF)
			return '\n';

		return b1;
	}

	public boolean eof() {
		return lookback == -1;
	}

	@Override
	public int read() throws IOException {
		var buf = lookback;
		var ch = -1;
		while (ch == -1) {
			buf = (buf << Byte.SIZE) | input.read();
			if (buf == -1)
				break;

			ch = decode(buf, ch);
			buf &= LOOKBACK_WINDOW;
		}
		lookback = buf;
		return ch;
	}

	@Override
	public void close() throws IOException {
		input.close();
	}
}
