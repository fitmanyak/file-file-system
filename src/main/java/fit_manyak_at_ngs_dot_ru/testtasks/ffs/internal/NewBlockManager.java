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

    public class BlockFile {
        private long size;
        private int blockChainLength;
        private int blockChainHead;

        private long position;
        private int inBlockPosition;
        private int blockIndex;
        private int inChainIndex;

        private final ByteBuffer buffer;
        private boolean bufferValid;

        private BlockFile(long size, int blockChainHead) {
            this.size = size;
            this.blockChainLength = getRequiredBlockCount(size);
            this.blockChainHead = blockChainHead;

            resetPositionAndBlockIndex();

            this.buffer = ByteBuffer.allocate(BLOCK_SIZE);
            this.bufferValid = false;
        }

        private void resetPositionAndBlockIndex() {
            resetPosition();
            resetBlockIndex();
        }

        private void resetPosition() {
            position = 0L;
            inBlockPosition = 0;
        }

        private void resetBlockIndex() {
            blockIndex = blockChainHead;
            inChainIndex = 0;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long newSize) throws IOException, IllegalArgumentException {
            if (newSize < 0) {
                throw new IllegalArgumentException("Bad size");// TODO
            }

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
            return blockChainLength - inChainIndex;
        }

        private void decreaseSize(long newSize) throws IOException {
            resize(newSize, this::resizeWithDecrease);

            if (size == 0L) {
                reset();
            }
            else if (position > size) {
                position = size;
                inBlockPosition = (int) (position % BLOCK_SIZE);
                invalidateBuffer();

                if (Integer.compareUnsigned(inChainIndex, blockChainLength) >= 0) {
                    int newInChainIndex = blockChainLength - 1;
                    try {
                        blockIndex = getNextBlockIndex(blockChainHead, newInChainIndex);
                    }
                    catch (Throwable t) {
                        resetPositionAndBlockIndex();
                    }
                    inChainIndex = newInChainIndex;
                }
            }
        }

        private void resizeWithDecrease(int newBlockChainLength) throws IOException {
            int releasedBlockCount = blockChainLength - newBlockChainLength;
            if (releasedBlockCount != 0) {
                if (Integer.compareUnsigned(inChainIndex, newBlockChainLength) < 0) {
                    free(blockIndex, getRemainingLength(), (newBlockChainLength - inChainIndex), releasedBlockCount);
                } else {
                    blockChainHead = free(blockChainHead, blockChainLength, newBlockChainLength, blockIndex,
                            getRemainingLength(), releasedBlockCount);
                }
            }
        }

        private void reset() {
            resetPositionAndBlockIndex();
            invalidateBuffer();
        }

        private void invalidateBuffer() {
            buffer.clear();
            bufferValid = false;
        }

        public int getBlockChainHead() {
            return blockChainHead;
        }

        public int read(ByteBuffer destination) throws IOException, IllegalArgumentException {
            if (destination.isReadOnly()) {
                throw new IllegalArgumentException("Read-only buffer");// TODO
            }

            int totalRead = 0;
            int destinationRemaining = destination.remaining();
            while (destinationRemaining != 0 && position != size) {
                readToBuffer();

                int inBlockRemaining = BLOCK_SIZE - inBlockPosition;
                if (inBlockRemaining > destinationRemaining) {
                    destination.put(buffer.array(), inBlockPosition, destinationRemaining);

                    position += destinationRemaining;
                    inBlockPosition += destinationRemaining;
                    totalRead += destinationRemaining;
                    destinationRemaining = 0;

                    buffer.position(inBlockPosition);
                } else {
                    destination.put(buffer);

                    position += inBlockRemaining;
                    inBlockPosition = 0;
                    totalRead += inBlockRemaining;
                    destinationRemaining -= inBlockRemaining;

                    invalidateBuffer();
                }
            }

            return totalRead;
        }

        private void readToBuffer() throws IOException {
            if (!bufferValid) {
                boolean blockStart = (inBlockPosition == 0);
                int actualBlockIndex = blockIndex;
                if (position != 0L && blockStart) {
                    actualBlockIndex = getNextBlockIndex(actualBlockIndex);
                }

                try {
                    readBlock(actualBlockIndex, buffer);
                } catch (Throwable t) {
                    invalidateBuffer();

                    throw t;
                }
                blockIndex = actualBlockIndex;
                inChainIndex++;
                bufferValid = true;

                if (!blockStart) {
                    buffer.position(inBlockPosition);
                }
            }
        }

        public int write(ByteBuffer source) throws IOException {
            int sourceRemaining = source.remaining();
            if (sourceRemaining == 0) {
                return 0;
            }

            long newPosition = position + sourceRemaining;
            if (newPosition > size) {
                increaseSize(newPosition);
            }

            int totalWrite = 0;
            // TODO

            return totalWrite;
        }
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

    private static int getRequiredBlockCount(long size) {
        return (int) getRequiredBlockCountLong(size);
    }

    private static long getRequiredBlockCountLong(long size) {
        return (size + BLOCK_SIZE_MINUS_ONE) >> BLOCK_SIZE_EXPONENT;
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

    private void readBlock(int blockIndex, ByteBuffer buffer) throws IOException {
        // TODO
    }

    public BlockFile createBlockFile(long size, int blockChainHead) {
        return new BlockFile(size, blockChainHead);
    }
}
