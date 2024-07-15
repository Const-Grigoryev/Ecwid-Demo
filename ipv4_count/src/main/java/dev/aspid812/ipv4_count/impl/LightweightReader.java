package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;

import org.jetbrains.annotations.NotNull;


//TODO: Optimize bulk `read()` operations to get rid of unnecessary allocations
public abstract class LightweightReader extends Reader {

	public abstract boolean eof();

	@Override
	public int read() throws IOException {
		throw new UnsupportedOperationException(getClass().getName() + ".read");
	}

	@Override
	public int read(@NotNull CharBuffer dest) throws IOException {
		if (dest.isReadOnly())
			throw new ReadOnlyBufferException();

		if (eof())
			return -1;

		var supply = 0;
		var demand = dest.remaining();
		while (supply < demand) {
			var ch = read();
			if (ch == -1)
				break;

			dest.put((char) ch);
			supply++;
		}
		return supply;
	}

	@Override
	public int read(@NotNull char[] destArray) throws IOException {
		var dest = CharBuffer.wrap(destArray);
		return read(dest);
	}

	@Override
	public int read(@NotNull char[] destArray, int destOffset, int demand) throws IOException {
		var dest = CharBuffer.wrap(destArray).slice(destOffset, demand);
		return read(dest);
	}
}
