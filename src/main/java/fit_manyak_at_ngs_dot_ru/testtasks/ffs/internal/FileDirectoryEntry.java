package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class FileDirectoryEntry extends DirectoryEntry implements IFileDirectoryEntry {
    private FileDirectoryEntry(IBlockFile entry, String name, IBlockManager blockManager) {
        super(entry, name, blockManager);
    }

    protected FileDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                                 IBlockManager blockManager) {

        super(entry, contentSize, contentBlockChainHead, name, blockManager);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public static IFileDirectoryEntry create(String name, IBlockManager blockManager) throws FileFileSystemException {
        return createNamed(FILE_FLAGS, name, blockManager, FileDirectoryEntry::new);
    }

    public static IFileDirectoryEntry open(int blockChainHead, IBlockManager blockManager)
            throws FileFileSystemException {

        return openTyped(blockChainHead, blockManager, FileDirectoryEntry::checkIsFile,
                FileDirectoryEntry::createForOpen);
    }

    private static void checkIsFile(boolean isDirectory) throws FileFileSystemException {
        if (isDirectory) {
            throw new FileFileSystemException("Directory entry is directory directory entry");// TODO
        }
    }

    private static IFileDirectoryEntry createForOpen(IBlockFile entry, boolean isDirectory, long contentSize,
                                                    int contentBlockChainHead, String name,
                                                    IBlockManager blockManager) {

        return new FileDirectoryEntry(entry, contentSize, contentBlockChainHead, name, blockManager);
    }
}
