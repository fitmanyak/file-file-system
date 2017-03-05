package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFile;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class File implements IFile {
    @Override
    public void close() throws FileFileSystemException {
    }

    @Override
    public int read(ByteBuffer buffer) throws FileFileSystemException {
        return 0;
    }

    @Override
    public int write(ByteBuffer buffer) throws FileFileSystemException {
        return 0;
    }
}
