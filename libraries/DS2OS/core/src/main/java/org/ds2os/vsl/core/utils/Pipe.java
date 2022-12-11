package org.ds2os.vsl.core.utils;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * A unidirectional Pipe connecting two streams with each other.
 */
public class Pipe {
    /**
     * The {@link PipedInputStream} source, where data can be read from the pipe.
     */
    private final PipedInputStream source;

    /**
     * The {@link PipedOutputStream} sink, where data can be written into the pipe.
     */
    private final PipedOutputStream sink;

    public Pipe() throws IOException {
        source = new PipedInputStream();
        sink = new PipedOutputStream(source);
    }

    /**
     * Getter for the source.
     * @return
     *      the {@link PipedInputStream} source.
     */
    public PipedInputStream getSource() {
        return source;
    }

    /**
     * Getter for the sink.
     * @return
     *      the {@link PipedOutputStream} sink.
     */
    public PipedOutputStream getSink() {
        return sink;
    }
}
