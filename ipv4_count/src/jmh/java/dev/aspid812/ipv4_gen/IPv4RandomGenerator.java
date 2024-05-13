package dev.aspid812.ipv4_gen;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;


public class IPv4RandomGenerator {

	private static final int OCTET_BITSIZE = 8;
	private static final int OCTET_MASK = ~(-1 << OCTET_BITSIZE);

	static String renderIPv4(int address) {
		return String.format("%d.%d.%d.%d\n",
			OCTET_MASK & (address >> OCTET_BITSIZE * 3),
			OCTET_MASK & (address >> OCTET_BITSIZE * 2),
			OCTET_MASK & (address >> OCTET_BITSIZE),
			OCTET_MASK & (address));
	}

	static List<byte[]> newRandomPool(Random rng, int poolSize) {
		//TODO: Should we take care of uniqueness of items in the pool?
		return rng.ints(poolSize)
			.mapToObj(IPv4RandomGenerator::renderIPv4)
			.map(String::getBytes)
			.toList();
	}

	public static IPv4RandomGenerator create(int poolSize) {
		var rng = new Random();
		var pool = newRandomPool(rng, poolSize);
		return new IPv4RandomGenerator(rng, pool);
	}

	final Random rng;
	final List<byte[]> pool;

	IPv4RandomGenerator(Random rng, List<byte[]> pool) {
		this.rng = rng;
		this.pool = pool;
	}

	public void sample(long size, OutputStream output) throws IOException {
		for (var remain = size; remain > 0; remain--) {
			var index = rng.nextInt(pool.size());
			output.write(pool.get(index));
		}
		output.flush();
	}

//	public void sample(ByteBuffer output) {
//		var index = rng.nextInt(pool.size());
//		var item = pool.get(index);
//		if (output.remaining() < item.length)
//			return;
//
//		output.put(item);
//	}
}
