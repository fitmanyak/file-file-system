package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.BlockManager;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.IBlockManager;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.RootDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;

import java.nio.file.Path;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 19.02.2017.
 */

@SuppressWarnings("unused")
public class FileFileSystem implements IFileFileSystem {
    private final IRootDirectory rootDirectory;

    private final IBlockManager blockManager;

    private FileFileSystem(IRootDirectory rootDirectory, IBlockManager blockManager) {
        this.rootDirectory = rootDirectory;

        this.blockManager = blockManager;
    }

    @Override
    public void close() throws FileFileSystemException {
        blockManager.close();
    }

    @Override
    public long getTotalSpace() {
        return blockManager.getTotalSpace();
    }

    @Override
    public long getFreeSpace() {
        return blockManager.getFreeSpace();
    }

    @Override
    public IRootDirectory getRootDirectory() {
        return rootDirectory;
    }

    public static void format(Path path, long size) throws FileFileSystemException {
        BlockManager.format(path, size, RootDirectory::format);
    }

    public static IFileFileSystem mount(Path path) throws FileFileSystemException {
        return ErrorHandlingHelper.getWithCloseableArgument(() -> BlockManager.mount(path), FileFileSystem::mount);
    }

    private static IFileFileSystem mount(IBlockManager blockManager) throws FileFileSystemException {
        return new FileFileSystem(RootDirectory.open(blockManager), blockManager);
    }
}
