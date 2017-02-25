package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 19.02.2017.
 */

public class FileFileSystem implements Closeable {
    private static final String TEST_PATH = "test.ffs";
    private static final long TEST_SIZE = 12345678;

    private final BlockManager blockManager;

    private FileFileSystem(BlockManager blockManager) {
        this.blockManager = blockManager;
    }

    @Override
    public void close() throws IOException {
        blockManager.close();
    }

    public static void format(Path path, long size) throws IOException, IllegalArgumentException {
        BlockManager.format(path, size, DirectoryEntry::formatRootDirectoryEntry);
    }

    public static FileFileSystem mount(Path path) throws IOException {
        return new FileFileSystem(BlockManager.mount(path, DirectoryEntry::checkRootDirectoryEntry));
    }

    public static void main(String[] args) {
        try {
            Path testPath = Paths.get(TEST_PATH);
            format(testPath, TEST_SIZE);

            try (FileFileSystem fileFileSystem = mount(testPath)) {
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
