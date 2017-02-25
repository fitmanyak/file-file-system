package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.messages.Messages;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 19.02.2017.
 */

public class FileFileSystem {
    private static final int SIGNATURE_SIZE = 2;
    private static final short SIGNATURE = (short) 0xFFF5;

    private static final int BLOCK_SIZE_RATIO_SIZE = 1;
    private static final byte BLOCK_SIZE_RATIO = 0;
    private static final int BLOCK_SIZE = 512;
    private static final int BLOCK_SIZE_MINUS_ONE = BLOCK_SIZE - 1;
    private static final int BLOCK_SIZE_EXPONENT = 9;

    private static final int BLOCK_INDEX_SIZE_EXPONENT_SIZE = 1;
    private static final byte BLOCK_INDEX_SIZE_EXPONENT = 0;
    private static final int BLOCK_INDEX_SIZE = 4;

    private static final int BLOCK_SIZE_PLUS_BLOCK_INDEX_SIZE = BLOCK_SIZE + BLOCK_INDEX_SIZE;

    private static final int MINIMAL_BLOCK_COUNT = 3;
    private static final long MINIMAL_SIZE = MINIMAL_BLOCK_COUNT * BLOCK_SIZE;
    private static final long MAXIMAL_SIZE = ((1L << (8L * BLOCK_INDEX_SIZE)) - 1L) * BLOCK_SIZE;

    private static final int CONTENT_SIZE_SIZE_EXPONENT_SIZE = 1;
    private static final byte CONTENT_SIZE_SIZE_EXPONENT = 0;
    private static final int CONTENT_SIZE_SIZE = 8;

    private static final int SIGNATURE_AND_GEOMETRY_SIZE =
            SIGNATURE_SIZE + BLOCK_SIZE_RATIO_SIZE + BLOCK_INDEX_SIZE_EXPONENT_SIZE + CONTENT_SIZE_SIZE_EXPONENT_SIZE +
                    BLOCK_INDEX_SIZE;

    private static final int FREE_BLOCK_DATA_SIZE = BLOCK_INDEX_SIZE + BLOCK_INDEX_SIZE;

    private static final int FIXED_SIZE_DATA_SIZE = SIGNATURE_AND_GEOMETRY_SIZE + FREE_BLOCK_DATA_SIZE;

    private static final String TEST_PATH = "test.ffs";
    private static final long TEST_SIZE = 12345678;

    public static void format(Path path, long size) throws IOException, IllegalArgumentException {
        if (size < MINIMAL_SIZE || size > MAXIMAL_SIZE) {
            throw new IllegalArgumentException(
                    String.format(Messages.BAD_SIZE_FOR_FORMAT_ERROR, size, MINIMAL_SIZE, MAXIMAL_SIZE));
        }

        try (RandomAccessFile file = new RandomAccessFile(path.toString(), "rw")) {
            long blockCountLong = getRequiredBlockCountLong(size);
            file.setLength(getTotalSize(blockCountLong));

            try (FileChannel channel = file.getChannel()) {
                int blockCount = (int) blockCountLong;
                ByteBuffer fixedSizeData = allocateFixedSizeDataBuffer();
                fixedSizeData.putShort(SIGNATURE);
                fixedSizeData.put(BLOCK_SIZE_RATIO);
                fixedSizeData.put(BLOCK_INDEX_SIZE_EXPONENT);
                fixedSizeData.put(CONTENT_SIZE_SIZE_EXPONENT);
                fixedSizeData.putInt(blockCount);
                fixedSizeData.putInt(blockCount);
                fixedSizeData.putInt(0);

                flipBufferAndWrite(fixedSizeData, channel);

                ByteBuffer nextBlockIndex = allocateBlockIndexBuffer();
                for (int i = 0; i < blockCount; i++) {
                    writeBlockIndex((i + 1), nextBlockIndex, channel);

                    nextBlockIndex.flip();
                }

                writeBlockIndex(0, nextBlockIndex, channel);
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

    private static ByteBuffer allocateFixedSizeDataBuffer() {
        return ByteBuffer.allocateDirect(FIXED_SIZE_DATA_SIZE);
    }

    private static ByteBuffer allocateBlockIndexBuffer() {
        return ByteBuffer.allocateDirect(BLOCK_INDEX_SIZE);
    }

    private static void flipBufferAndWrite(ByteBuffer buffer, FileChannel channel) throws IOException {
        buffer.flip();
        channel.write(buffer);
    }

    private static void writeBlockIndex(int index, ByteBuffer buffer, FileChannel channel) throws IOException {
        buffer.putInt(index);
        flipBufferAndWrite(buffer, channel);
    }

    private static void check(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer fixedSizeData = allocateFixedSizeDataBuffer();
            if (channel.read(fixedSizeData) != FIXED_SIZE_DATA_SIZE) {
                throw new FileFileSystemException(Messages.BAD_SIZE_OF_FIXED_SIZE_DATA_ERROR);
            }
            fixedSizeData.flip();

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

            if (channel.size() != getTotalSize(blockCount)) {
                throw new FileFileSystemException(Messages.BAD_SIZE_ERROR);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Path testPath = Paths.get(TEST_PATH);
            format(testPath, TEST_SIZE);
            check(testPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
