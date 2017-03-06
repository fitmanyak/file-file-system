package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class DirectoryDirectoryEntry extends DirectoryEntry implements IDirectoryDirectoryEntry {
    private DirectoryDirectoryEntry(IBlockFile entry, String name, IBlockManager blockManager) {
        super(entry, name, blockManager);
    }

    protected DirectoryDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                                      IBlockManager blockManager) {

        super(entry, contentSize, contentBlockChainHead, name, blockManager);
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    public static IDirectoryDirectoryEntry create(String name, IBlockManager blockManager)
            throws FileFileSystemException {

        return createNamed(DIRECTORY_FLAGS, name, blockManager, DirectoryDirectoryEntry::new);
    }

    public static IDirectoryDirectoryEntry open(int blockChainHead, IBlockManager blockManager)
            throws FileFileSystemException {

        return openTyped(blockChainHead, blockManager, DirectoryEntry::checkIsDirectory,
                DirectoryDirectoryEntry::createForOpen);
    }

    private static IDirectoryDirectoryEntry createForOpen(IBlockFile entry, boolean isDirectory, long contentSize,
                                                         int contentBlockChainHead, String name,
                                                         IBlockManager blockManager) {

        return new DirectoryDirectoryEntry(entry, contentSize, contentBlockChainHead, name, blockManager);
    }
}
