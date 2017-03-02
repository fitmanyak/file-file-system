package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class NewBlockManager {
    private static final int BLOCK_SIZE = 512;
    private static final int BLOCK_SIZE_MINUS_ONE = BLOCK_SIZE - 1;
    private static final int BLOCK_SIZE_EXPONENT = 9;

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

    private static int getRequiredBlockCount(long size) {
        return (int) getRequiredBlockCountLong(size);
    }

    private static long getRequiredBlockCountLong(long size) {
        return (size + BLOCK_SIZE_MINUS_ONE) >> BLOCK_SIZE_EXPONENT;
    }

    private static void checkBlockFileSize(long size) throws IllegalArgumentException {
        if (size < 0) {
            throw new IllegalArgumentException("Bad block file size");// TODO
        }
    }

    private int allocate(int requiredBlockCount) throws IOException {
        return 0;// TODO
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

    private int getNextBlockIndex(int blockIndex) throws IOException {
        return getNextBlockIndex(blockIndex, 1);
    }

    private int getNextBlockIndex(int blockIndex, int moveCount) throws IOException {
        for (int i = 0; Integer.compareUnsigned(i, moveCount) < 0; i++) {
            // TODO
        }

        return blockIndex;
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
