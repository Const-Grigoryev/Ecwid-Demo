package dev.aspid812.ipv4_count;

import dev.aspid812.ipv4_count.impl.BitScale;
import dev.aspid812.ipv4_count.impl.IPv4Parser;
import dev.aspid812.ipv4_count.impl.IPv4Parser.ParseResult;

import java.io.*;
import java.nio.charset.Charset;

public class IPv4Count {

    private static final long IPv4_SPACE_SIZE = 1L << 32;

    private static final int READER_BUFFER_LENGTH = 0x100000;


    @FunctionalInterface
    public interface ErrorHandler {
        boolean DECISION_TERMINATE = true;
        boolean DECISION_PROCEED = false;

        boolean onError(String error);
    }

    static BitScale newAddressSet() {
        return new BitScale(IPv4_SPACE_SIZE);
    }

    static BitScale accumulate(BitScale addressSet, IPv4Parser parser, ErrorHandler errorHandler) throws IOException {
        var done = false;
        while (!done) {
            var status = parser.parseNextLine(address -> {
                addressSet.witness(Integer.toUnsignedLong(address));
            });
            done = switch (status) {
                case MISTAKE:
                    yield errorHandler.onError(parser.getLastError());
                default:
                    yield status == ParseResult.NOTHING;
            };
        }

        return addressSet;
    }

    public long countUnique(InputStream input, Charset charset, ErrorHandler errorHandler) throws IOException {
        return countUnique(new InputStreamReader(input, charset), errorHandler);
    }

    public long countUnique(Reader input, ErrorHandler errorHandler) throws IOException {
        var linewiseInput = input instanceof LineNumberReader
            ? (LineNumberReader) input
            : new LineNumberReader(input, READER_BUFFER_LENGTH);
        var parser = new IPv4Parser(linewiseInput);

        return accumulate(newAddressSet(), parser, errorHandler).count();
    }
}
