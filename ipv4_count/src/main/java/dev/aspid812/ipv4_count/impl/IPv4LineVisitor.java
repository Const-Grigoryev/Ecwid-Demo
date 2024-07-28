package dev.aspid812.ipv4_count.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.function.IntSupplier;

import static dev.aspid812.ipv4_count.impl.IPv4Address.OCTETS_PER_ADDRESS;
import static dev.aspid812.ipv4_count.impl.IPv4Address.BITS_PER_OCTET;
import static dev.aspid812.ipv4_count.impl.IPv4Address.OCTET_MASK;

import dev.aspid812.ipv4_count.impl.IPv4LineVisitor.Parser.State;


public interface IPv4LineVisitor<R> {

	R address(int address);
	R mistake(String message);
	R nothing();

	private static boolean delimiter(int ch) {
		return ch == '\n';
	}

	final class Parser {
		// Automaton's state summarizes a consumed portion (`p`) of the input string.
		enum State {
			OPEN_OCTET,     // `p` contains some octets, and the last one isn't complete yet; thus, `p` ends with a digit;
			CLOSED_OCTET,   // `p` is empty, or contains some octets, each of which is _closed_ by a following dot;
			NONSENSE        // `p` may not be a prefix of any recognizable substance (valid IP address).
		}

		private int address = 0x00;
		private String error = null;

		private int octets = 0;
		private State state = State.CLOSED_OCTET;
	}

	default R parseLine(IntSupplier input, Parser parser) {
		// Output registers: construction site of a parsing product.
		var address = parser.address;
		var error = parser.error;

		// Loop over characters of the input string (single line), changing `state` after each. The variable named
		// `octets` counts closed octets and, strictly speaking, this number is a part of automaton's state too.
		var octets = parser.octets;
		var state = parser.state;
		while (true) {
			// **Assertion 1 (loop invariant):** regardless of the state, `0 <= octets && octets < OCTETS_PER_ADDRESS`
			// both before and after the loop's body.

			// **Assertion 2:** one and only one character is read at each iteration. This is correct because we
			// do not receive a character from the reader anywhere else.
			var ch = input.getAsInt();

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

		// Store the final state for a future use
		parser.address = address;
		parser.error = error;
		parser.octets = octets;
		parser.state = state;

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

	default R parseLine(Reader input) throws IOException {
		var dealer = (IntSupplier) () -> {
			try {
				return input.read();
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		};

		try {
			return parseLine(dealer, new Parser());
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}
}
