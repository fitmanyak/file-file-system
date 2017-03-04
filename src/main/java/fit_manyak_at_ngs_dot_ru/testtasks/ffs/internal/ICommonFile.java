package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface ICommonFile {
    public long getSize();
    public void setSize(long newSize) throws IOException, IllegalArgumentException;

    public long getPosition();
    public void setPosition(long newPosition) throws IOException, IllegalArgumentException;

    public void reset();

    public void clear() throws IOException, IllegalArgumentException;

    public int read(ByteBuffer destination) throws IOException, IllegalArgumentException;
    public int read(long newPosition, ByteBuffer destination) throws IOException, IllegalArgumentException;

    public int write(ByteBuffer source) throws IOException, IllegalArgumentException;
    public int write(long newPosition, ByteBuffer source) throws IOException, IllegalArgumentException;
}
