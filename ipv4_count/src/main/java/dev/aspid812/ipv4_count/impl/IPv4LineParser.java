package dev.aspid812.ipv4_count.impl;

import java.nio.ByteBuffer;

import static dev.aspid812.ipv4_count.impl.IPv4Address.BITS_PER_OCTET;
import static dev.aspid812.ipv4_count.impl.IPv4Address.OCTET_MASK;


public final class IPv4LineParser {

	public enum LineToken {
		ADDRESS,
		NONSENSE,
		EMPTY_LINE
	}

	// Automaton's state summarizes a consumed portion (`p`) of the input string. We store it as a plain `int`,
	// whose parts have the following meaning: (1)-(4).

	// (1) When `p` contains some octets, and the last one isn't complete yet (i.e., `p` ends with a digit),
	// then the lowest bit of the state (_open octet flag_) is set to 1. If `p` is empty, or contains some octets,
	// each of which is closed by a following dot, then that bit is 0.
	private static final int ST_OPEN_OCTET_FLAG = 0x0000_0001;

	// (2) The next two bits count the number of closed octets within `p`. It may only be 0, 1, 2 or 3, because
	// the fourth octet is never followed by a closing dot.
	private static final int ST_OCTET_COUNT_MASK = 0x0000_0006;

	// (3) Next pack of bits is reserved to indicate various errors. In a normal situation, they all are zeros.
	private static final int ST_ERROR_MASK = 0x7FFF_FFF8;

	// (4) The highest bit is set when the parser encounters an EOL character. It marks the current state as _final_
	// and breaks a parsing loop. Since this flag is the highest bit of a signed integer, one can be easily check it
	// by comparing to zero: the state is final if and only if `state < 0`.
	private static final int ST_FINAL_FLAG = 0x8000_0000;

	private static final int ST_INIT = 0;
	private static final int ST_CLOSED_OCTET = ST_INIT;
	private static final int ST_OPEN_OCTET = ST_INIT | ST_OPEN_OCTET_FLAG;

	private static final int ST_ERR_UNEXPECTED_CHARACTER = 0x08;
	private static final int ST_ERR_INVALID_OCTET = 0x09;
	private static final int ST_FIN_EMPTY_LINE = ST_FINAL_FLAG;
	private static final int ST_FIN_ADDRESS = ST_OCTET_COUNT_MASK | ST_OPEN_OCTET_FLAG | ST_FINAL_FLAG;

	private int state = ST_INIT;
	private int address = 0x00;

	public boolean parseLine(ByteBuffer input) {
		// If the given state is not final, unpack it to local variables. Otherwise, reset to initial state.
		var restore = state >= 0;
		var state = restore ? this.state : ST_INIT;
		var address = restore ? this.address : 0x00;

		// Loop over characters of the input string, changing `state` after each; stop when the automaton reaches
		// a final state (or, naturally, when no character left in the input).
		while (input.hasRemaining() && state >= 0) {
			var ch = input.get();

			// Compute transition function: given a character of the alphabet, map current state to a new one. After
			// that, the character is considered consumed and adopted. Please note that the order of case labels
			// is important here, since we essentially rely on falling-through.
			state = switch (state & ~ST_OCTET_COUNT_MASK) {
				case ST_OPEN_OCTET:
					if (ch == '.') {
						address <<= BITS_PER_OCTET;
						yield state + ST_OPEN_OCTET_FLAG;   // Remove open octet flag, increase octet counter
					}

				case ST_CLOSED_OCTET:
					if ('0' <= ch && ch <= '9') {
						var octet = address & OCTET_MASK;
						address ^= (octet ^= octet * 10 + (ch - '0'));
						yield (octet & ~OCTET_MASK) != 0
							? ST_ERR_INVALID_OCTET
							: state | ST_OPEN_OCTET_FLAG;   // Set open octet flag, keep octet counter untouched
					}

				default:
					if (ch == '\n' || ch == '\r')
						yield state | ST_FINAL_FLAG;

					// If we've fallen-through down to here, it means the automaton was failed to recognize the input.
					yield (state & ST_ERROR_MASK) == 0
						? ST_ERR_UNEXPECTED_CHARACTER
						: state;                            // Fast-forward to very end of the line
			};
		}

		// Store the reached state for a future use.
		this.state = state;
		this.address = address;

		// Return `true` if the state is final. This means that another line of the input is parsed completely,
		// and a user may inspect the result with `classify()` and other methods.
		return state < 0;
	}

	public LineToken classify() {
		if (state >= 0)
			return null;

		return switch (state) {
			case ST_FIN_ADDRESS    -> LineToken.ADDRESS;
			case ST_FIN_EMPTY_LINE -> LineToken.EMPTY_LINE;
			default                -> LineToken.NONSENSE;
		};
	}

	public int getAddress() {
		return address;
	}

	public String getErrorMessage() {
		if ((state & ~ST_FINAL_FLAG) == ST_ERR_UNEXPECTED_CHARACTER)
			return "Unexpected character";

		if ((state & ~ST_FINAL_FLAG) == ST_ERR_INVALID_OCTET)
			return "Invalid octet value";

		if (ST_FIN_EMPTY_LINE < state && state < ST_FIN_ADDRESS)
			return "Malformed address (too short)";

		return null;
	}
}
