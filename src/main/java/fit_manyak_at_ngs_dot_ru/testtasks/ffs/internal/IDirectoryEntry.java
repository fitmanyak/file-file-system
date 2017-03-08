package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.ICommonDirectoryEntry;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 06.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IDirectoryEntry<T extends IInternalDirectoryItem> extends ICommonDirectoryEntry, IBlockChainBased {
    public IDirectFile getContent() throws FileFileSystemException;

    public IBlockManager getBlockManager();

    public T getItem(IInternalDirectory parentDirectory);
}
