package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IFile extends Closeable {
    public int read(ByteBuffer buffer) throws IOException, IllegalArgumentException;

    public int write(ByteBuffer buffer) throws IOException, IllegalArgumentException;
}
