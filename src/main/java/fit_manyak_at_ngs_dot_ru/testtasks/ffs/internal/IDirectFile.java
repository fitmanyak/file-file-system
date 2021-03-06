package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.ICommonFile;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 05.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IDirectFile extends ICommonFile {
    @Override
    public boolean isEmpty();

    @Override
    public long getSize();

    @Override
    public long getPosition();

    @Override
    public void reset();

    public void savePosition(Position saved);
    public void setPosition(Position newPosition);

    public int write(Position newPosition, ByteBuffer source) throws FileFileSystemException;
}
