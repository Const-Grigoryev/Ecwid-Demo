package dev.aspid812.common;

import java.io.InputStream;
import java.io.PrintStream;


@FunctionalInterface
public interface Application {

	int EXIT_OK = 0;
	int EXIT_FATAL = -1;

	int run(InputStream input, PrintStream output, PrintStream logger);

	static Application echo(int status, String message) {
		return (input, output, logger) -> {
			output.println(message);
			return status;
		};
	}
}
