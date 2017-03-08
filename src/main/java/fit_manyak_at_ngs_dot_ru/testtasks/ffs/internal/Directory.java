package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 06.03.2017.
 */

public class Directory extends BaseDirectory<IInternalDirectory, IDirectoryDirectoryEntry> {
    public Directory(IDirectoryDirectoryEntry entry, IInternalDirectory parentDirectory) {
        super(entry, parentDirectory);
    }

    protected static IInternalDirectory create(String name, IInternalDirectory parentDirectory,
                                               IBlockManager blockManager) throws FileFileSystemException {

        return createItem(name, parentDirectory, blockManager, DirectoryDirectoryEntry::create,
                Messages.DIRECTORY_CREATE_ERROR);
    }
}
