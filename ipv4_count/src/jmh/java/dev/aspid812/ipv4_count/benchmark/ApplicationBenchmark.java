package dev.aspid812.ipv4_count.benchmark;

import dev.aspid812.ipv4_count.IPv4CountApp;
import dev.aspid812.ipv4_gen.IPv4RandomGeneratorApp;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@Fork(1)
public class ApplicationBenchmark {

	static ProcessBuilder execJavaClass(Class<?> mainClass, String... options) {
		var classPath = System.getProperty("java.class.path");
		var command = new ArrayList<String>();
		command.addAll(List.of("java", "--class-path", classPath, mainClass.getCanonicalName()));
		command.addAll(List.of(options));
		return new ProcessBuilder(command)
			.redirectError(ProcessBuilder.Redirect.INHERIT);
	}

	static void generateInputFile(Path filePath, String lines) throws IOException {
		var generator = IPv4RandomGeneratorApp.forCommandLine("-n" + lines);
		try (var output = Files.newOutputStream(filePath)) {
			generator.run(output, System.err);
			System.err.println("Created file: " + filePath);
		}
	}

	@State(Scope.Thread)
	public static class Context {
		@Param({"1K", "1M", "16M"})
		public String lines;

		public Path filePath;

		@Setup(Level.Trial)
		public void setup() throws IOException {
			var tempDir = System.getProperty("java.io.tmpdir");
			filePath = Path.of(tempDir, "RandomIPs-" + lines + ".txt");
			if (!Files.exists(filePath)) {
				generateInputFile(filePath, lines);
			}
			else {
				System.err.println("Already present: " + filePath);
			}
		}
	}

	@Benchmark
	@BenchmarkMode(Mode.SingleShotTime)
	public void generateOnly(Context ctx) throws IOException, InterruptedException {
		var process = execJavaClass(IPv4RandomGeneratorApp.class, "-n" + ctx.lines)
			.redirectOutput(ProcessBuilder.Redirect.DISCARD)
			.start();
		process.waitFor();
	}

	@Benchmark
	@BenchmarkMode(Mode.SingleShotTime)
	public void pipeline(Context ctx) throws IOException, InterruptedException {
		var processes = ProcessBuilder.startPipeline(List.of(
			execJavaClass(IPv4RandomGeneratorApp.class, "-n" + ctx.lines),
			execJavaClass(IPv4CountApp.class)
				.redirectOutput(ProcessBuilder.Redirect.INHERIT)
		));
		processes.get(1).waitFor();
	}

	@Benchmark
	@BenchmarkMode(Mode.SingleShotTime)
	public void redirect(Context ctx) throws IOException, InterruptedException {
		var process = execJavaClass(IPv4CountApp.class)
			.redirectInput(ctx.filePath.toFile())
			.redirectOutput(ProcessBuilder.Redirect.INHERIT)
			.start();
		process.waitFor();
	}
}
