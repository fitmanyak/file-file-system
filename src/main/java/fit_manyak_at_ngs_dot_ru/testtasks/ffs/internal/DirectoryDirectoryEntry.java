package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class DirectoryDirectoryEntry extends NamedDirectoryEntry {
    private DirectoryDirectoryEntry(IBlockFile entry, String name, BlockManager blockManager) {
        super(entry, name, blockManager);
    }

    protected DirectoryDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                                      BlockManager blockManager) {

        super(entry, contentSize, contentBlockChainHead, name, blockManager);
    }

    public static DirectoryDirectoryEntry create(String name, BlockManager blockManager)
            throws FileFileSystemException {

        return createNamed(DIRECTORY_FLAGS, name, blockManager,
                entry -> new DirectoryDirectoryEntry(entry, name, blockManager));
    }
}
