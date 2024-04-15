package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.io.Reader;
public class IPv4Parser {

    private static final int BITS_PER_OCTET = 8;
    private static final int OCTETS_PER_ADDRESS = 4;
    private static final int OCTET_MASK = ~(-1 << BITS_PER_OCTET);

    public enum ParseResult {
        ADDRESS,
        MISTAKE,
        NOTHING
    }

    private enum State {
        OCTET,
        OCTET_DIGIT,
        ERROR,
        FAST_FORWARD,
        STOP
    }

    final Reader reader;

    private String lastError;

    public IPv4Parser(Reader reader) {
        this.reader = reader;
    }

    public String getLastError() {
        return lastError;
    }

    @SuppressWarnings("fallthrough")
    public ParseResult parseNextLine(IPv4Builder sink) throws IOException {
        var address = 0x00;
        var error = (String) null;
        var octets = 0;
        var state = State.OCTET_DIGIT;
        while (state != State.STOP) {
            var charCode = reader.read();
            var ch = charCode == -1 ? '\n' : (char) charCode;
            state = switch (state) {
                case OCTET:
                    if (ch == '.' && ++octets < OCTETS_PER_ADDRESS) {
                        address <<= BITS_PER_OCTET;
                        yield State.OCTET_DIGIT;
                    }
                    if (ch == '\n' && ++octets == OCTETS_PER_ADDRESS) {
                        yield State.STOP;
                    }

                case OCTET_DIGIT:
                    if ('0' <= ch && ch <= '9') {
                        var octet = address & OCTET_MASK;
                        address ^= (octet ^= octet * 10 + (ch - '0'));
                        if ((octet & ~OCTET_MASK) != 0) {
                            error = "Invalid octet value";
                            yield State.FAST_FORWARD;
                        }
                        yield State.OCTET;
                    }

                case ERROR:     // Dummy label, may be reached only by falling-through
                    error = "Unexpected character";
                    yield State.FAST_FORWARD;

                case FAST_FORWARD:
                    if (ch == '\n')
                        yield State.STOP;
                    yield State.FAST_FORWARD;

                case STOP:      // Actually unreachable, just make the compiler happy
                    throw new RuntimeException();
            };
        }

        lastError = error;
        if (error != null) {
            return ParseResult.MISTAKE;
        }

        sink.accept(address);
        return ParseResult.ADDRESS;
    }
}
