package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.io.Reader;

import static dev.aspid812.ipv4_count.impl.IPv4Address.OCTETS_PER_ADDRESS;
import static dev.aspid812.ipv4_count.impl.IPv4Address.BITS_PER_OCTET;
import static dev.aspid812.ipv4_count.impl.IPv4Address.OCTET_MASK;


public interface IPv4LineVisitor<R> {

	R address(int address);
	R mistake(String message);
	R nothing();

	private static boolean delimiter(int ch) {
		return ch == '\n';
	}

	default R parseLine(Reader input) throws IOException {
		// Automaton's state summarizes a consumed portion (`p`) of the input string.
		enum State {
			OPEN_OCTET,     // `p` contains some octets, and the last one isn't complete yet; thus, `p` ends with a digit;
			CLOSED_OCTET,   // `p` is empty, or contains some octets, each of which is _closed_ by a following dot;
			NONSENSE        // `p` may not be a prefix of any recognizable substance (valid IP address).
		}

		// Output registers: construction site of a parsing product.
		var address = 0x00;
		var error = (String) null;

		// Loop over characters of the input string (single line), changing `state` after each. The variable named
		// `octets` counts closed octets and, strictly speaking, this number is a part of automaton's state too.
		var octets = 0;
		var state = State.CLOSED_OCTET;
		while (true) {
			// **Assertion 1 (loop invariant):** regardless of the state, `0 <= octets && octets < OCTETS_PER_ADDRESS`
			// both before and after the loop's body.

			// **Assertion 2:** one and only one character is read at each iteration. This is correct because we
			// do not receive a character from the reader anywhere else.
			var ch = input.read();

			// **Assertion 3:** the loop breaks when and only when it encounters an EOL or EOF character. Therefore,
			// a single parser invocation consumes exactly one line from the input.
			if (ch == -1 || delimiter(ch))
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
		return switch (state) {
			case OPEN_OCTET:
				if (++octets == OCTETS_PER_ADDRESS)
					yield address(address);

			case CLOSED_OCTET:
				if (octets == 0)
					yield nothing();

			default:
				error = "Malformed address (too short)";

			case NONSENSE:
				yield mistake(error);
		};
	}
}
