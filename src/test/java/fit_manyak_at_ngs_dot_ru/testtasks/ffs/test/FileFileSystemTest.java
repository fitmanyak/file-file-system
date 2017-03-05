package fit_manyak_at_ngs_dot_ru.testtasks.ffs.test;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystem;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class FileFileSystemTest {
    private static final String TEST_PATH = "test.ffs";
    private static final long TEST_SIZE = 12345678;

    @Test
    public void test() throws FileFileSystemException {
        Path testPath = Paths.get(TEST_PATH);
        FileFileSystem.format(testPath, TEST_SIZE);

        try (FileFileSystem fileFileSystem = FileFileSystem.mount(testPath)) {
        }
    }
}
