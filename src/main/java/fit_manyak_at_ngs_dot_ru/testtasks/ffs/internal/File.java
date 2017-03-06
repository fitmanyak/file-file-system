package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFile;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class File implements IFile {
    @Override
    public String getName() {
        return null;// TODO
    }

    @Override
    public void setName(String newName) throws FileFileSystemException {
        // TODO
    }

    @Override
    public void remove() throws FileFileSystemException {
        // TODO
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isEmpty() throws FileFileSystemException {
        return false;// TODO
    }

    @Override
    public void close() throws FileFileSystemException {
        // TODO
    }

    @Override
    public IDirectory getParentDirectory() {
        return null;// TODO
    }

    @Override
    public long getSize() throws FileFileSystemException {
        return 0;// TODO
    }

    @Override
    public void setSize(long newSize) throws FileFileSystemException {
        // TODO
    }

    @Override
    public long getPosition() throws FileFileSystemException {
        return 0;// TODO
    }

    @Override
    public void setPosition(long newPosition) throws FileFileSystemException {
        // TODO
    }

    @Override
    public void reset() throws FileFileSystemException {
        // TODO
    }

    @Override
    public int read(ByteBuffer destination) throws FileFileSystemException {
        return 0;// TODO
    }

    @Override
    public int read(long newPosition, ByteBuffer destination) throws FileFileSystemException {
        return 0;// TODO
    }

    @Override
    public int write(ByteBuffer source) throws FileFileSystemException {
        return 0;// TODO
    }

    @Override
    public int write(long newPosition, ByteBuffer source) throws FileFileSystemException {
        return 0;// TODO
    }
}
