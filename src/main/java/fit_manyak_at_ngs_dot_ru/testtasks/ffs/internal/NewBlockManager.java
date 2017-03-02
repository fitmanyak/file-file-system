package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class NewBlockManager implements Closeable {
    private static final int SIGNATURE_SIZE = 2;

    private static final int BLOCK_SIZE_RATIO_SIZE = 1;
    private static final int BLOCK_SIZE = 512;
    private static final int BLOCK_SIZE_MINUS_ONE = BLOCK_SIZE - 1;
    private static final int BLOCK_SIZE_EXPONENT = 9;

    private static final int BLOCK_INDEX_SIZE_EXPONENT_SIZE = 1;
    private static final int BLOCK_INDEX_SIZE = 4;

    private static final int CONTENT_SIZE_SIZE_EXPONENT_SIZE = 1;

    private static final int SIGNATURE_AND_GEOMETRY_SIZE =
            SIGNATURE_SIZE + BLOCK_SIZE_RATIO_SIZE + BLOCK_INDEX_SIZE_EXPONENT_SIZE + CONTENT_SIZE_SIZE_EXPONENT_SIZE +
                    BLOCK_INDEX_SIZE;

    private static final int FREE_BLOCK_DATA_SIZE = BLOCK_INDEX_SIZE + BLOCK_INDEX_SIZE;
    private static final long FREE_BLOCK_DATA_POSITION = SIGNATURE_AND_GEOMETRY_SIZE;

    private static final int FIXED_SIZE_DATA_SIZE = SIGNATURE_AND_GEOMETRY_SIZE + FREE_BLOCK_DATA_SIZE;

    private static final int NULL_BLOCK_INDEX = 0;

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface IBlockFileSizeChanger {
        public void resize(int newBlockChainLength) throws IOException;
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface IBlockFileWithinBlockIOOperationProcessor {
        public void process(int blockIndex, int withinBlockPosition, ByteBuffer buffer) throws IOException;
    }

    public class BlockFile {
        private long size;
        private int blockChainLength;
        private int blockChainHead;

        private long position;
        private int withinBlockPosition;

        private int blockIndex;
        private int withinChainIndex;

        private BlockFile() {
            this(0L, NULL_BLOCK_INDEX);
        }

        private BlockFile(long size, int blockChainHead) {
            this.size = size;
            this.blockChainLength = getRequiredBlockCount(size);
            this.blockChainHead = blockChainHead;

            reset();
        }

        private void reset() {
            position = 0L;
            withinBlockPosition = 0;

            blockIndex = blockChainHead;
            withinChainIndex = 0;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long newSize) throws IOException, IllegalArgumentException {
            checkBlockFileSize(newSize);

            if (newSize > size) {
                increaseSize(newSize);
            } else if (newSize < size) {
                decreaseSize(newSize);
            }
        }

        private void increaseSize(long newSize) throws IOException {
            resize(newSize, this::resizeWithIncrease);
        }

        private void resize(long newSize, IBlockFileSizeChanger sizeChanger) throws IOException {
            int newBlockChainLength = getRequiredBlockCount(newSize);
            sizeChanger.resize(newBlockChainLength);

            size = newSize;
            blockChainLength = newBlockChainLength;
        }

        private void resizeWithIncrease(int newBlockChainLength) throws IOException {
            if (size == 0L) {
                blockChainHead = allocate(newBlockChainLength);
                blockIndex = blockChainHead;
            } else {
                int additionalBlockCount = newBlockChainLength - blockChainLength;
                if (additionalBlockCount != 0) {
                    reallocate(blockIndex, getRemainingLength(), additionalBlockCount);
                }
            }
        }

        private int getRemainingLength() {
            return blockChainLength - withinChainIndex;
        }

        private void decreaseSize(long newSize) throws IOException {
            resize(newSize, this::resizeWithDecrease);

            if (size == 0L) {
                reset();
            } else if (position > size) {
                position = size;
                withinBlockPosition = (int) (position % BLOCK_SIZE);

                int newWithinChainIndex = (int) (position / BLOCK_SIZE);
                if (startNextBlock()) {
                    newWithinChainIndex--;
                }

                if (withinChainIndex != newWithinChainIndex) {
                    try {
                        blockIndex = getNextBlockIndex(blockChainHead, newWithinChainIndex);
                    } catch (Throwable t) {
                        reset();
                    }
                    withinChainIndex = newWithinChainIndex;
                }
            }
        }

        private void resizeWithDecrease(int newBlockChainLength) throws IOException {
            int releasedBlockCount = blockChainLength - newBlockChainLength;
            if (releasedBlockCount != 0) {
                if (Integer.compareUnsigned(withinChainIndex, newBlockChainLength) < 0) {
                    free(blockIndex, getRemainingLength(), (newBlockChainLength - withinChainIndex),
                            releasedBlockCount);
                } else {
                    blockChainHead = free(blockChainHead, blockChainLength, newBlockChainLength, blockIndex,
                            getRemainingLength(), releasedBlockCount);
                }
            }
        }

        private boolean startNextBlock() {
            return position != 0L && withinBlockPosition == 0;
        }

        public int getBlockChainHead() {
            return blockChainHead;
        }

        public int read(ByteBuffer destination) throws IOException, IllegalArgumentException {
            if (destination.isReadOnly()) {
                throw new IllegalArgumentException("Read-only buffer");// TODO
            }

            try {
                return processIOOperation(destination, NewBlockManager.this::readWithinBlock);
            } finally {
                destination.limit(destination.position());
            }
        }

        private int processIOOperation(ByteBuffer buffer,
                                       IBlockFileWithinBlockIOOperationProcessor withinBlockIOOperationProcessor)
                throws IOException {

            int totalProcessed = 0;
            int bufferRemaining = buffer.remaining();
            while (bufferRemaining != 0 && position != size) {
                int actualBlockIndex = blockIndex;
                int actualWithinChainIndex = withinChainIndex;
                if (startNextBlock()) {
                    actualBlockIndex = getNextBlockIndex(actualBlockIndex);
                    actualWithinChainIndex++;
                }

                int withinBlockRemaining = BLOCK_SIZE - withinBlockPosition;
                int toProcess = withinBlockRemaining < bufferRemaining ? withinBlockRemaining : bufferRemaining;
                buffer.limit(buffer.position() + toProcess);
                withinBlockIOOperationProcessor.process(actualBlockIndex, withinBlockPosition, buffer);

                totalProcessed += toProcess;
                bufferRemaining -= toProcess;

                position += toProcess;

                if (withinBlockRemaining == toProcess) {
                    withinBlockPosition = 0;
                } else {
                    withinBlockPosition += toProcess;
                }

                blockIndex = actualBlockIndex;
                withinChainIndex = actualWithinChainIndex;
            }

            return totalProcessed;
        }

        public int write(ByteBuffer source) throws IOException {
            long newPosition = position + source.remaining();
            if (newPosition > size) {
                increaseSize(newPosition);
            }

            int sourceLimit = source.limit();
            try {
                return processIOOperation(source, NewBlockManager.this::writeWithinBlock);
            } finally {
                source.limit(sourceLimit);
            }
        }
    }

    private final FileChannel channel;

    private int freeBlockCount;
    private int freeBlockChainHead;
    private final ByteBuffer freeBlockData;

    private final ByteBuffer nextBlockIndex;

    private final long blockTableOffset;

    private NewBlockManager(FileChannel channel, int freeBlockCount, int freeBlockChainHead, ByteBuffer nextBlockIndex,
                            long blockTableOffset) {

        this.channel = channel;

        this.freeBlockCount = freeBlockCount;
        this.freeBlockChainHead = freeBlockChainHead;
        this.freeBlockData = ByteBuffer.allocateDirect(FREE_BLOCK_DATA_SIZE);

        this.nextBlockIndex = nextBlockIndex;

        this.blockTableOffset = blockTableOffset;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    private static int getRequiredBlockCount(long size) {
        return (int) getRequiredBlockCountLong(size);
    }

    private static long getRequiredBlockCountLong(long size) {
        return (size + BLOCK_SIZE_MINUS_ONE) >> BLOCK_SIZE_EXPONENT;
    }

    private static void checkBlockFileSize(long size) throws IllegalArgumentException {
        if (size < 0L) {
            throw new IllegalArgumentException("Bad block file size");// TODO
        }
    }

    private int allocate(int requiredBlockCount) throws IOException {
        if (Integer.compareUnsigned(freeBlockCount, requiredBlockCount) < 0) {
            throw new FileFileSystemException("Not enough free blocks");// TODO
        }

        int newFreeBlockCount = freeBlockCount - requiredBlockCount;
        int newFreeBlockChainHead = NULL_BLOCK_INDEX;
        int allocatedBlockChainTail = NULL_BLOCK_INDEX;
        boolean haveMoreFreeBlocks = newFreeBlockCount != 0;
        if (haveMoreFreeBlocks) {
            allocatedBlockChainTail = getNextBlockIndex(freeBlockChainHead, (requiredBlockCount - 1));
            newFreeBlockChainHead = getNextBlockIndex(allocatedBlockChainTail);
        }

        freeBlockData.putInt(newFreeBlockCount);
        freeBlockData.putInt(newFreeBlockChainHead);
        flipBufferAndWrite(FREE_BLOCK_DATA_POSITION, freeBlockData, "Write free block data error");// TODO

        int allocatedBlockChainHead = freeBlockChainHead;

        freeBlockCount = newFreeBlockCount;
        freeBlockChainHead = newFreeBlockChainHead;

        if (haveMoreFreeBlocks) {
            nextBlockIndex.putInt(NULL_BLOCK_INDEX);
            flipBufferAndWrite(getNextBlockIndexPosition(allocatedBlockChainTail), nextBlockIndex,
                    "Write next block index error");// TODO
        }

        return allocatedBlockChainHead;
    }

    private int getNextBlockIndex(int blockIndex) throws IOException {
        return getNextBlockIndex(blockIndex, 1);
    }

    private int getNextBlockIndex(int blockIndex, int moveCount) throws IOException {
        for (int i = 0; Integer.compareUnsigned(i, moveCount) < 0; i++) {
            readAndFlipBuffer(getNextBlockIndexPosition(blockIndex), nextBlockIndex,
                    Messages.NEXT_BLOCK_INDEX_READ_ERROR);
            blockIndex = nextBlockIndex.getInt();
            nextBlockIndex.clear();

            if (blockIndex == NULL_BLOCK_INDEX) {
                throw new FileFileSystemException("Unexpected end of block chain");// TODO
            }
        }

        return blockIndex;
    }

    private void readAndFlipBuffer(long position, ByteBuffer destination, String errorMessage) throws IOException {
        read(position, destination, errorMessage);

        destination.flip();
    }

    private void read(long position, ByteBuffer destination, String errorMessage) throws IOException {
        int destinationRemaining = destination.remaining();
        if (channel.read(destination, position) != destinationRemaining) {
            throw new FileFileSystemException(errorMessage);
        }
    }

    private static long getNextBlockIndexPosition(int blockIndex) {
        return FIXED_SIZE_DATA_SIZE + Integer.toUnsignedLong(blockIndex) * BLOCK_INDEX_SIZE;
    }

    private void flipBufferAndWrite(long position, ByteBuffer source, String errorMessage) throws IOException {
        source.flip();

        write(position, source, errorMessage);

        source.clear();
    }

    private void write(long position, ByteBuffer source, String errorMessage) throws IOException {
        int sourceRemaining = source.remaining();
        if (channel.write(source, position) != sourceRemaining) {
            throw new FileFileSystemException(errorMessage);
        }
    }

    private void reallocate(int blockIndex, int remainingLength, int additionalBlockCount) throws IOException {
        // TODO
    }

    private void free(int blockIndex, int remainingLength, int newRemainingLength, int releasedBlockCount)
            throws IOException {

        free(blockIndex, remainingLength, newRemainingLength, blockIndex, remainingLength, releasedBlockCount);
    }

    private int free(int blockChainHead, int blockChainLength, int newBlockChainLength, int blockIndex,
                     int remainingLength, int releasedBlockCount) throws IOException {

        boolean delete = (newBlockChainLength == 0);
        // TODO

        return delete ? NULL_BLOCK_INDEX : blockChainHead;
    }

    private void readWithinBlock(int blockIndex, int withinBlockPosition, ByteBuffer destination) throws IOException {
        // TODO
    }

    private void writeWithinBlock(int blockIndex, int withinBlockPosition, ByteBuffer source) throws IOException {
        // TODO
    }

    public BlockFile createBlockFile() {
        return new BlockFile();
    }

    public BlockFile createBlockFile(long size) throws IOException {
        checkBlockFileSize(size);

        BlockFile file = createBlockFile();
        file.setSize(size);

        return file;
    }

    public BlockFile openBlockFile(long size, int blockChainHead) throws IOException {
        return openBlockFile(size, blockChainHead, false);
    }

    public BlockFile openBlockFile(long size, int blockChainHead, boolean skipCheckBlockChainHead) throws IOException {
        checkBlockFileSize(size);

        // TODO

        return new BlockFile(size, blockChainHead);
    }
}
