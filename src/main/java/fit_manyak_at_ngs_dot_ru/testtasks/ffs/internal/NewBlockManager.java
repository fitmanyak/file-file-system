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
        public void resize(int newBlockChainLength) throws IOException, IllegalArgumentException;
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface IBlockFileWithinBlockIOOperation {
        public void perform(int blockIndex, int withinBlockPosition, ByteBuffer buffer)
                throws IOException, IllegalArgumentException;
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface IIOOperation {
        public int perform(ByteBuffer buffer) throws IOException, IllegalArgumentException;
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

        private void increaseSize(long newSize) throws IOException, IllegalArgumentException {
            resize(newSize, this::resizeWithIncrease);
        }

        private void resize(long newSize, IBlockFileSizeChanger sizeChanger)
                throws IOException, IllegalArgumentException {

            int newBlockChainLength = getRequiredBlockCount(newSize);
            sizeChanger.resize(newBlockChainLength);

            size = newSize;
            blockChainLength = newBlockChainLength;
        }

        private void resizeWithIncrease(int newBlockChainLength) throws IOException, IllegalArgumentException {
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

        private void decreaseSize(long newSize) throws IOException, IllegalArgumentException {
            resize(newSize, this::resizeWithDecrease);

            if (size == 0L) {
                reset();
            } else if (position > size) {
                setPosition(size);
            }
        }

        private void resizeWithDecrease(int newBlockChainLength) throws IOException, IllegalArgumentException {
            int releasedBlockCount = blockChainLength - newBlockChainLength;
            if (releasedBlockCount != 0) {
                if (newBlockChainLength == 0) {
                    deallocate(blockChainHead, blockChainLength, blockIndex, getRemainingLength());
                    blockChainHead = NULL_BLOCK_INDEX;
                } else if (Integer.compareUnsigned(withinChainIndex, newBlockChainLength) < 0) {
                    deallocate(blockIndex, (newBlockChainLength - withinChainIndex), releasedBlockCount);
                } else {
                    deallocate(blockChainHead, newBlockChainLength, blockIndex, getRemainingLength(),
                            releasedBlockCount, (newBlockChainLength == withinChainIndex));
                }
            }
        }

        public int getBlockChainHead() {
            return blockChainHead;
        }

        public long getPosition() {
            return position;
        }

        public void setPosition(long newPosition) throws IOException, IllegalArgumentException {
            if (newPosition < 0L) {
                throw new IllegalArgumentException("Bad block file position");// TODO
            }

            if (newPosition > size) {
                throw new FileFileSystemException("Big block file position");// TODO
            }

            if (newPosition == position) {
                return;
            }

            position = newPosition;
            withinBlockPosition = (int) (position % BLOCK_SIZE);

            int newWithinChainIndex = (int) (position / BLOCK_SIZE);
            if (startNextBlock()) {
                newWithinChainIndex--;
            }

            try {
                blockIndex = Integer.compareUnsigned(newWithinChainIndex, withinChainIndex) < 0 ?
                        getNextBlockIndex(blockChainHead, newWithinChainIndex) :
                        getNextBlockIndex(blockIndex, (newWithinChainIndex - withinChainIndex));
            } catch (Throwable t) {
                reset();
            }
            withinChainIndex = newWithinChainIndex;
        }

        private boolean startNextBlock() {
            return position != 0L && withinBlockPosition == 0;
        }

        public int read(ByteBuffer destination) throws IOException, IllegalArgumentException {
            if (destination.isReadOnly()) {
                throw new IllegalArgumentException("Read-only buffer");// TODO
            }

            try {
                return performIOOperation(destination, NewBlockManager.this::readWithinBlock);
            } finally {
                destination.limit(destination.position());
            }
        }

        private int performIOOperation(ByteBuffer buffer, IBlockFileWithinBlockIOOperation operation)
                throws IOException, IllegalArgumentException {

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
                operation.perform(actualBlockIndex, withinBlockPosition, buffer);

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

        public int read(long newPosition, ByteBuffer destination) throws IOException, IllegalArgumentException {
            return performIOOperationAtPosition(newPosition, destination, this::read);
        }

        private int performIOOperationAtPosition(long newPosition, ByteBuffer buffer, IIOOperation operation)
                throws IOException, IllegalArgumentException {

            setPosition(newPosition);

            return operation.perform(buffer);
        }

        public int write(ByteBuffer source) throws IOException, IllegalArgumentException {
            long newPosition = position + source.remaining();
            if (newPosition > size) {
                increaseSize(newPosition);
            }

            int sourceLimit = source.limit();
            try {
                return performIOOperation(source, NewBlockManager.this::writeWithinBlock);
            } finally {
                source.limit(sourceLimit);
            }
        }

        public int write(long newPosition, ByteBuffer source) throws IOException {
            return performIOOperationAtPosition(newPosition, source, this::write);
        }
    }


    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface IPositionedIOOperation {
        public void perform(long position, ByteBuffer buffer, String errorMessage)
                throws IOException, IllegalArgumentException;
    }

    private final FileChannel channel;

    private final int blockCount;

    private int freeBlockCount;
    private int freeBlockChainHead;
    private final ByteBuffer freeBlockData;

    private final ByteBuffer nextBlockIndex;

    private final long blockTableOffset;

    private NewBlockManager(FileChannel channel, int blockCount, int freeBlockCount, int freeBlockChainHead,
                            ByteBuffer nextBlockIndex, long blockTableOffset) {

        this.channel = channel;

        this.blockCount = blockCount;

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

    private int allocate(int requiredBlockCount) throws IOException, IllegalArgumentException {
        checkEnoughFreeBlocks(requiredBlockCount);

        int newFreeBlockCount = freeBlockCount - requiredBlockCount;
        int newFreeBlockChainHead = NULL_BLOCK_INDEX;
        int allocatedBlockChainTail = freeBlockChainHead;
        boolean hasMoreFreeBlocks = newFreeBlockCount != 0;
        if (hasMoreFreeBlocks) {
            allocatedBlockChainTail = getBlockChainTail(freeBlockChainHead, requiredBlockCount);
            newFreeBlockChainHead = getNextBlockIndex(allocatedBlockChainTail);
        }

        int allocatedBlockChainHead = freeBlockChainHead;
        writeFreeBlockData(newFreeBlockCount, newFreeBlockChainHead);

        if (hasMoreFreeBlocks) {
            writeNextBlockIndex(allocatedBlockChainTail, NULL_BLOCK_INDEX);
        }

        return allocatedBlockChainHead;
    }

    private void checkEnoughFreeBlocks(int requiredBlockCount) throws FileFileSystemException {
        if (Integer.compareUnsigned(freeBlockCount, requiredBlockCount) < 0) {
            throw new FileFileSystemException("Not enough free blocks");// TODO
        }
    }

    private int getBlockChainTail(int blockChainHead, int blockChainLength)
            throws IOException, IllegalArgumentException {

        return getNextBlockIndex(blockChainHead, (blockChainLength - 1));
    }

    private int getNextBlockIndex(int blockIndex) throws IOException, IllegalArgumentException {
        return getNextBlockIndex(blockIndex, 1);
    }

    private int getNextBlockIndex(int blockIndex, int moveCount) throws IOException, IllegalArgumentException {
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

    private void readAndFlipBuffer(long position, ByteBuffer destination, String errorMessage)
            throws IOException, IllegalArgumentException {

        read(position, destination, errorMessage);

        destination.flip();
    }

    private void read(long position, ByteBuffer destination, String errorMessage)
            throws IOException, IllegalArgumentException {

        performIOOperation(destination, dst -> channel.read(dst, position), errorMessage);
    }

    private static void performIOOperation(ByteBuffer buffer, IIOOperation operation, String errorMessage)
            throws IOException, IllegalArgumentException {

        int bufferRemaining = buffer.remaining();
        if (operation.perform(buffer) != bufferRemaining) {
            throw new FileFileSystemException(errorMessage);
        }
    }

    private static long getNextBlockIndexPosition(int blockIndex) {
        return FIXED_SIZE_DATA_SIZE + Integer.toUnsignedLong(blockIndex) * BLOCK_INDEX_SIZE;
    }

    private void writeFreeBlockData(int newFreeBlockCount, int newFreeBlockChainHead)
            throws IOException, IllegalArgumentException {

        freeBlockData.putInt(newFreeBlockCount);
        freeBlockData.putInt(newFreeBlockChainHead);
        flipBufferAndWrite(FREE_BLOCK_DATA_POSITION, freeBlockData, "Write free block data error");// TODO

        freeBlockCount = newFreeBlockCount;
        freeBlockChainHead = newFreeBlockChainHead;
    }

    private void flipBufferAndWrite(long position, ByteBuffer source, String errorMessage)
            throws IOException, IllegalArgumentException {

        source.flip();

        write(position, source, errorMessage);

        source.clear();
    }

    private void write(long position, ByteBuffer source, String errorMessage)
            throws IOException, IllegalArgumentException {

        performIOOperation(source, src -> channel.write(src, position), errorMessage);
    }

    private void writeNextBlockIndex(int blockIndex, int newNextBlockIndex)
            throws IOException, IllegalArgumentException {

        nextBlockIndex.putInt(newNextBlockIndex);
        flipBufferAndWrite(getNextBlockIndexPosition(blockIndex), nextBlockIndex,
                "Write next block index error");// TODO
    }

    private void reallocate(int blockIndex, int remainingLength, int additionalBlockCount)
            throws IOException, IllegalArgumentException {

        checkEnoughFreeBlocks(additionalBlockCount);

        int blockChainTail = getBlockChainTail(blockIndex, remainingLength);
        int additionalBlockChainHead = allocate(additionalBlockCount);
        writeNextBlockIndex(blockChainTail, additionalBlockChainHead);
    }

    private void deallocate(int blockChainHead, int blockChainLength, int blockIndex, int remainingLength)
            throws IOException, IllegalArgumentException {

        checkReleasedBlockCount(blockChainLength);

        releaseBlockChain(blockChainHead, getBlockChainTail(blockIndex, remainingLength), blockChainLength);
    }

    private void checkReleasedBlockCount(int releasedBlockCount) throws FileFileSystemException {
        if (Integer.compareUnsigned(blockCount, releasedBlockCount) < 0) {
            throw new FileFileSystemException("Too many released blocks");// TODO
        }

        if (freeBlockCount != blockCount - releasedBlockCount) {
            throw new FileFileSystemException(Messages.BAD_FREE_BLOCK_COUNT_ERROR);// TODO
        }
    }

    private void releaseBlockChain(int blockChainHead, int blockChainTail, int blockChainLength)
            throws IOException, IllegalArgumentException {

        writeNextBlockIndex(blockChainTail, freeBlockChainHead);
        writeFreeBlockData((freeBlockCount + blockChainLength), blockChainHead);
    }

    private void deallocate(int blockIndex, int newRemainingLength, int releasedBlockCount)
            throws IOException, IllegalArgumentException {

        checkReleasedBlockCount(releasedBlockCount);

        int newBlockChainTail = getBlockChainTail(blockIndex, newRemainingLength);
        int releasedBlockChainHead = getNextBlockIndex(newBlockChainTail);
        int releasedBlockChainTail = getBlockChainTail(releasedBlockChainHead, releasedBlockCount);
        breakAndReleaseBlockChain(newBlockChainTail, releasedBlockChainHead, releasedBlockChainTail,
                releasedBlockCount);
    }

    private void breakAndReleaseBlockChain(int newBlockChainTail, int releasedBlockChainHead,
                                           int releasedBlockChainTail, int releasedBlockCount)
            throws IOException, IllegalArgumentException {

        writeNextBlockIndex(newBlockChainTail, NULL_BLOCK_INDEX);
        releaseBlockChain(releasedBlockChainHead, releasedBlockChainTail, releasedBlockCount);
    }

    private void deallocate(int blockChainHead, int newBlockChainLength, int blockIndex, int remainingLength,
                            int releasedBlockCount, boolean isBlockIndexReleasedBlockChainHead)
            throws IOException, IllegalArgumentException {

        checkReleasedBlockCount(releasedBlockCount);

        int newBlockChainTail = getBlockChainTail(blockChainHead, newBlockChainLength);
        int releasedBlockChainHead =
                isBlockIndexReleasedBlockChainHead ? blockIndex : getNextBlockIndex(newBlockChainTail);
        int releasedBlockChainTail = getBlockChainTail(blockIndex, remainingLength);
        breakAndReleaseBlockChain(newBlockChainTail, releasedBlockChainHead, releasedBlockChainTail,
                releasedBlockCount);
    }

    private void readWithinBlock(int blockIndex, int withinBlockPosition, ByteBuffer destination)
            throws IOException, IllegalArgumentException {

        performIOOperationWithinBlock(blockIndex, withinBlockPosition, destination, Messages.BLOCK_READ_ERROR,
                this::read);
    }

    private void performIOOperationWithinBlock(int blockIndex, int withinBlockPosition, ByteBuffer buffer,
                                               String errorMessage, IPositionedIOOperation operation)
            throws IOException, IllegalArgumentException {

        operation.perform((blockTableOffset + Integer.toUnsignedLong(blockIndex) * BLOCK_SIZE + withinBlockPosition),
                buffer, errorMessage);
    }

    private void writeWithinBlock(int blockIndex, int withinBlockPosition, ByteBuffer source)
            throws IOException, IllegalArgumentException {

        performIOOperationWithinBlock(blockIndex, withinBlockPosition, source, "Block write error", this::write);// TODO
    }

    public BlockFile createBlockFile() {
        return new BlockFile();
    }

    public BlockFile createBlockFile(long size) throws IOException, IllegalArgumentException {
        checkBlockFileSize(size);

        BlockFile file = createBlockFile();
        file.setSize(size);

        return file;
    }

    public BlockFile openBlockFile(long size, int blockChainHead) throws IOException, IllegalArgumentException {
        return openBlockFile(size, blockChainHead, false);
    }

    public BlockFile openBlockFile(long size, int blockChainHead, boolean skipCheckBlockChainHead)
            throws IOException, IllegalArgumentException {

        checkBlockFileSize(size);

        if (!skipCheckBlockChainHead) {
            checkBlockChainHead((size == 0L), blockChainHead, "Bad block file block chain head");// TODO
        }

        return new BlockFile(size, blockChainHead);
    }

    private static void checkBlockChainHead(boolean isEmpty, int blockChainHead, String errorMessage)
            throws FileFileSystemException {

        if ((isEmpty && blockChainHead != NULL_BLOCK_INDEX) || (!isEmpty && blockChainHead == NULL_BLOCK_INDEX)) {
            throw new FileFileSystemException(errorMessage);
        }
    }
}
