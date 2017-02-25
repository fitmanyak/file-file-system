package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 19.02.2017.
 */

public class FileFileSystem {
    private static final String TEST_PATH = "test.ffs";
    private static final long TEST_SIZE = 12345678;

    public static void main(String[] args) {
        try {
            Path testPath = Paths.get(TEST_PATH);
            BlockManager.format(testPath, TEST_SIZE, DirectoryEntry::formatRootDirectoryEntry);
            BlockManager.check(testPath, DirectoryEntry::checkRootDirectoryEntry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
