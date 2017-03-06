package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import java.io.Closeable;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface ICloseable extends Closeable {
    public void close() throws FileFileSystemException;
}
