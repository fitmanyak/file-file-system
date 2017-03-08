package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

@SuppressWarnings({"UnnecessaryInterfaceModifier", "unused"})
public interface ICommonFile extends IEmptyCheckable {
    public long getSize() throws FileFileSystemException;
    public void setSize(long newSize) throws FileFileSystemException;

    public long getPosition() throws FileFileSystemException;
    public void setPosition(long newPosition) throws FileFileSystemException;

    public void reset() throws FileFileSystemException;

    public int read(ByteBuffer destination) throws FileFileSystemException;
    public int read(long newPosition, ByteBuffer destination) throws FileFileSystemException;

    public int write(ByteBuffer source) throws FileFileSystemException;
    public int write(long newPosition, ByteBuffer source) throws FileFileSystemException;
}
