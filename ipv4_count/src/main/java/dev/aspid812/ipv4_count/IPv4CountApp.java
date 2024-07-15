package dev.aspid812.ipv4_count;

import java.io.*;


public class IPv4CountApp {

	public static final int EXIT_OK = 0;
	public static final int EXIT_FATAL = -1;

	final IPv4Count engine = new IPv4Count();

	static class ErrorHandlers {
		public static IPv4Count.ErrorHandler reportAndProceed(PrintStream logger) {
			return error -> {
				logger.println(error);
				return IPv4Count.ControlFlag.PROCEED;
			};
		}
	}

	public int run(InputStream input, PrintStream output, PrintStream logger) {
		try {
			var count = engine.countUnique(input, ErrorHandlers.reportAndProceed(logger));
			output.println(count);
		}
		catch (IOException ex) {
			ex.printStackTrace(logger);
			return EXIT_FATAL;
		}

		return EXIT_OK;
	}

	public static void main(String[] args) {
		var application = new IPv4CountApp();
		var status = application.run(System.in, System.out, System.err);
		if (status != EXIT_OK) {
			System.exit(status);
		}
	}
}
