package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectory;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 06.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IParentDirectory extends IDirectory {
    public void checkNameUnique(String name) throws FileFileSystemException;

    public void removeItem(int entryBlockChainHead) throws FileFileSystemException;
}
