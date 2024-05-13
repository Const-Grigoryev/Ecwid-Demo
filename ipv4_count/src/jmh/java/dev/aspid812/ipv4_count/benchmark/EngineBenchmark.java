package dev.aspid812.ipv4_count.benchmark;

import dev.aspid812.ipv4_count.impl.IPv4Parser;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

@Fork(1)
public class EngineBenchmark {

	@State(Scope.Thread)
	public static class Context {
		Reader source;
		IPv4Parser parser;

		@Setup(Level.Iteration)
		public void setup() {
			source = new StringReader("3.14.159.26");
			parser = new IPv4Parser(source);
		}

		@TearDown(Level.Iteration)
		public void teardown() throws IOException {
			source.close();
		}
	}

//	@Benchmark
//	@Warmup(iterations = 1)
//	@Measurement(iterations = 15, batchSize = 1000)
//	public void dryRun(Context ctx, Blackhole sink) throws IOException {
//		ctx.source.mark(12);
//		while (true) {
//			var ch = ctx.source.read();
//			if (ch == -1)
//				break;
//		}
//		sink.consume(ctx.source);
//		ctx.source.reset();
//	}

//	@Benchmark
//	@Warmup(iterations = 1)
//	@Measurement(iterations = 15, batchSize = 1000)
//	public final void parsing(Context ctx, Blackhole sink) throws IOException {
//		ctx.source.mark(12);
//		var token = ctx.parser.parseNextLine(sink::consume);
//		if (token != IPv4Parser.ParseResult.ADDRESS)
//			throw new RuntimeException("Something went wrong...");
//		ctx.source.reset();
//	}
}
