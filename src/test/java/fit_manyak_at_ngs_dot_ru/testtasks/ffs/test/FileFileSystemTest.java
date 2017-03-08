package fit_manyak_at_ngs_dot_ru.testtasks.ffs.test;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystem;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFileFileSystem;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.BlockManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class FileFileSystemTest {
    private static final String PATH = "test.ffs";

    private static final long SIZE = 12345678L;

    private static final int TOTAL_BLOCK_COUNT = BlockManager.getBlockCount(SIZE);
    private static final long TOTAL_SPACE = BlockManager.getSize(TOTAL_BLOCK_COUNT);
    private static final int FREE_BLOCK_COUNT = TOTAL_BLOCK_COUNT - BlockManager.ROOT_DIRECTORY_ENTRY_BLOCK_COUNT;
    private static final long FREE_SPACE = BlockManager.getSize(FREE_BLOCK_COUNT);

    private static final String DIRECTORY = "Directory";
    private static final int DIRECTORY_FREE_BLOCK_COUNT = FREE_BLOCK_COUNT - 2;
    private static final long DIRECTORY_FREE_SPACE = BlockManager.getSize(DIRECTORY_FREE_BLOCK_COUNT);

    private static final String FILE = "File";
    private static final int FILE_FREE_BLOCK_COUNT = DIRECTORY_FREE_BLOCK_COUNT - 2;
    private static final long FILE_FREE_SPACE = BlockManager.getSize(FILE_FREE_BLOCK_COUNT);
    private static final long FILE_SIZE = 12345L;
    private static final int FILE_RESIZE_FREE_BLOCK_COUNT = FILE_FREE_BLOCK_COUNT - BlockManager.getRequiredBlockCount(FILE_SIZE);
    private static final long FILE_RESIZE_FREE_SPACE = BlockManager.getSize(FILE_RESIZE_FREE_BLOCK_COUNT);

    private IFileFileSystem fileFileSystem;

    @Before
    public void prepare() throws FileFileSystemException {
        Path path = Paths.get(PATH);
        FileFileSystem.format(path, SIZE);

        fileFileSystem = FileFileSystem.mount(path);
    }

    @After
    public void finish() throws FileFileSystemException {
        if (fileFileSystem != null) {
            fileFileSystem.close();
        }
    }

    @Test
    public void testSpaces() throws FileFileSystemException {
        Assert.assertEquals(TOTAL_SPACE, fileFileSystem.getTotalSpace());
        Assert.assertEquals(FREE_SPACE, fileFileSystem.getFreeSpace());
    }

    @Test
    public void testCreateDirectory() throws FileFileSystemException {
        fileFileSystem.getRootDirectory().createSubDirectory(DIRECTORY);

        Assert.assertEquals(DIRECTORY_FREE_SPACE, fileFileSystem.getFreeSpace());
    }

    @Test
    public void testRemoveDirectory() throws FileFileSystemException {
        testCreateDirectory();

        removeDirectory();
    }

    private void removeDirectory() throws FileFileSystemException {
        fileFileSystem.getRootDirectory().openSubDirectory(DIRECTORY).remove();

        Assert.assertEquals(FREE_SPACE, fileFileSystem.getFreeSpace());
    }

    @Test
    public void testCreateFile() throws FileFileSystemException {
        testCreateDirectory();

        fileFileSystem.getRootDirectory().openSubDirectory(DIRECTORY).createFile(FILE);

        Assert.assertEquals(FILE_FREE_SPACE, fileFileSystem.getFreeSpace());
    }

    @Test
    public void testResizeFile() throws FileFileSystemException {
        testCreateFile();

        fileFileSystem.getRootDirectory().openSubDirectory(DIRECTORY).openFile(FILE).setSize(FILE_SIZE);

        Assert.assertEquals(FILE_RESIZE_FREE_SPACE, fileFileSystem.getFreeSpace());
    }

    @Test
    public void testClearFile() throws FileFileSystemException {
        testResizeFile();

        fileFileSystem.getRootDirectory().openSubDirectory(DIRECTORY).openFile(FILE).setSize(0L);

        Assert.assertEquals(FILE_FREE_SPACE, fileFileSystem.getFreeSpace());
    }

    @Test
    public void testRemoveFile() throws FileFileSystemException {
        testResizeFile();

        fileFileSystem.getRootDirectory().openSubDirectory(DIRECTORY).openFile(FILE).remove();

        Assert.assertEquals(DIRECTORY_FREE_SPACE, fileFileSystem.getFreeSpace());

        removeDirectory();
    }
}
