package dev.aspid812.ipv4_gen

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel


interface Sample {
	val size: Long
	val exhausted: Boolean

	fun draw(output: ByteBuffer): Long
}


@Throws(IOException::class)
fun Sample.draw(output: WritableByteChannel, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
	val buffer = ByteBuffer.allocate(bufferSize)
	while (!this.exhausted) {
		this.draw(buffer)
		buffer.flip()
		if (buffer.limit() == 0)
			throw IOException("Buffer's too small to fit in a sample")

		do {
			val written = output.write(buffer)
			if (written == 0)
				throw IOException("Channel took zero bytes")
		} while (buffer.hasRemaining())
		buffer.clear()
	}
}


@Throws(IOException::class)
fun Sample.draw(output: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
	val channel = Channels.newChannel(output)
	this.draw(channel, bufferSize)      // Intentionally don't close the channel
	output.flush()
}
