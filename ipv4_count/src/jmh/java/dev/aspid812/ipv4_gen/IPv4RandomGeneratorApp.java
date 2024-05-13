package dev.aspid812.ipv4_gen;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class IPv4RandomGeneratorApp {

	public static final int EXIT_OK = 0;
	public static final int EXIT_FATAL = -1;

	public static Pattern POOL_SIZE_OPTION = Pattern.compile("-p(?<value>\\d+)");
	public static Pattern SAMPLE_SIZE_OPTION = Pattern.compile("-n(?<value>\\d+)(?<mult>[KMB]?)");

	public static int POOL_SIZE_DEFAULT = 64_000;
	public static long SAMPLE_SIZE_DEFAULT = Long.MAX_VALUE;    // Effectively endless

	private static long multiplier(String abbr) {
		return switch (abbr) {
			case "" -> 1L;
			case "K" -> 1_000L;
			case "M" -> 1_000_000L;
			case "B" -> 1_000_000_000L;
			default -> throw new IllegalArgumentException("Unrecognized option");
		};
	}

	private static Stream<Matcher> filterOptions(String[] options, Pattern pattern) {
		return Arrays.stream(options)
			.map(pattern::matcher)
			.filter(Matcher::matches);
	}

	public static IPv4RandomGeneratorApp forCommandLine(String... options) {
		var poolSize = filterOptions(options, POOL_SIZE_OPTION)
			.mapToInt(m -> Integer.parseInt(m.group("value")))
			.findAny()
			.orElse(POOL_SIZE_DEFAULT);
		var sampleSize = filterOptions(options, SAMPLE_SIZE_OPTION)
			.mapToLong(m -> Long.parseLong(m.group("value")) * multiplier(m.group("mult")))
			.findAny()
			.orElse(SAMPLE_SIZE_DEFAULT);
		return new IPv4RandomGeneratorApp(poolSize, sampleSize);
	}

	final int poolSize;
	final long sampleSize;

	public IPv4RandomGeneratorApp(int poolSize, long sampleSize) {
		this.poolSize = poolSize;
		this.sampleSize = sampleSize;
	}

	public int run(OutputStream output, PrintStream logger) {
		try {
			var engine = IPv4RandomGenerator.create(poolSize);
			engine.sample(sampleSize, output);
		}
		catch (IOException ex) {
			ex.printStackTrace(logger);
			return EXIT_FATAL;
		}

		return EXIT_OK;
	}

	public static void main(String[] args) {
		var application = IPv4RandomGeneratorApp.forCommandLine(args);
		var status = application.run(System.out, System.err);
		if (status != EXIT_OK) {
			System.exit(status);
		}
	}
}
