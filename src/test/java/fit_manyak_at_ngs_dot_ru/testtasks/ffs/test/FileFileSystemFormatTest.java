package fit_manyak_at_ngs_dot_ru.testtasks.ffs.test;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystem;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.BlockManager;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.DirectoryEntry;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.IBlockManager;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 08.03.2017.
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class FileFileSystemFormatTest {
    private static final String PATH = "format.ffs";

    private static final long SMALL_SIZE = BlockManager.MINIMAL_SIZE - 1L;
    private static final long BIG_SIZE = BlockManager.MAXIMAL_SIZE + 1L;

    private static final long SIZE = 123456L;
    private static final int BLOCK_COUNT = BlockManager.getBlockCount(SIZE);
    private static final int FREE_BLOCK_COUNT = BLOCK_COUNT - BlockManager.ROOT_DIRECTORY_ENTRY_BLOCK_COUNT;
    private static final long FILE_SIZE = BlockManager.getTotalSize(BLOCK_COUNT);

    //private static

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Path path;

    @Before
    public void prepare() {
        path = Paths.get(PATH);
    }

    @Test
    public void testSmallSize() throws FileFileSystemException {
        testBadSize(SMALL_SIZE);
    }

    private void testBadSize(long badSize) throws FileFileSystemException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(CoreMatchers
                .equalTo(String.format(Messages.BAD_SIZE_FOR_FORMAT_ERROR, badSize, BlockManager.MINIMAL_SIZE,
                        BlockManager.MAXIMAL_SIZE)));

        FileFileSystem.format(path, badSize);
    }

    @Test
    public void testBigSize() throws FileFileSystemException {
        testBadSize(BIG_SIZE);
    }

    @Test
    public void testSize() throws IOException {
        FileFileSystem.format(path, SIZE);

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            Assert.assertEquals(FILE_SIZE, channel.size());

            ByteBuffer fixedSizeData = createReadAndFlipBuffer(BlockManager.FIXED_SIZE_DATA_SIZE, channel);
            Assert.assertEquals(BlockManager.SIGNATURE_VALUE, fixedSizeData.getShort());
            Assert.assertEquals(BlockManager.BLOCK_SIZE_RATIO_VALUE, fixedSizeData.get());
            Assert.assertEquals(BlockManager.BLOCK_INDEX_SIZE_EXPONENT_VALUE, fixedSizeData.get());
            Assert.assertEquals(BlockManager.CONTENT_SIZE_SIZE_EXPONENT_VALUE, fixedSizeData.get());
            Assert.assertEquals(BLOCK_COUNT, fixedSizeData.getInt());
            Assert.assertEquals(FREE_BLOCK_COUNT, fixedSizeData.getInt());
            Assert.assertEquals(BlockManager.ROOT_DIRECTORY_ENTRY_BLOCK_COUNT, fixedSizeData.getInt());

            ByteBuffer nextBlockIndex = ByteBuffer.allocateDirect(IBlockManager.BLOCK_INDEX_SIZE);
            assertNextBlockIndex(IBlockManager.NULL_BLOCK_INDEX, nextBlockIndex, channel);

            for (int i = BlockManager.FIRST_BLOCK_INITIAL_NEXT_BLOCK_INDEX; i < BLOCK_COUNT; i++) {
                assertNextBlockIndex(i, nextBlockIndex, channel);
            }

            assertNextBlockIndex(IBlockManager.NULL_BLOCK_INDEX, nextBlockIndex, channel);

            ByteBuffer rootDirectoryEntry = createReadAndFlipBuffer(BlockManager.BLOCK_SIZE, channel);
            Assert.assertEquals(DirectoryEntry.DIRECTORY_FLAGS_VALUE, rootDirectoryEntry.getInt());
            Assert.assertEquals(0L, rootDirectoryEntry.getLong());
            Assert.assertEquals(0, rootDirectoryEntry.getInt());
            Assert.assertEquals(0, rootDirectoryEntry.getShort());
        }
    }
    private ByteBuffer createReadAndFlipBuffer(int size, FileChannel channel) throws IOException {
        ByteBuffer destination = ByteBuffer.allocateDirect(size);
        readAndFlipBuffer(destination, channel);

        return destination;
    }

    private void readAndFlipBuffer(ByteBuffer destination, FileChannel channel) throws IOException {
        channel.read(destination);
        destination.flip();
    }

    private void assertNextBlockIndex(int expectedNextBlockIndex, ByteBuffer nextBlockIndex, FileChannel channel)
            throws IOException {

        readAndFlipBuffer(nextBlockIndex, channel);
        Assert.assertEquals(expectedNextBlockIndex, nextBlockIndex.getInt());
        nextBlockIndex.clear();
    }
}
