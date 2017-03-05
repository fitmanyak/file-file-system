package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class FileDirectoryEntry extends DirectoryEntry {
    private FileDirectoryEntry(IBlockFile entry, String name, BlockManager blockManager) {
        super(entry, name, blockManager);
    }

    protected FileDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                                 BlockManager blockManager) {

        super(entry, contentSize, contentBlockChainHead, name, blockManager);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public static FileDirectoryEntry create(String name, BlockManager blockManager) throws FileFileSystemException {
        return createNamed(FILE_FLAGS, name, blockManager, FileDirectoryEntry::new);
    }

    public static FileDirectoryEntry open(int blockChainHead, BlockManager blockManager)
            throws FileFileSystemException {

        return openTyped(blockChainHead, blockManager, FileDirectoryEntry::checkIsFile,
                FileDirectoryEntry::createForOpen);
    }

    private static void checkIsFile(boolean isDirectory) throws FileFileSystemException {
        if (isDirectory) {
            throw new FileFileSystemException("Directory entry is directory directory entry");// TODO
        }
    }

    private static FileDirectoryEntry createForOpen(IBlockFile entry, boolean isDirectory, long contentSize,
                                                    int contentBlockChainHead, String name, BlockManager blockManager) {

        return new FileDirectoryEntry(entry, contentSize, contentBlockChainHead, name, blockManager);
    }
}
