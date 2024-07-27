package dev.aspid812.ipv4_count;

import java.io.*;

import dev.aspid812.ipv4_count.IPv4Count.ControlFlag;
import dev.aspid812.ipv4_count.IPv4Count.ErrorHandler;


public class IPv4CountApp {

	public static final int EXIT_OK = 0;
	public static final int EXIT_FATAL = -1;

	enum ErrorHandlers {;
		public static ErrorHandler reportAndProceed(PrintStream logger) {
			return error -> {
				logger.println(error);
				return ControlFlag.PROCEED;
			};
		}
	}

	public int run(InputStream input, PrintStream output, PrintStream logger) {
		var errorHandler = ErrorHandlers.reportAndProceed(logger);
		var counter = new IPv4Count(errorHandler);
		try {
			counter.account(input);
		}
		catch (IOException ex) {
			ex.printStackTrace(logger);
			return EXIT_FATAL;
		}

		counter.uniqueAddresses().ifPresent((count) -> {
			output.println(count);
		});
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
