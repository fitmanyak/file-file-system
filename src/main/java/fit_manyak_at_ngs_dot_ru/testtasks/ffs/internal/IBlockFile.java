package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IBlockFile extends ICommonFile {
    public int getBlockChainHead();
}
