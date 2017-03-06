package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.ICloseable;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 06.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IBlockManager extends ICloseable {
    public static final int BLOCK_INDEX_SIZE = 4;

    public static final int CONTENT_SIZE_SIZE = 8;

    public static final int NULL_BLOCK_INDEX = 0;

    public static final int ROOT_DIRECTORY_ENTRY_BLOCK_INDEX = 0;

    public IBlockFile createBlockFile(long size) throws FileFileSystemException;

    public IBlockFile openBlockFile(long size, int blockChainHead) throws FileFileSystemException;
    public IBlockFile openBlockFile(long size, int blockChainHead, boolean skipCheckBlockChainHead)
            throws FileFileSystemException;
}
