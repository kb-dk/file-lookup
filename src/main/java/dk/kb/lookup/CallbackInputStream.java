/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.lookup;

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.util.Json;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Buffered InputStream that uses a callback for filling the buffer when needed.
 */
public class CallbackInputStream extends InputStream {
    private static Log log = LogFactory.getLog(CallbackInputStream.class);

    private final Consumer<ByteArrayOutputStream> producer;
    private final ByteArrayOutputStream callbackBuffer = new ByteArrayOutputStream();
    private final Consumer<Boolean> closeCallback;
    private ByteArrayInputStream outputBuffer;
    private boolean empty = false;

    /**
     * The contentProducer should add content to the provided ByteArrayOutputStream when {@code Consumer#accept} is
     * called. If no content is added, the contentProducer is considered depleted.
     * @param contentProducer for delivering content to the stream.
     */
    public CallbackInputStream(Consumer<ByteArrayOutputStream> contentProducer) {
        this(contentProducer, depleted -> {}); // No special action on depletion/close
    }

    /**
     * The contentProducer should add content to the provided ByteArrayOutputStream when {@code Consumer#accept} is
     * called. If no content is added, the contentProducer is considered depleted.
     * @param contentProducer for delivering content to the stream.
     * @param closeCallback called when the stream is closed. If called with true, the content was depleted.
     *                      If called with false, {@link #close()} was explicitly called.
     */
    public CallbackInputStream(Consumer<ByteArrayOutputStream> contentProducer, Consumer<Boolean> closeCallback) {
        this.producer = contentProducer;
        this.closeCallback = closeCallback;
    }

    /**
     * The contentProducer should add content to the provided ByteArrayOutputStream when {@code Consumer#accept} is
     * called. If no content is added, the contentProducer is considered depleted.
     * @param contentProducer for delivering content to the stream.
     * @param closeCallback called when the stream is closed. If called with true, the content was depleted.
     *                      If called with false, {@link #close()} was explicitly called.
     */
    public CallbackInputStream(Callable<String> contentProducer, Consumer<Boolean> closeCallback) {
        this.producer = makeUTF8Producer(contentProducer);
        this.closeCallback = closeCallback;
    }

    /**
     * Convenience wrapper for using the {@link CallbackInputStream} with code that produces Strings.
     * @param producer a String producer.
     * @return the String producer transmogrified to be a contentProducer for {@link CallbackInputStream}.
     */
    public static Consumer<ByteArrayOutputStream> makeUTF8Producer(Callable<String> producer) {
        return sink -> {
            try {
                sink.write(producer.call().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException("Exception calling producer", e);
            }
        };
    }

    /**
     * Takes a Jackson-annotated JSON Object (likely defined by Swagger and stored in the {@code model} folder)
     * and converts it to a single-line UTF-8 String representation.
     * @param producer a producer of Jackson-annotated JSON Objects. Returning null signals no more data.
     * @return the JSON producer wrapped to deliver bytes.
     */
    public static Consumer<ByteArrayOutputStream> makeJSONProducer(Callable<Object> producer) {
        final ObjectWriter jsonWriter = Json.mapper().writer(new MinimalPrettyPrinter());
        final AtomicBoolean first = new AtomicBoolean(true);
        final AtomicBoolean depleted = new AtomicBoolean(false);
        return sink -> {
            if (depleted.get()) {
                return;
            }
            try {
                String result = first.get() ? "{" : "";
                Object jsonObject = producer.call();
                if (jsonObject == null) {
                    result += "}";
                } else {
                    if (!first.get()) {
                        result += ",\n";
                    }
                    result += jsonWriter.writeValueAsString(jsonObject);
                }
                first.set(false);
                sink.write(result.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("Exception in makeJSONProducer. Ending stream", e);
            }
        };
    }

    @Override
    public int read() throws IOException {
        if (!ensureContent()) {
            return -1;
        }
        return outputBuffer.read(); // We know there is at least 1 byte available
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!ensureContent()) {
            return -1;
        }
        return outputBuffer.read(b, off, len); // We know there is at least 1 byte available
    }

    @Override
    public int available() throws IOException {
        return ensureContent() ? 1 : 0;
    }

    /**
     * If the {@link #outputBuffer} is empty, new content is requested from the {@link #producer}.
     * If the producer runs dry, the CallbackInputStream is considered depleted.
     * @return ture if some content is available, else false.
     */
    private boolean ensureContent() {
        if (empty) {
            return false;
        }
        if (outputBuffer != null && outputBuffer.available() > 0) {
            return true;
        }

        // Get new content
        callbackBuffer.reset();
        producer.accept(callbackBuffer);
        if (callbackBuffer.size() == 0) {
            // No new content: Signal stop
            empty = true;
            outputBuffer = null; // Just to tidy up
            closeCallback.accept(true);
            return false;
        }

        // Ready the new content for delivery
        outputBuffer = new ByteArrayInputStream(callbackBuffer.toByteArray());
        return true;
    }

    @Override
    public void close() {
        empty = true;
        closeCallback.accept(false);
    }
}
