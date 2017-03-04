package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IDirectory extends Closeable {
    public IFile createFile(String name) throws IOException, IllegalArgumentException;
    public IFile openFile(String name) throws IOException, IllegalArgumentException;
}
