package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.io.Reader;
public class IPv4Parser {

    private static final int BITS_PER_OCTET = 8;
    private static final int OCTETS_PER_ADDRESS = 4;
    private static final int OCTET_MASK = ~(-1 << BITS_PER_OCTET);

    public enum ParseResult {
        SUCCESS,
        ERROR,
        END_OF_FILE
    }

    private enum State {
        INIT,
        OCTET_START,
        OCTET,
        OCTET_END,
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
        int octetsRead = 0;
        String error = null;
        Integer octet = null, address = null;
        var state = State.INIT;
        while (state != State.STOP) {
            int charCode = reader.read();
            char ch = charCode == -1 ? '\n' : (char) charCode;
            state = switch (state) {
                case INIT:
                    address = 0;
                    if (ch == '\n')
                        yield State.STOP;

                case OCTET_START:
                    octet = 0;
                    if (ch == '0')
                        yield State.OCTET_END;

                case OCTET:
                    if ('0' <= ch && ch <= '9') {
                        octet = octet * 10 + (ch - '0');
                        if ((octet & ~OCTET_MASK) == 0)
                            yield State.OCTET;
                    }

                case OCTET_END:
                    if (ch == '.' || ch == '\n') {
                        address = (address << BITS_PER_OCTET) | octet;
                        octetsRead++;
                        if (ch == '.' && octetsRead < OCTETS_PER_ADDRESS)
                            yield State.OCTET_START;
                        if (ch == '\n' && octetsRead == OCTETS_PER_ADDRESS)
                            yield State.STOP;
                    }

                case ERROR:     // Dummy label, may be reached only by falling-through
                    error = "Shit happens";

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
            return ParseResult.ERROR;
        }

        if (octetsRead == 0) {
            return ParseResult.END_OF_FILE;
        }

        sink.accept(address);
        return ParseResult.SUCCESS;
    }
}
