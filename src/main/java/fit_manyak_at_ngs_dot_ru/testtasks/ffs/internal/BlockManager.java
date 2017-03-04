package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IIOOperation;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.Utilities;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
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

    private static final int MINIMAL_BLOCK_COUNT = 4;
    private static final long MINIMAL_SIZE = MINIMAL_BLOCK_COUNT * BLOCK_SIZE;
    private static final long MAXIMAL_SIZE = ((1L << (8L * BLOCK_INDEX_SIZE)) - 1L) * BLOCK_SIZE;

    private static final int CONTENT_SIZE_SIZE_EXPONENT_SIZE = 1;
    private static final byte CONTENT_SIZE_SIZE_EXPONENT = 0;
    public static final int CONTENT_SIZE_SIZE = 8;

    private static final int SIGNATURE_AND_GEOMETRY_SIZE =
            SIGNATURE_SIZE + BLOCK_SIZE_RATIO_SIZE + BLOCK_INDEX_SIZE_EXPONENT_SIZE + CONTENT_SIZE_SIZE_EXPONENT_SIZE +
                    BLOCK_INDEX_SIZE;

    private static final int FREE_BLOCK_DATA_SIZE = BLOCK_INDEX_SIZE + BLOCK_INDEX_SIZE;
    private static final long FREE_BLOCK_DATA_POSITION = SIGNATURE_AND_GEOMETRY_SIZE;

    private static final int FIXED_SIZE_DATA_SIZE = SIGNATURE_AND_GEOMETRY_SIZE + FREE_BLOCK_DATA_SIZE;

    public static final int NULL_BLOCK_INDEX = 0;

    private static final int ROOT_DIRECTORY_ENTRY_BLOCK_INDEX = 0;
    private static final int ROOT_DIRECTORY_ENTRY_BLOCK_COUNT = 1;

    private static final int FIRST_BLOCK_INITIAL_NEXT_BLOCK_INDEX = ROOT_DIRECTORY_ENTRY_BLOCK_COUNT + 1;

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

    private class BlockFile implements IBlockFile {
        private long size;
        private int blockChainLength;
        private int blockChainHead;

        private long position;
        private int withinBlockPosition;

        private int blockIndex;
        private int withinChainIndex;

        private BlockFile() {
            this(0L, 0, NULL_BLOCK_INDEX);
        }

        private BlockFile(long size, int blockChainLength, int blockChainHead) {
            this.size = size;
            this.blockChainLength = blockChainLength;
            this.blockChainHead = blockChainHead;

            reset();
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
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

        @Override
        public long getPosition() {
            return position;
        }

        @Override
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

        @Override
        public void reset() {
            position = 0L;
            withinBlockPosition = 0;

            blockIndex = blockChainHead;
            withinChainIndex = 0;
        }

        @Override
        public void clear() throws IOException, IllegalArgumentException {
            setSize(0L);
        }

        @Override
        public int read(ByteBuffer destination) throws IOException, IllegalArgumentException {
            if (destination.isReadOnly()) {
                throw new IllegalArgumentException("Read-only buffer");// TODO
            }

            try {
                return performIOOperation(destination, BlockManager.this::readWithinBlock);
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

        @Override
        public int read(long newPosition, ByteBuffer destination) throws IOException, IllegalArgumentException {
            return performIOOperationAtPosition(newPosition, destination, this::read);
        }

        private int performIOOperationAtPosition(long newPosition, ByteBuffer buffer, IIOOperation operation)
                throws IOException, IllegalArgumentException {

            setPosition(newPosition);

            return operation.perform(buffer);
        }

        @Override
        public int write(ByteBuffer source) throws IOException, IllegalArgumentException {
            long newPosition = position + source.remaining();
            if (newPosition > size) {
                increaseSize(newPosition);
            }

            int sourceLimit = source.limit();
            try {
                return performIOOperation(source, BlockManager.this::writeWithinBlock);
            } finally {
                source.limit(sourceLimit);
            }
        }

        @Override
        public int write(long newPosition, ByteBuffer source) throws IOException, IllegalArgumentException {
            return performIOOperationAtPosition(newPosition, source, this::write);
        }

        @Override
        public void close() throws IOException, IllegalArgumentException {
            clear();
        }

        @Override
        public int getBlockChainHead() {
            return blockChainHead;
        }

        @Override
        public void setCalculatedSize(long newSize) {
            if (size != newSize) {
                size = newSize;
                blockChainLength = getRequiredBlockCount(size);

                if (position > size) {
                    position = size;
                    withinBlockPosition = (int) position;
                }
            }
        }
    }

    private final FileChannel channel;

    private final int blockCount;

    private int freeBlockCount;
    private int freeBlockChainHead;
    private final ByteBuffer freeBlockData;

    private final ByteBuffer nextBlockIndex;

    private final long blockTableOffset;

    private BlockManager(FileChannel channel, int blockCount, int freeBlockCount, int freeBlockChainHead,
                         ByteBuffer nextBlockIndex) {

        this.channel = channel;

        this.blockCount = blockCount;

        this.freeBlockCount = freeBlockCount;
        this.freeBlockChainHead = freeBlockChainHead;
        this.freeBlockData = ByteBuffer.allocateDirect(FREE_BLOCK_DATA_SIZE);

        this.nextBlockIndex = nextBlockIndex;

        this.blockTableOffset = getNextBlockIndexPosition(blockCount);
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
        checkSize(size, 0L, "Bad block file size");// TODO
    }

    private static void checkSize(long size, long minimalSize, String errorMessage) throws IllegalArgumentException {
        if (size < minimalSize || size > MAXIMAL_SIZE) {
            throw new IllegalArgumentException(errorMessage);
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

        Utilities.readAndFlipBuffer(destination, dst -> read(position, dst, errorMessage));
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

        Utilities.flipBufferAndWrite(source, src -> write(position, src, errorMessage));
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

        read(getAbsolutePosition(blockIndex, withinBlockPosition), destination, Messages.BLOCK_READ_ERROR);
    }

    private long getAbsolutePosition(int blockIndex, int withinBlockPosition) {
        return blockTableOffset + Integer.toUnsignedLong(blockIndex) * BLOCK_SIZE + withinBlockPosition;
    }

    private void writeWithinBlock(int blockIndex, int withinBlockPosition, ByteBuffer source)
            throws IOException, IllegalArgumentException {

        write(getAbsolutePosition(blockIndex, withinBlockPosition), source, "Block write error");// TODO
    }

    public IBlockFile createBlockFile(long size) throws IOException, IllegalArgumentException {
        IBlockFile file = new BlockFile();
        file.setSize(size);

        return file;
    }

    public IBlockFile openBlockFile(long size, int blockChainHead) throws IOException, IllegalArgumentException {
        return openBlockFile(size, blockChainHead, false);
    }

    public IBlockFile openBlockFile(long size, int blockChainHead, boolean skipCheckBlockChainHead)
            throws IOException, IllegalArgumentException {

        checkBlockFileSize(size);

        int blockChainLength = getRequiredBlockCount(size);
        checkBlockChainHead(blockCount, blockChainLength, blockChainHead, "Bad block file block chain length",
                "Bad block file block chain head", skipCheckBlockChainHead);// TODO

        return new BlockFile(size, blockChainLength, blockChainHead);
    }

    private static void checkBlockChainHead(int blockCount, int blockChainLength, int blockChainHead,
                                            String badBlockChainLengthErrorMessage,
                                            String badBlockChainHeadErrorMessage) throws FileFileSystemException {

        checkBlockChainHead(blockCount, blockChainLength, blockChainHead, badBlockChainLengthErrorMessage,
                badBlockChainHeadErrorMessage, false);
    }

    private static void checkBlockChainHead(int blockCount, int blockChainLength, int blockChainHead,
                                            String badBlockChainLengthErrorMessage,
                                            String badBlockChainHeadErrorMessage, boolean skipCheckBlockChainHead)
            throws FileFileSystemException {

        if (Integer.compareUnsigned(blockCount, blockChainLength) < 0) {
            throw new FileFileSystemException(badBlockChainLengthErrorMessage);
        }

        if (!skipCheckBlockChainHead) {
            boolean blockChainIsEmpty = blockChainLength == 0;
            boolean blockChainHeadIsNULL = blockChainHead == NULL_BLOCK_INDEX;
            if ((blockChainIsEmpty && !blockChainHeadIsNULL) || (!blockChainIsEmpty && blockChainHeadIsNULL)) {
                throw new FileFileSystemException(badBlockChainHeadErrorMessage);
            }
        }
    }

    public static void format(Path path, long size, Consumer<ByteBuffer> rootDirectoryEntryFormatter)
            throws IOException, IllegalArgumentException {

        checkSize(size, MINIMAL_SIZE,
                String.format(Messages.BAD_SIZE_FOR_FORMAT_ERROR, size, MINIMAL_SIZE, MAXIMAL_SIZE));

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
                flipBufferAndWrite(fixedSizeData, channel, "Fixed-size data write error");// TODO

                ByteBuffer nextBlockIndex = ByteBuffer.allocateDirect(BLOCK_INDEX_SIZE);
                writeNextBlockIndex(NULL_BLOCK_INDEX, nextBlockIndex, channel);

                for (int i = FIRST_BLOCK_INITIAL_NEXT_BLOCK_INDEX; i < blockCount; i++) {
                    writeNextBlockIndex(i, nextBlockIndex, channel);
                }

                writeNextBlockIndex(NULL_BLOCK_INDEX, nextBlockIndex, channel);

                ByteBuffer rootDirectoryEntry = ByteBuffer.allocateDirect(BLOCK_SIZE);
                rootDirectoryEntryFormatter.accept(rootDirectoryEntry);
                flipBufferAndWrite(rootDirectoryEntry, channel, "Root directory entry write error");// TODO
            }
        }
    }

    private static long getTotalSize(int blockCount) {
        return getTotalSize(Integer.toUnsignedLong(blockCount));
    }

    private static long getTotalSize(long blockCountLong) {
        return FIXED_SIZE_DATA_SIZE + blockCountLong * BLOCK_SIZE_PLUS_BLOCK_INDEX_SIZE;
    }

    private static void flipBufferAndWrite(ByteBuffer source, FileChannel channel, String errorMessage)
            throws IOException, IllegalArgumentException {

        Utilities.flipBufferAndWrite(source, src -> performIOOperation(src, channel::write, errorMessage));
    }

    private static void writeNextBlockIndex(int index, ByteBuffer buffer, FileChannel channel)
            throws IOException, IllegalArgumentException {

        buffer.putInt(index);
        flipBufferAndWrite(buffer, channel, "Next block index write error");// TODO
    }

    public static BlockManager mount(Path path) throws IOException, IllegalArgumentException {
        return Utilities.createWithCloseableArgument(
                () -> FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE),
                BlockManager::mount);
    }

    private static BlockManager mount(FileChannel channel) throws IOException {
        ByteBuffer fixedSizeData =
                createReadAndFlipBuffer(FIXED_SIZE_DATA_SIZE, channel, Messages.FIXED_SIZE_DATA_READ_ERROR);
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
        int freeBlockChainHead = fixedSizeData.getInt();
        checkBlockChainHead(blockCount, freeBlockCount, freeBlockChainHead, Messages.BAD_FREE_BLOCK_COUNT_ERROR,
                Messages.BAD_FREE_BLOCK_CHAIN_HEAD_ERROR);

        if (channel.size() != getTotalSize(blockCount)) {
            throw new FileFileSystemException(Messages.BAD_SIZE_ERROR);
        }

        ByteBuffer nextBlockIndex =
                createReadAndFlipBuffer(BLOCK_INDEX_SIZE, channel, Messages.NEXT_BLOCK_INDEX_READ_ERROR);
        if (nextBlockIndex.getInt() != NULL_BLOCK_INDEX) {
            throw new FileFileSystemException(Messages.BAD_ROOT_DIRECTORY_ENTRY_BLOCK_NEXT_BLOCK_INDEX_ERROR);
        }
        nextBlockIndex.clear();

        return new BlockManager(channel, blockCount, freeBlockCount, freeBlockChainHead, nextBlockIndex);
    }

    private static ByteBuffer createReadAndFlipBuffer(int size, FileChannel channel, String errorMessage)
            throws IOException, IllegalArgumentException {

        ByteBuffer destination = ByteBuffer.allocateDirect(size);
        Utilities.readAndFlipBuffer(destination, dst -> performIOOperation(dst, channel::read, errorMessage));

        return destination;
    }
}
