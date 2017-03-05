package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.BlockManager;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.RootDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;

import java.nio.file.Path;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 19.02.2017.
 */

public class FileFileSystem implements ICloseable {
    private final RootDirectory rootDirectory;

    private final BlockManager blockManager;

    private FileFileSystem(RootDirectory rootDirectory, BlockManager blockManager) {
        this.rootDirectory = rootDirectory;

        this.blockManager = blockManager;
    }

    @Override
    public void close() throws FileFileSystemException {
        blockManager.close();
    }

    public IDirectory getRootDirectory() {
        return rootDirectory;
    }

    public static void format(Path path, long size) throws FileFileSystemException {
        BlockManager.format(path, size, RootDirectory::format);
    }

    public static FileFileSystem mount(Path path) throws FileFileSystemException {
        return ErrorHandlingHelper.getWithCloseableArgument(() -> BlockManager.mount(path), FileFileSystem::mount);
    }

    private static FileFileSystem mount(BlockManager blockManager) throws FileFileSystemException {
        return new FileFileSystem(RootDirectory.open(blockManager), blockManager);
    }
}
