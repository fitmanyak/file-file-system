package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class RootDirectoryDirectoryEntry extends DirectoryEntry<IInternalRootDirectory>
        implements IRootDirectoryDirectoryEntry {

    private static final String NAME = "";
    private static final byte[] NAME_BYTES = new byte[0];

    private RootDirectoryDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead,
                                        IBlockManager blockManager) {

        super(entry, contentSize, contentBlockChainHead, NAME, blockManager);
    }

    @Override
    public void setName(String newName) throws FileFileSystemException {
        throw new UnsupportedOperationException(Messages.CANT_RENAME_ROOT_DIRECTORY_ERROR);
    }

    @Override
    public void remove() throws FileFileSystemException {
        throw new UnsupportedOperationException(Messages.CANT_REMOVE_ROOT_DIRECTORY_ERROR);
    }

    @Override
    public IInternalRootDirectory getItem(IInternalDirectory parentDirectory) {
        throw new UnsupportedOperationException(Messages.CANT_GET_ROOT_DIRECTORY_THROUGH_ENTRY_ERROR);
    }

    static void format(ByteBuffer block) {
        fillNewEntryData(block, DIRECTORY_FLAGS_VALUE, NAME_BYTES);
    }

    static IRootDirectoryDirectoryEntry open(IBlockManager blockManager) throws FileFileSystemException {
        return open(IBlockManager.ROOT_DIRECTORY_ENTRY_BLOCK_INDEX, true, blockManager,
                RootDirectoryDirectoryEntry::open);
    }

    private static IRootDirectoryDirectoryEntry open(IBlockFile entry, boolean isDirectory, long contentSize,
                                                     int contentBlockChainHead, int nameSize,
                                                     IBlockManager blockManager)
            throws FileFileSystemException {

        if (!isDirectory) {
            throw new FileFileSystemException(Messages.BAD_ROOT_DIRECTORY_ENTRY_FLAGS_ERROR);
        }

        if (nameSize != 0) {
            throw new FileFileSystemException(Messages.BAD_ROOT_DIRECTORY_ENTRY_NAME_ERROR);
        }

        return new RootDirectoryDirectoryEntry(entry, contentSize, contentBlockChainHead, blockManager);
    }
}
