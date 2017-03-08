package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IRootDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;

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
        throw new UnsupportedOperationException(Messages.CANT_RENAME_ROOT_DIRECTORY_ERROR);
    }

    @Override
    public void remove() throws FileFileSystemException {
        throw new UnsupportedOperationException(Messages.CANT_REMOVE_ROOT_DIRECTORY_ERROR);
    }

    public static void format(ByteBuffer block) {
        RootDirectoryDirectoryEntry.format(block);
    }

    public static IRootDirectory open(IBlockManager blockManager) throws FileFileSystemException {
        return new RootDirectory(RootDirectoryDirectoryEntry.open(blockManager));
    }
}
