package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFile;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class File implements IFile {
    private final DirectoryEntry entry;

    public File(DirectoryEntry entry) {
        this.entry = entry;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        return 0;
    }

    @Override
    public int write(ByteBuffer buffer) throws IOException {
        return 0;
    }
}
