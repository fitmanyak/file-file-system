package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IRootDirectory;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class RootDirectory extends BaseDirectory<IInternalRootDirectory, IRootDirectoryDirectoryEntry>
        implements IInternalRootDirectory {

    private RootDirectory(IRootDirectoryDirectoryEntry entry) {
        super(entry, null);
    }

    @Override
    public void setName(String newName) throws FileFileSystemException {
        throw new UnsupportedOperationException("Can't change root directory name");// TODO
    }

    @Override
    public void remove() throws FileFileSystemException {
        throw new UnsupportedOperationException("Can't remove root directory");// TODO
    }

    public static void format(ByteBuffer block) {
        RootDirectoryDirectoryEntry.format(block);
    }

    public static IRootDirectory open(IBlockManager blockManager) throws FileFileSystemException {
        return new RootDirectory(RootDirectoryDirectoryEntry.open(blockManager));
    }
}
