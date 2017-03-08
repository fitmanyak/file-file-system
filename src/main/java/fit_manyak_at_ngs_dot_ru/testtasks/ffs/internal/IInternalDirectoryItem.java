package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectoryItem;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 06.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
interface IInternalDirectoryItem extends IDirectoryItem, IBlockChainBased {
    public IInternalFile getAsFile() throws FileFileSystemException;
    public IInternalDirectory getAsDirectory() throws FileFileSystemException;
}
