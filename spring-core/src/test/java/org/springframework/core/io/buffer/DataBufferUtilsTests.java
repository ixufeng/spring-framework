/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io.buffer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.stream.Collectors;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class DataBufferUtilsTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void readReadableByteChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI();
		FileChannel channel = FileChannel.open(Paths.get(uri), StandardOpenOption.READ);
		Flux<DataBuffer> flux = DataBufferUtils.read(channel, this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		assertFalse(channel.isOpen());
	}

	@Test
	public void readAsynchronousFileChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI();
		AsynchronousFileChannel
				channel = AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ);
		Flux<DataBuffer> flux = DataBufferUtils.read(channel, this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void readAsynchronousFileChannelPosition() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI();
		AsynchronousFileChannel
				channel = AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ);
		Flux<DataBuffer> flux = DataBufferUtils.read(channel, 3, this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void readUnalignedChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI();
		FileChannel channel = FileChannel.open(Paths.get(uri), StandardOpenOption.READ);
		Flux<DataBuffer> flux = DataBufferUtils.read(channel, this.bufferFactory, 5);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("fooba"))
				.consumeNextWith(stringConsumer("rbazq"))
				.consumeNextWith(stringConsumer("ux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		assertFalse(channel.isOpen());
	}

	@Test
	public void readInputStream() throws Exception {
		InputStream is = DataBufferUtilsTests.class.getResourceAsStream("DataBufferUtilsTests.txt");
		Flux<DataBuffer> flux = DataBufferUtils.read(is, this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void writeOutputStream() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		DataBuffer qux = stringBuffer("qux");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz, qux);

		Path tempFile = Files.createTempFile("DataBufferUtilsTests", null);
		OutputStream os = Files.newOutputStream(tempFile);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, os);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		String result = Files.readAllLines(tempFile)
				.stream()
				.collect(Collectors.joining());

		assertEquals("foobarbazqux", result);
		os.close();
	}

	@Test
	public void writeWritableByteChannel() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		DataBuffer qux = stringBuffer("qux");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz, qux);

		Path tempFile = Files.createTempFile("DataBufferUtilsTests", null);
		WritableByteChannel channel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		String result = Files.readAllLines(tempFile)
				.stream()
				.collect(Collectors.joining());

		assertEquals("foobarbazqux", result);
		channel.close();
	}

	@Test
	public void writeAsynchronousFileChannel() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		DataBuffer qux = stringBuffer("qux");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz, qux);

		Path tempFile = Files.createTempFile("DataBufferUtilsTests", null);
		AsynchronousFileChannel channel =
				AsynchronousFileChannel.open(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel, 0);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		String result = Files.readAllLines(tempFile)
				.stream()
				.collect(Collectors.joining());

		assertEquals("foobarbazqux", result);
		channel.close();
	}

	@Test
	public void takeUntilByteCount() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(flux, 5L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("ba"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		release(baz);
	}

	@Test
	public void skipUntilByteCount() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(flux, 5L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("r"))
				.consumeNextWith(stringConsumer("baz"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void skipUntilByteCountShouldSkipAll() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(flux, 9L);

		StepVerifier.create(result)
				.expectNextCount(0)
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void releaseConsumer() {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);

		flux.subscribe(DataBufferUtils.releaseConsumer());

		// AbstractDataBufferAllocatingTestCase.LeakDetector will assert the release of the buffers
	}

	public void foo() {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataBufferUtils.write(flux, bos)
				.subscribe(DataBufferUtils.releaseConsumer());


	}

}
