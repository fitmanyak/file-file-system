package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

import java.io.IOException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public abstract class NamedDirectoryEntry extends NewDirectoryEntry {
    protected NamedDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                                  BlockManager blockManager) {

        super(entry, contentSize, contentBlockChainHead, name, blockManager);
    }

    protected static <T extends NamedDirectoryEntry> T createNamed(int flags, String name, BlockManager blockManager,
                                                                   ICreator<T> creator)
            throws IOException, IllegalArgumentException {

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Empty directory entry name");// TODO
        }

        return create(flags, name, blockManager, creator);
    }
}
