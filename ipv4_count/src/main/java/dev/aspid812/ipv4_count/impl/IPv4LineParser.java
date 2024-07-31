package dev.aspid812.ipv4_count.impl;

import javax.sound.sampled.Line;
import java.nio.CharBuffer;
import java.util.function.IntSupplier;

import static dev.aspid812.ipv4_count.impl.IPv4Address.OCTETS_PER_ADDRESS;
import static dev.aspid812.ipv4_count.impl.IPv4Address.BITS_PER_OCTET;
import static dev.aspid812.ipv4_count.impl.IPv4Address.OCTET_MASK;


//TODO: Fix up comments
public final class IPv4LineParser {

	public enum LineToken {
		VALID_ADDRESS,
		IRRELEVANT_CONTENT,
		NOTHING
	}

	private static boolean delimiter(int ch) {
		return ch == '\n';
	}

	// Automaton's state summarizes a consumed portion (`p`) of the input string.
	private enum State {
		OPEN_OCTET,     // `p` contains some octets, and the last one isn't complete yet; thus, `p` ends with a digit;
		CLOSED_OCTET,   // `p` is empty, or contains some octets, each of which is _closed_ by a following dot;
		NONSENSE        // `p` may not be a prefix of any recognizable substance (valid IP address).
	}

	private int address;
	private String error;

	private int octets;
	private State state;

	public boolean ready() {
		return state == null;
	}

	public int getAddress() {
		return address;
	}

	public String getErrorMessage() {
		return error;
	}

	public int parseLine(CharBuffer input) {
		var dealer = (IntSupplier) () -> input.hasRemaining() ? input.get() : -1;
		parseLine(dealer);
		return input.position();
	}

	void parseLine(IntSupplier input) {
		var restore = state != null;

		// Output registers: construction site of a parsing product.
		var address = restore ? this.address : 0x00;
		var error = restore ? this.error : null;

		// Loop over characters of the input string (single line), changing `state` after each. The variable named
		// `octets` counts closed octets and, strictly speaking, this number is a part of automaton's state too.
		var octets = restore ? this.octets : 0;
		var state = restore ? this.state : State.CLOSED_OCTET;

		var eol = false;
		while (true) {
			// **Assertion 1 (loop invariant):** regardless of the state, `0 <= octets && octets < OCTETS_PER_ADDRESS`
			// both before and after the loop's body.

			// **Assertion 2:** one and only one character is read at each iteration. This is correct because we
			// do not receive a character from the reader anywhere else.
			var ch = input.getAsInt();

			// **Assertion 3:** the loop breaks when and only when it encounters an EOL or EOF character. Therefore,
			// a single parser invocation consumes exactly one line from the input.
			if (ch == -1 || (eol = delimiter(ch)))
				break;

			// **Assertion 4:** by now, we have already guaranteed that `ch` is a regular character, which belongs
			// to the automaton's alphabet, not a special character like EOL or EOF.

			// Compute transition function: given a character of the alphabet, map current state to a new one. After
			// that, the character is considered consumed and adopted. Please note that the order of case labels
			// is important here, since we essentially rely on falling-through.
			state = switch (state) {
				case OPEN_OCTET:
					if (ch == '.' && ++octets < OCTETS_PER_ADDRESS) {
						address <<= BITS_PER_OCTET;
						yield State.CLOSED_OCTET;
					}

				case CLOSED_OCTET:
					if ('0' <= ch && ch <= '9') {
						var octet = address & OCTET_MASK;
						address ^= (octet ^= octet * 10 + (ch - '0'));
						if ((octet & ~OCTET_MASK) != 0) {
							error = "Invalid octet value";
							yield State.NONSENSE;
						}
						yield State.OPEN_OCTET;
					}

				default:
					// If we've fallen-through down to here, it means the automaton was failed to recognize the input.
					error = "Unexpected character";

				case NONSENSE:
					yield State.NONSENSE;
			};
		}

		// Compute classifying function: is the state our automaton has reached accepting or non-accepting?
		// It can be thought of as a peculiar special case of a transition function for the EOL character. Thus,
		// fall-through matters here as before (watch you step!). Finally, depending on the classification result,
		// send a parsing product to a corresponding visitor/builder function.
		if (eol) {
			var accepting = switch (state) {
				case OPEN_OCTET   -> ++octets == OCTETS_PER_ADDRESS;  // Valid address
				case CLOSED_OCTET -> octets == 0;                     // Empty line
				case NONSENSE     -> error != null;                   // Bad line, but it's OK to "accept" it whenever the error is already checked
			};
			if (!accepting) {
				error = "Malformed address (too short)";
			}
			state = null;
		}

		// Store the final state for a future use
		this.address = address;
		this.error = error;
		this.octets = octets;
		this.state = state;
	}

	public LineToken classify() {
		if (state == null) {
			if (error != null)
				return LineToken.IRRELEVANT_CONTENT;
			if (octets == OCTETS_PER_ADDRESS)
				return LineToken.VALID_ADDRESS;
			if (octets == 0)
				return LineToken.NOTHING;
		}

		throw new IllegalStateException("state = " + state + ",  octets = " + octets);
	}
}
