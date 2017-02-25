package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.messages.Messages;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 25.02.2017.
 */

public class BlockManager implements Closeable {
    private static final int SIGNATURE_SIZE = 2;
    private static final short SIGNATURE = (short) 0xFFF5;

    private static final int BLOCK_SIZE_RATIO_SIZE = 1;
    private static final byte BLOCK_SIZE_RATIO = 0;
    public static final int BLOCK_SIZE = 512;
    private static final int BLOCK_SIZE_MINUS_ONE = BLOCK_SIZE - 1;
    private static final int BLOCK_SIZE_EXPONENT = 9;

    private static final int BLOCK_INDEX_SIZE_EXPONENT_SIZE = 1;
    private static final byte BLOCK_INDEX_SIZE_EXPONENT = 0;
    public static final int BLOCK_INDEX_SIZE = 4;

    private static final int BLOCK_SIZE_PLUS_BLOCK_INDEX_SIZE = BLOCK_SIZE + BLOCK_INDEX_SIZE;

    private static final int MINIMAL_BLOCK_COUNT = 3;
    private static final long MINIMAL_SIZE = MINIMAL_BLOCK_COUNT * BLOCK_SIZE;
    private static final long MAXIMAL_SIZE = ((1L << (8L * BLOCK_INDEX_SIZE)) - 1L) * BLOCK_SIZE;

    private static final int CONTENT_SIZE_SIZE_EXPONENT_SIZE = 1;
    private static final byte CONTENT_SIZE_SIZE_EXPONENT = 0;
    public static final int CONTENT_SIZE_SIZE = 8;

    private static final int SIGNATURE_AND_GEOMETRY_SIZE =
            SIGNATURE_SIZE + BLOCK_SIZE_RATIO_SIZE + BLOCK_INDEX_SIZE_EXPONENT_SIZE + CONTENT_SIZE_SIZE_EXPONENT_SIZE +
                    BLOCK_INDEX_SIZE;

    private static final int FREE_BLOCK_DATA_SIZE = BLOCK_INDEX_SIZE + BLOCK_INDEX_SIZE;

    private static final int FIXED_SIZE_DATA_SIZE = SIGNATURE_AND_GEOMETRY_SIZE + FREE_BLOCK_DATA_SIZE;

    public static final int NULL_BLOCK_INDEX = 0;

    private static final int ROOT_DIRECTORY_ENTRY_BLOCK_COUNT = 1;

    private static final int FIRST_BLOCK_INITIAL_NEXT_BLOCK_INDEX = ROOT_DIRECTORY_ENTRY_BLOCK_COUNT + 1;

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    public interface IRootDirectoryEntryChecker {
        public void check(ByteBuffer rootDirectoryEntry) throws FileFileSystemException;
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface IReaderToBuffer {
        public int read(ByteBuffer buffer) throws IOException;
    }

    private final FileChannel channel;

    private final int blockCount;

    private final int freeBlockCount;
    private final int freeBlockChainHead;
    private final ByteBuffer freeBlockData;

    private final long blockTableOffset;

    private BlockManager(FileChannel channel, int blockCount, int freeBlockCount, int freeBlockChainHead,
                         long blockTableOffset) {

        this.channel = channel;

        this.blockCount = blockCount;

        this.freeBlockCount = freeBlockCount;
        this.freeBlockChainHead = freeBlockChainHead;
        this.freeBlockData = ByteBuffer.allocateDirect(FREE_BLOCK_DATA_SIZE);

        this.blockTableOffset = blockTableOffset;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public static void format(Path path, long size, Consumer<ByteBuffer> rootDirectoryEntryFormatter)
            throws IOException, IllegalArgumentException {

        if (size < MINIMAL_SIZE || size > MAXIMAL_SIZE) {
            throw new IllegalArgumentException(
                    String.format(Messages.BAD_SIZE_FOR_FORMAT_ERROR, size, MINIMAL_SIZE, MAXIMAL_SIZE));
        }

        try (RandomAccessFile file = new RandomAccessFile(path.toString(), "rw")) {
            long blockCountLong = getRequiredBlockCountLong(size);
            file.setLength(getTotalSize(blockCountLong));

            try (FileChannel channel = file.getChannel()) {
                int blockCount = (int) blockCountLong;
                ByteBuffer fixedSizeData = ByteBuffer.allocateDirect(FIXED_SIZE_DATA_SIZE);
                fixedSizeData.putShort(SIGNATURE);
                fixedSizeData.put(BLOCK_SIZE_RATIO);
                fixedSizeData.put(BLOCK_INDEX_SIZE_EXPONENT);
                fixedSizeData.put(CONTENT_SIZE_SIZE_EXPONENT);
                fixedSizeData.putInt(blockCount);
                fixedSizeData.putInt(blockCount - ROOT_DIRECTORY_ENTRY_BLOCK_COUNT);
                fixedSizeData.putInt(ROOT_DIRECTORY_ENTRY_BLOCK_COUNT);
                flipBufferAndWrite(fixedSizeData, channel);

                ByteBuffer nextBlockIndex = ByteBuffer.allocateDirect(BLOCK_INDEX_SIZE);
                writeBlockIndexAndFlipBuffer(NULL_BLOCK_INDEX, nextBlockIndex, channel);

                for (int i = FIRST_BLOCK_INITIAL_NEXT_BLOCK_INDEX; i < blockCount; i++) {
                    writeBlockIndexAndFlipBuffer(i, nextBlockIndex, channel);
                }

                writeBlockIndex(NULL_BLOCK_INDEX, nextBlockIndex, channel);

                ByteBuffer rootDirectoryEntry = ByteBuffer.allocateDirect(BLOCK_SIZE);
                rootDirectoryEntryFormatter.accept(rootDirectoryEntry);
                flipBufferAndWrite(rootDirectoryEntry, channel);
            }
        }
    }

    private static int getRequiredBlockCount(long size) {
        return (int) getRequiredBlockCountLong(size);
    }

    private static long getRequiredBlockCountLong(long size) {
        return (size + BLOCK_SIZE_MINUS_ONE) >> BLOCK_SIZE_EXPONENT;
    }

    private static long getTotalSize(int blockCount) {
        return getTotalSize(Integer.toUnsignedLong(blockCount));
    }

    private static long getTotalSize(long blockCountLong) {
        return FIXED_SIZE_DATA_SIZE + blockCountLong * BLOCK_SIZE_PLUS_BLOCK_INDEX_SIZE;
    }

    private static void flipBufferAndWrite(ByteBuffer buffer, FileChannel channel) throws IOException {
        buffer.flip();
        channel.write(buffer);
    }

    private static void writeBlockIndexAndFlipBuffer(int index, ByteBuffer buffer, FileChannel channel)
            throws IOException {

        writeBlockIndex(index, buffer, channel);
        buffer.flip();
    }

    private static void writeBlockIndex(int index, ByteBuffer buffer, FileChannel channel) throws IOException {
        buffer.putInt(index);
        flipBufferAndWrite(buffer, channel);
    }

    public static BlockManager mount(Path path, IRootDirectoryEntryChecker rootDirectoryEntryChecker)
            throws IOException {

        FileChannel channel = null;
        try {
            channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);

            ByteBuffer fixedSizeData =
                    readAndFlipBuffer(FIXED_SIZE_DATA_SIZE, channel, Messages.FIXED_SIZE_DATA_READ_ERROR);

            if (fixedSizeData.getShort() != SIGNATURE) {
                throw new FileFileSystemException(Messages.BAD_SIGNATURE_ERROR);
            }

            if (fixedSizeData.get() != BLOCK_SIZE_RATIO) {
                throw new FileFileSystemException(Messages.BAD_BLOCK_SIZE_RATIO_ERROR);
            }

            if (fixedSizeData.get() != BLOCK_INDEX_SIZE_EXPONENT) {
                throw new FileFileSystemException(Messages.BAD_BLOCK_INDEX_SIZE_EXPONENT_ERROR);
            }

            if (fixedSizeData.get() != CONTENT_SIZE_SIZE_EXPONENT) {
                throw new FileFileSystemException(Messages.BAD_CONTENT_SIZE_SIZE_EXPONENT_ERROR);
            }

            int blockCount = fixedSizeData.getInt();
            if (Integer.compareUnsigned(blockCount, MINIMAL_BLOCK_COUNT) < 0) {
                throw new FileFileSystemException(Messages.BAD_BLOCK_COUNT_ERROR);
            }

            int freeBlockCount = fixedSizeData.getInt();
            if (Integer.compareUnsigned(blockCount, freeBlockCount) < 0) {
                throw new FileFileSystemException(Messages.BAD_FREE_BLOCK_COUNT_ERROR);
            }

            int freeBlockChainHead = fixedSizeData.getInt();
            checkBlockChainHead(() -> (freeBlockCount == 0), freeBlockChainHead,
                    Messages.BAD_FREE_BLOCK_CHAIN_HEAD_ERROR);

            if (channel.size() != getTotalSize(blockCount)) {
                throw new FileFileSystemException(Messages.BAD_SIZE_ERROR);
            }

            ByteBuffer rootDirectoryEntryBlockNextBlockIndex =
                    readAndFlipBuffer(BLOCK_INDEX_SIZE, channel, Messages.NEXT_BLOCK_INDEX_READ_ERROR);
            if (rootDirectoryEntryBlockNextBlockIndex.getInt() != NULL_BLOCK_INDEX) {
                throw new FileFileSystemException(Messages.BAD_ROOT_DIRECTORY_ENTRY_BLOCK_NEXT_BLOCK_INDEX_ERROR);
            }

            long blockTableOffset = getBlockTableOffset(blockCount);
            ByteBuffer rootDirectoryEntry =
                    readAndFlipBuffer(BLOCK_SIZE, channel, blockTableOffset, Messages.BLOCK_READ_ERROR);
            rootDirectoryEntryChecker.check(rootDirectoryEntry);

            return new BlockManager(channel, blockCount, freeBlockCount, freeBlockChainHead, blockTableOffset);
        } catch (Throwable t) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (Throwable closeThrowable) {
                    t.addSuppressed(closeThrowable);
                }
            }

            throw t;
        }
    }

    private static ByteBuffer readAndFlipBuffer(int size, FileChannel channel, String errorMessage) throws IOException {
        return readAndFlipBuffer(size, channel::read, errorMessage);
    }

    private static ByteBuffer readAndFlipBuffer(int size, FileChannel channel, long position, String errorMessage)
            throws IOException {

        return readAndFlipBuffer(size, buffer -> channel.read(buffer, position), errorMessage);
    }

    private static ByteBuffer readAndFlipBuffer(int size, IReaderToBuffer reader, String errorMessage)
            throws IOException {

        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        if (reader.read(buffer) != size) {
            throw new FileFileSystemException(errorMessage);
        }
        buffer.flip();

        return buffer;
    }

    public static void checkBlockChainHead(Supplier<Boolean> isEmptyProvider, int blockChainHead, String errorMessage)
            throws FileFileSystemException {

        boolean isEmpty = isEmptyProvider.get();
        if ((isEmpty && blockChainHead != NULL_BLOCK_INDEX) || (!isEmpty && blockChainHead == NULL_BLOCK_INDEX)) {
            throw new FileFileSystemException(errorMessage);
        }
    }

    private static long getBlockTableOffset(int blockCount) {
        return FIXED_SIZE_DATA_SIZE + Integer.toUnsignedLong(blockCount) * BLOCK_INDEX_SIZE;
    }
}
