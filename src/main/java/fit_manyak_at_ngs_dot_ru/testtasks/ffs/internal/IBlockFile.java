package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.ICloseable;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IRemovable;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IBlockFile extends IDirectFile, ICloseable, IRemovable, IBlockChainBased {
    public boolean isEmpty();

    public void setCalculatedSize(long newSize);
}
