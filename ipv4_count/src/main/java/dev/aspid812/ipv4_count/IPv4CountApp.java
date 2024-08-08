package dev.aspid812.ipv4_count;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import dev.aspid812.common.Application;
import dev.aspid812.ipv4_count.IPv4Count.ErrorHandler;


public class IPv4CountApp implements Application {

	public static final int EXIT_STARTUP = -2;

	enum ErrorHandlers {;
		public static ErrorHandler reportAndProceed(PrintStream logger) {
			return error -> {
				logger.println(error);
			};
		}
	}

	public static Application forCommandLine(String... arguments) {
		var inputFiles = Arrays.stream(arguments)
			.flatMap(arg -> {
				try {
					var file = Path.of(arg);
					return Stream.of(file);
				}
				catch (InvalidPathException ex) {
					return Stream.empty();
				}
			})
			.filter(Files::isReadable)
			.toList();

		if (inputFiles.size() < arguments.length) {
			return Application.echo(EXIT_STARTUP, """
				Error: an argument cannot be interpreted as a valid file name, \
				or such file does not exist, or it has a read restriction."""
			);
		}

		var includeStandardInput = inputFiles.isEmpty();
		return new IPv4CountApp(inputFiles, includeStandardInput);
	}

	final List<Path> inputFiles;
	final boolean includeStandardInput;     //FIXME: `Boolean` 🤮

	public IPv4CountApp(List<Path> inputFiles, boolean includeStandardInput) {
		this.inputFiles = Objects.requireNonNull(inputFiles);
		this.includeStandardInput = includeStandardInput;
	}

	@Override
	public int run(InputStream input, PrintStream output, PrintStream logger) {
		var errorHandler = ErrorHandlers.reportAndProceed(logger);
		var counter = new IPv4Count(errorHandler);
		try {
			for (var inputFile : inputFiles) {
				counter.account(inputFile);
			}
			if (includeStandardInput) {
				// Intentionally without `try`: closing this channel would cause (an undesirable) closing of STDIN
				var standardInputChannel = Channels.newChannel(input);
				counter.account(standardInputChannel);
			}
		}
		catch (IOException ex) {
			ex.printStackTrace(logger);
			return EXIT_FATAL;
		}

		counter.uniqueAddresses().ifPresent(count -> {
			output.println(count);
		});
		return EXIT_OK;
	}

	public static void main(String[] args) {
		var application = IPv4CountApp.forCommandLine(args);
		var status = application.run(System.in, System.out, System.err);
		if (status != EXIT_OK) {
			System.exit(status);
		}
	}
}
