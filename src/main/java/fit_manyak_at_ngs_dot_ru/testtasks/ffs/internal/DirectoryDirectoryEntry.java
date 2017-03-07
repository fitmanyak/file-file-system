package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class DirectoryDirectoryEntry extends DirectoryEntry<IInternalDirectory> implements IDirectoryDirectoryEntry {
    private DirectoryDirectoryEntry(IBlockFile entry, String name, IBlockManager blockManager) {
        super(entry, name, blockManager);
    }

    protected DirectoryDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                                      IBlockManager blockManager) {

        super(entry, contentSize, contentBlockChainHead, name, blockManager);
    }

    @Override
    public IInternalDirectory getItem(IInternalDirectory parentDirectory) {
        return new Directory(this, parentDirectory);
    }

    public static IDirectoryDirectoryEntry create(String name, IBlockManager blockManager)
            throws FileFileSystemException {

        return createNamed(DIRECTORY_FLAGS_VALUE, name, blockManager, DirectoryDirectoryEntry::new);
    }
}
