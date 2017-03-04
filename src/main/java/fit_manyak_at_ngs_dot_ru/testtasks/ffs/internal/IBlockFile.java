package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.ICommonFile;

import java.io.Closeable;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IBlockFile extends ICommonFile, Closeable {
    public int getBlockChainHead();

    public void setCalculatedSize(long newSize);
}
