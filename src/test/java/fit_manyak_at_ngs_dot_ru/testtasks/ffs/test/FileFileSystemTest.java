package fit_manyak_at_ngs_dot_ru.testtasks.ffs.test;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystem;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFileFileSystem;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class FileFileSystemTest {
    private static final String TEST_PATH = "test.ffs";
    private static final long TEST_SIZE = 12345678L;
    private static final long TEST_TOTAL_SPACE = 12249600L;
    private static final long TEST_FREE_SPACE = 12249088L;

    @Test
    public void test() throws FileFileSystemException {
        Path testPath = Paths.get(TEST_PATH);
        FileFileSystem.format(testPath, TEST_SIZE);

        try (IFileFileSystem fileFileSystem = FileFileSystem.mount(testPath)) {
            Assert.assertEquals("Wrong total space", TEST_TOTAL_SPACE, fileFileSystem.getTotalSpace());
            Assert.assertEquals("Wrong free space", TEST_FREE_SPACE, fileFileSystem.getFreeSpace());
        }
    }
}
