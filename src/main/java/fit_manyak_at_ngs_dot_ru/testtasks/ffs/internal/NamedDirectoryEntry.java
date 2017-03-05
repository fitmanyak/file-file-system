package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IProviderWithArgument;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public abstract class NamedDirectoryEntry extends DirectoryEntry {
    protected NamedDirectoryEntry(IBlockFile entry, String name, BlockManager blockManager) {
        super(entry, name, blockManager);
    }

    protected NamedDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                                  BlockManager blockManager) {

        super(entry, contentSize, contentBlockChainHead, name, blockManager);
    }

    protected static <T extends NamedDirectoryEntry> T createNamed(int flags, String name, BlockManager blockManager,
                                                                   IProviderWithArgument<T, IBlockFile> creator)
            throws FileFileSystemException {

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Empty directory entry name");// TODO
        }

        return create(flags, name, blockManager, creator);
    }
}
