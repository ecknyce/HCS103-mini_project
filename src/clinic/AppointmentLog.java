package clinic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** AutoCloseable resource: try-with-resources guarantees one close attempt. */
public final class AppointmentLog implements AutoCloseable {
    private final BufferedWriter writer;
    private boolean closed;

    public AppointmentLog(Path path) throws IOException {
        writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        System.out.println("[log] opened");
        write("LOG OPENED");
    }

    public synchronized void write(String message) {
        if (closed) throw new IllegalStateException("Log is already closed.");
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write appointment log: " + e.getMessage(), e);
        }
    }

    @Override public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            try {
                writer.write("LOG CLOSED");
                writer.newLine();
                writer.close();
            } finally {
                System.out.println("[log] closed");
            }
        }
    }
}
