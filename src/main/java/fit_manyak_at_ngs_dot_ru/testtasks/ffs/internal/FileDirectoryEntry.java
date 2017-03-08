package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class FileDirectoryEntry extends DirectoryEntry<IInternalFile> implements IFileDirectoryEntry {
    private FileDirectoryEntry(IBlockFile entry, String name, IBlockManager blockManager) {
        super(entry, name, blockManager);
    }

    FileDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                       IBlockManager blockManager) {

        super(entry, contentSize, contentBlockChainHead, name, blockManager);
    }

    @Override
    public IInternalFile getItem(IInternalDirectory parentDirectory) {
        return new File(this, parentDirectory);
    }

    public static IFileDirectoryEntry create(String name, IBlockManager blockManager) throws FileFileSystemException {
        return createNamed(FILE_FLAGS_VALUE, name, blockManager, FileDirectoryEntry::new);
    }
}
