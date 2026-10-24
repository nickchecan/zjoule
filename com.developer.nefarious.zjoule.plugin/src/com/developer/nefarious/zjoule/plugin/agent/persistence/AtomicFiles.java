package com.developer.nefarious.zjoule.plugin.agent.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes a file by replacing it from a completed temporary file.
 */
final class AtomicFiles {

    private AtomicFiles() {
    }

    /**
     * Writes UTF-8 text so readers never observe a partial file.
     * <p>
     * Some file systems refuse an atomic replace. The temporary file is already
     * complete, so a plain replace is still safe for readers that ignore
     * {@code *.tmp}.
     *
     * @param target the destination file.
     * @param contents the full file contents.
     * @throws IOException when the write or replace fails.
     */
    static void write(final Path target, final String contents) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName().toString() + ".tmp");
        Files.writeString(temporary, contents, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
