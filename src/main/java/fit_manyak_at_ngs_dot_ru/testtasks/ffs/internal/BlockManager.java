package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ICommonOperationWithArgument;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOUtilities;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOperationWithArgument;

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

public class BlockManager implements IBlockManager {
    private static final int SIGNATURE_SIZE = 2;
    @SuppressWarnings("WeakerAccess")
    public static final short SIGNATURE_VALUE = (short) 0xFFF5;

    private static final int BLOCK_SIZE_RATIO_SIZE = 1;
    @SuppressWarnings("WeakerAccess")
    public static final byte BLOCK_SIZE_RATIO_VALUE = 0;
    @SuppressWarnings("WeakerAccess")
    public static final int BLOCK_SIZE = 512;
    private static final long BLOCK_SIZE_MINUS_ONE = BLOCK_SIZE - 1L;
    private static final long BLOCK_SIZE_EXPONENT = 9L;

    private static final int BLOCK_INDEX_SIZE_EXPONENT_SIZE = 1;
    @SuppressWarnings("WeakerAccess")
    public static final byte BLOCK_INDEX_SIZE_EXPONENT_VALUE = 0;

    private static final int CONTENT_SIZE_SIZE_EXPONENT_SIZE = 1;
    @SuppressWarnings("WeakerAccess")
    public static final byte CONTENT_SIZE_SIZE_EXPONENT_VALUE = 0;

    private static final int SIGNATURE_AND_GEOMETRY_SIZE =
            SIGNATURE_SIZE + BLOCK_SIZE_RATIO_SIZE + BLOCK_INDEX_SIZE_EXPONENT_SIZE + CONTENT_SIZE_SIZE_EXPONENT_SIZE +
                    BLOCK_INDEX_SIZE;

    private static final int FREE_BLOCK_DATA_SIZE = BLOCK_INDEX_SIZE + BLOCK_INDEX_SIZE;
    private static final long FREE_BLOCK_DATA_POSITION = SIGNATURE_AND_GEOMETRY_SIZE;

    @SuppressWarnings("WeakerAccess")
    public static final int FIXED_SIZE_DATA_SIZE = SIGNATURE_AND_GEOMETRY_SIZE + FREE_BLOCK_DATA_SIZE;

    private static final int BLOCK_SIZE_PLUS_BLOCK_INDEX_SIZE = BLOCK_SIZE + BLOCK_INDEX_SIZE;

    private static final int MINIMAL_BLOCK_COUNT = 4;
    private static final long MAXIMAL_BLOCK_COUNT = (1L << (8L * BLOCK_INDEX_SIZE)) - 1L;
    @SuppressWarnings("WeakerAccess")
    public static final long MINIMAL_SIZE =
            FIXED_SIZE_DATA_SIZE + MINIMAL_BLOCK_COUNT * BLOCK_SIZE_PLUS_BLOCK_INDEX_SIZE;
    @SuppressWarnings("WeakerAccess")
    public static final long MAXIMAL_SIZE =
            FIXED_SIZE_DATA_SIZE + MAXIMAL_BLOCK_COUNT * BLOCK_SIZE_PLUS_BLOCK_INDEX_SIZE;

    private static final long MAXIMAL_BLOCK_FILE_SIZE = MAXIMAL_BLOCK_COUNT * BLOCK_SIZE;

    @SuppressWarnings("WeakerAccess")
    public static final int ROOT_DIRECTORY_ENTRY_BLOCK_COUNT = 1;

    @SuppressWarnings("WeakerAccess")
    public static final int FIRST_BLOCK_INITIAL_NEXT_BLOCK_INDEX = ROOT_DIRECTORY_ENTRY_BLOCK_COUNT + 1;

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface IBlockFileSizeChanger {
        public void resize(int withinChainIndex, int blockChainLength, int newBlockChainLength)
                throws FileFileSystemException;
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface IBlockFileWithinBlockOperation {
        public void perform(int blockIndex, int withinBlockPosition, ByteBuffer buffer) throws FileFileSystemException;
    }

    private class BlockFile implements IBlockFile {
        private long size;
        private int blockChainHead;

        private final Position position;

        private BlockFile() {
            this(0L, NULL_BLOCK_INDEX);
        }

        private BlockFile(long size, int blockChainHead) {
            this.size = size;
            this.blockChainHead = blockChainHead;

            this.position = new Position(blockChainHead);
        }

        @Override
        public boolean isEmpty() {
            return size == 0L;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public void setSize(long newSize) throws FileFileSystemException {
            checkBlockFileSize(newSize);

            if (newSize > size) {
                increaseSize(newSize);
            } else if (newSize < size) {
                decreaseSize(newSize);
            }
        }

        private void increaseSize(long newSize) throws FileFileSystemException {
            resize(newSize, this::resizeWithIncrease);
        }

        private void resize(long newSize, IBlockFileSizeChanger sizeChanger) throws FileFileSystemException {
            sizeChanger.resize(getWithinChainIndex(), getRequiredBlockCount(size), getRequiredBlockCount(newSize));

            size = newSize;
        }

        private int getWithinChainIndex() {
            int withinChainIndex = (int) (position.getPosition() >> BLOCK_SIZE_EXPONENT);
            if (startNextBlock()) {
                withinChainIndex--;
            }

            return withinChainIndex;
        }

        private boolean startNextBlock() {
            return startNextBlock(getWithinBlockPosition());
        }

        private boolean startNextBlock(int withinBlockPosition) {
            return position.getPosition() != 0L && withinBlockPosition == 0;
        }

        private int getWithinBlockPosition() {
            return (int) (position.getPosition() & BLOCK_SIZE_MINUS_ONE);
        }

        private void resizeWithIncrease(int withinChainIndex, int blockChainLength, int newBlockChainLength)
                throws FileFileSystemException {

            if (isEmpty()) {
                blockChainHead = allocate(newBlockChainLength);
                position.setBlockIndex(blockChainHead);
            } else {
                int additionalBlockCount = newBlockChainLength - blockChainLength;
                if (additionalBlockCount != 0) {
                    reallocate(position.getBlockIndex(), (blockChainLength - withinChainIndex), additionalBlockCount);
                }
            }
        }

        private void decreaseSize(long newSize) throws FileFileSystemException {
            resize(newSize, this::resizeWithDecrease);

            if (isEmpty()) {
                reset();
            } else if (position.getPosition() > size) {
                setPosition(size);
            }
        }

        private void resizeWithDecrease(int withinChainIndex, int blockChainLength, int newBlockChainLength)
                throws FileFileSystemException {

            int releasedBlockCount = blockChainLength - newBlockChainLength;
            if (releasedBlockCount != 0) {
                if (newBlockChainLength == 0) {
                    deallocate(blockChainHead, blockChainLength, position.getBlockIndex(),
                            (blockChainLength - withinChainIndex));
                    blockChainHead = NULL_BLOCK_INDEX;
                } else if (Integer.compareUnsigned(withinChainIndex, newBlockChainLength) < 0) {
                    deallocate(position.getBlockIndex(), (newBlockChainLength - withinChainIndex), releasedBlockCount);
                } else {
                    deallocate(blockChainHead, newBlockChainLength, position.getBlockIndex(),
                            (blockChainLength - withinChainIndex), releasedBlockCount,
                            (newBlockChainLength == withinChainIndex));
                }
            }
        }

        @Override
        public long getPosition() {
            return position.getPosition();
        }

        @Override
        public void setPosition(long newPosition) throws FileFileSystemException {
            if (newPosition < 0L) {
                throw new IllegalArgumentException(Messages.BAD_BLOCK_FILE_POSITION_ERROR);
            }

            if (newPosition > size) {
                throw new FileFileSystemException(Messages.BIG_BLOCK_FILE_POSITION_ERROR);
            }

            if (position.getPosition() == newPosition) {
                return;
            }

            int withinChainIndex = getWithinChainIndex();

            position.setPosition(newPosition);

            int newWithinChainIndex = getWithinChainIndex();

            try {
                position.setBlockIndex(Integer.compareUnsigned(newWithinChainIndex, withinChainIndex) < 0 ?
                        getNextBlockIndex(blockChainHead, newWithinChainIndex) :
                        getNextBlockIndex(position.getBlockIndex(), (newWithinChainIndex - withinChainIndex)));
            } catch (Throwable t) {
                reset();
            }
        }

        @Override
        public void reset() {
            position.reset(blockChainHead);
        }

        @Override
        public void savePosition(Position saved) {
            saved.set(position);
        }

        @Override
        public void setPosition(Position newPosition) {
            position.set(newPosition);
        }

        @Override
        public int write(Position newPosition, ByteBuffer source) throws FileFileSystemException {
            setPosition(newPosition);

            return write(source);
        }

        @Override
        public int read(ByteBuffer destination) throws FileFileSystemException {
            if (destination.isReadOnly()) {
                throw new IllegalArgumentException(Messages.READ_ONLY_BUFFER_ERROR);
            }

            try {
                return performOperation(destination, BlockManager.this::readWithinBlock);
            } finally {
                destination.limit(destination.position());
            }
        }

        private int performOperation(ByteBuffer buffer, IBlockFileWithinBlockOperation operation)
                throws FileFileSystemException {

            int totalProcessed = 0;
            int bufferRemaining = buffer.remaining();
            while (bufferRemaining != 0 && position.getPosition() != size) {
                int actualBlockIndex = position.getBlockIndex();
                int withinBlockPosition = getWithinBlockPosition();
                if (startNextBlock(withinBlockPosition)) {
                    actualBlockIndex = getNextBlockIndex(actualBlockIndex);
                }

                int withinBlockRemaining = BLOCK_SIZE - withinBlockPosition;
                int toProcess = withinBlockRemaining < bufferRemaining ? withinBlockRemaining : bufferRemaining;
                buffer.limit(buffer.position() + toProcess);
                operation.perform(actualBlockIndex, withinBlockPosition, buffer);

                totalProcessed += toProcess;
                bufferRemaining -= toProcess;

                position.setPosition(position.getPosition() + toProcess);
                position.setBlockIndex(actualBlockIndex);
            }

            return totalProcessed;
        }

        @Override
        public int read(long newPosition, ByteBuffer destination) throws FileFileSystemException {
            return performOperationAtPosition(newPosition, destination, this::read);
        }

        private int performOperationAtPosition(long newPosition, ByteBuffer buffer,
                                               IOperationWithArgument<ByteBuffer> operation)
                throws FileFileSystemException {

            setPosition(newPosition);

            return operation.perform(buffer);
        }

        @Override
        public int write(ByteBuffer source) throws FileFileSystemException {
            long newPosition = position.getPosition() + source.remaining();
            if (newPosition > size) {
                increaseSize(newPosition);
            }

            int sourceLimit = source.limit();
            try {
                return performOperation(source, BlockManager.this::writeWithinBlock);
            } finally {
                source.limit(sourceLimit);
            }
        }

        @Override
        public int write(long newPosition, ByteBuffer source) throws FileFileSystemException {
            return performOperationAtPosition(newPosition, source, this::write);
        }

        @Override
        public void close() throws FileFileSystemException {
            remove();
        }

        @Override
        public void remove() throws FileFileSystemException {
            setSize(0L);
        }

        @Override
        public int getBlockChainHead() {
            return blockChainHead;
        }

        @Override
        public void setCalculatedSize(long newSize) {
            size = newSize;
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
    public void close() throws FileFileSystemException {
        ErrorHandlingHelper.performAction(channel::close, Messages.FILE_CHANNEL_CLOSE_ERROR);
    }

    @Override
    public long getTotalSpace() {
        return getSize(blockCount);
    }

    @SuppressWarnings("WeakerAccess")
    public static long getSize(int blockCount) {
        return Integer.toUnsignedLong(blockCount) * BLOCK_SIZE;
    }

    @Override
    public long getFreeSpace() {
        return getSize(freeBlockCount);
    }

    private static void checkBlockFileSize(long size) {
        checkSize(size, 0L, MAXIMAL_BLOCK_FILE_SIZE, Messages.BAD_BLOCK_FILE_SIZE_ERROR);
    }

    private static void checkSize(long size, long minimalSize, long maximalSize, String errorMessageFormat) {
        if (size < minimalSize || size > maximalSize) {
            throw new IllegalArgumentException(String.format(errorMessageFormat, size, minimalSize, maximalSize));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static int getRequiredBlockCount(long size) {
        return (int) ((size + BLOCK_SIZE_MINUS_ONE) >> BLOCK_SIZE_EXPONENT);
    }

    private int allocate(int requiredBlockCount) throws FileFileSystemException {
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
            throw new FileFileSystemException(Messages.NOT_ENOUGH_FREE_BLOCKS_ERROR);
        }
    }

    private int getBlockChainTail(int blockChainHead, int blockChainLength) throws FileFileSystemException {
        return getNextBlockIndex(blockChainHead, (blockChainLength - 1));
    }

    private int getNextBlockIndex(int blockIndex) throws FileFileSystemException {
        return getNextBlockIndex(blockIndex, 1);
    }

    private int getNextBlockIndex(int blockIndex, int moveCount) throws FileFileSystemException {
        for (int i = 0; Integer.compareUnsigned(i, moveCount) < 0; i++) {
            readAndFlipBuffer(getNextBlockIndexPosition(blockIndex), nextBlockIndex,
                    Messages.NEXT_BLOCK_INDEX_READ_ERROR);
            blockIndex = nextBlockIndex.getInt();
            nextBlockIndex.clear();

            if (blockIndex == NULL_BLOCK_INDEX) {
                throw new FileFileSystemException(Messages.UNEXPECTED_END_BLOCK_CHAIN_ERROR);
            }
        }

        return blockIndex;
    }

    private void readAndFlipBuffer(long position, ByteBuffer destination,
                                   @SuppressWarnings("SameParameterValue") String errorMessage)
            throws FileFileSystemException {

        IOUtilities.readAndFlipBuffer(destination, dst -> read(position, dst, errorMessage));
    }

    private void read(long position, ByteBuffer destination, String errorMessage) throws FileFileSystemException {
        performOperation(destination, dst -> channel.read(dst, position), errorMessage);
    }

    private static void performOperation(ByteBuffer buffer, ICommonOperationWithArgument<ByteBuffer> operation,
                                         String errorMessage) throws FileFileSystemException {

        boolean partiallyPerformed;
        try {
            int bufferRemaining = buffer.remaining();
            partiallyPerformed = operation.perform(buffer) != bufferRemaining;
        } catch (Throwable t) {
            throw new FileFileSystemException(errorMessage, t);
        }

        if (partiallyPerformed) {
            throw new FileFileSystemException(errorMessage);
        }
    }

    private static long getNextBlockIndexPosition(int blockIndex) {
        return FIXED_SIZE_DATA_SIZE + Integer.toUnsignedLong(blockIndex) * BLOCK_INDEX_SIZE;
    }

    private void writeFreeBlockData(int newFreeBlockCount, int newFreeBlockChainHead) throws FileFileSystemException {
        freeBlockData.putInt(newFreeBlockCount);
        freeBlockData.putInt(newFreeBlockChainHead);
        flipBufferAndWrite(FREE_BLOCK_DATA_POSITION, freeBlockData, Messages.FREE_BLOCK_DATA_WRITE_ERROR);

        freeBlockCount = newFreeBlockCount;
        freeBlockChainHead = newFreeBlockChainHead;
    }

    private void flipBufferAndWrite(long position, ByteBuffer source, String errorMessage)
            throws FileFileSystemException {

        IOUtilities.flipBufferAndWrite(source, src -> write(position, src, errorMessage));
    }

    private void write(long position, ByteBuffer source, String errorMessage) throws FileFileSystemException {
        performOperation(source, src -> channel.write(src, position), errorMessage);
    }

    private void writeNextBlockIndex(int blockIndex, int newNextBlockIndex) throws FileFileSystemException {
        nextBlockIndex.putInt(newNextBlockIndex);
        flipBufferAndWrite(getNextBlockIndexPosition(blockIndex), nextBlockIndex,
                Messages.NEXT_BLOCK_INDEX_WRITE_ERROR);
    }

    private void reallocate(int blockIndex, int remainingLength, int additionalBlockCount)
            throws FileFileSystemException {

        checkEnoughFreeBlocks(additionalBlockCount);

        int blockChainTail = getBlockChainTail(blockIndex, remainingLength);
        int additionalBlockChainHead = allocate(additionalBlockCount);
        writeNextBlockIndex(blockChainTail, additionalBlockChainHead);
    }

    private void deallocate(int blockChainHead, int blockChainLength, int blockIndex, int remainingLength)
            throws FileFileSystemException {

        checkReleasedBlockCount(blockChainLength);

        releaseBlockChain(blockChainHead, getBlockChainTail(blockIndex, remainingLength), blockChainLength);
    }

    private void checkReleasedBlockCount(int releasedBlockCount) throws FileFileSystemException {
        if (Integer.compareUnsigned(blockCount, releasedBlockCount) < 0) {
            throw new FileFileSystemException(Messages.TOO_MANY_RELEASED_BLOCKS_ERROR);
        }

        if (Integer.compareUnsigned((blockCount - releasedBlockCount), freeBlockCount) < 0) {
            throw new FileFileSystemException(Messages.BAD_FREE_BLOCK_COUNT_ERROR);
        }
    }

    private void releaseBlockChain(int blockChainHead, int blockChainTail, int blockChainLength)
            throws FileFileSystemException {

        writeNextBlockIndex(blockChainTail, freeBlockChainHead);
        writeFreeBlockData((freeBlockCount + blockChainLength), blockChainHead);
    }

    private void deallocate(int blockIndex, int newRemainingLength, int releasedBlockCount)
            throws FileFileSystemException {

        checkReleasedBlockCount(releasedBlockCount);

        int newBlockChainTail = getBlockChainTail(blockIndex, newRemainingLength);
        int releasedBlockChainHead = getNextBlockIndex(newBlockChainTail);
        int releasedBlockChainTail = getBlockChainTail(releasedBlockChainHead, releasedBlockCount);
        breakAndReleaseBlockChain(newBlockChainTail, releasedBlockChainHead, releasedBlockChainTail,
                releasedBlockCount);
    }

    private void breakAndReleaseBlockChain(int newBlockChainTail, int releasedBlockChainHead,
                                           int releasedBlockChainTail, int releasedBlockCount)
            throws FileFileSystemException {

        writeNextBlockIndex(newBlockChainTail, NULL_BLOCK_INDEX);
        releaseBlockChain(releasedBlockChainHead, releasedBlockChainTail, releasedBlockCount);
    }

    private void deallocate(int blockChainHead, int newBlockChainLength, int blockIndex, int remainingLength,
                            int releasedBlockCount, boolean isBlockIndexReleasedBlockChainHead)
            throws FileFileSystemException {

        checkReleasedBlockCount(releasedBlockCount);

        int newBlockChainTail = getBlockChainTail(blockChainHead, newBlockChainLength);
        int releasedBlockChainHead =
                isBlockIndexReleasedBlockChainHead ? blockIndex : getNextBlockIndex(newBlockChainTail);
        int releasedBlockChainTail = getBlockChainTail(blockIndex, remainingLength);
        breakAndReleaseBlockChain(newBlockChainTail, releasedBlockChainHead, releasedBlockChainTail,
                releasedBlockCount);
    }

    private void readWithinBlock(int blockIndex, int withinBlockPosition, ByteBuffer destination)
            throws FileFileSystemException {

        read(getAbsolutePosition(blockIndex, withinBlockPosition), destination, Messages.BLOCK_READ_ERROR);
    }

    private long getAbsolutePosition(int blockIndex, int withinBlockPosition) {
        return blockTableOffset + getSize(blockIndex) + withinBlockPosition;
    }

    private void writeWithinBlock(int blockIndex, int withinBlockPosition, ByteBuffer source)
            throws FileFileSystemException {

        write(getAbsolutePosition(blockIndex, withinBlockPosition), source, Messages.BLOCK_WRITE_ERROR);
    }

    @Override
    public IBlockFile createBlockFile(long size) throws FileFileSystemException {
        IBlockFile file = new BlockFile();
        file.setSize(size);

        return file;
    }

    @Override
    public IBlockFile openBlockFile(long size, int blockChainHead) throws FileFileSystemException {
        return openBlockFile(size, blockChainHead, false);
    }

    @Override
    public IBlockFile openBlockFile(long size, int blockChainHead, boolean skipCheckBlockChainHead)
            throws FileFileSystemException {

        checkBlockFileSize(size);

        checkBlockChainHead(blockCount, getRequiredBlockCount(size), blockChainHead,
                Messages.BAD_BLOCK_FILE_BLOCK_CHAIN_LENGTH_ERROR, Messages.BAD_BLOCK_FILE_BLOCK_CHAIN_HEAD_ERROR,
                skipCheckBlockChainHead);

        return new BlockFile(size, blockChainHead);
    }

    private static void checkBlockChainHead(int blockCount, int blockChainLength, int blockChainHead,
                                            @SuppressWarnings("SameParameterValue") String badBlockChainLengthErrorMessage,
                                            @SuppressWarnings("SameParameterValue") String badBlockChainHeadErrorMessage)
            throws FileFileSystemException {

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
            throws FileFileSystemException {

        checkSize(size, MINIMAL_SIZE, MAXIMAL_SIZE, Messages.BAD_SIZE_FOR_FORMAT_ERROR);

        ErrorHandlingHelper.performActionWithCloseableArgument(() -> new RandomAccessFile(path.toString(), "rw"),
                Messages.FILE_OPEN_ERROR, file -> format(file, size, rootDirectoryEntryFormatter),
                Messages.FILE_CLOSE_ERROR);
    }

    private static void format(RandomAccessFile file, long size,
                               Consumer<ByteBuffer> rootDirectoryEntryFormatter)
            throws FileFileSystemException {

        long blockCountLong = getBlockCountLong(size);
        ErrorHandlingHelper
                .performAction(() -> file.setLength(getTotalSize(blockCountLong)), Messages.FILE_SIZE_SET_ERROR);

        ErrorHandlingHelper.performActionWithCloseableArgument(file::getChannel, Messages.FILE_CHANNEL_GET_ERROR,
                channel -> format(channel, blockCountLong, rootDirectoryEntryFormatter),
                Messages.FILE_CHANNEL_CLOSE_ERROR);
    }

    @SuppressWarnings("WeakerAccess")
    public static int getBlockCount(long size) {
        return (int) getBlockCountLong(size);
    }

    private static long getBlockCountLong(long size) {
        return (size - FIXED_SIZE_DATA_SIZE) / BLOCK_SIZE_PLUS_BLOCK_INDEX_SIZE;
    }

    private static void format(FileChannel channel, long blockCountLong,
                               Consumer<ByteBuffer> rootDirectoryEntryFormatter) throws FileFileSystemException {

        int blockCount = (int) blockCountLong;
        ByteBuffer fixedSizeData = ByteBuffer.allocateDirect(FIXED_SIZE_DATA_SIZE);
        fixedSizeData.putShort(SIGNATURE_VALUE);
        fixedSizeData.put(BLOCK_SIZE_RATIO_VALUE);
        fixedSizeData.put(BLOCK_INDEX_SIZE_EXPONENT_VALUE);
        fixedSizeData.put(CONTENT_SIZE_SIZE_EXPONENT_VALUE);
        fixedSizeData.putInt(blockCount);
        fixedSizeData.putInt(blockCount - ROOT_DIRECTORY_ENTRY_BLOCK_COUNT);
        fixedSizeData.putInt(ROOT_DIRECTORY_ENTRY_BLOCK_COUNT);
        flipBufferAndWrite(fixedSizeData, channel, Messages.FIXED_SIZE_DATA_WRITE_ERROR);

        ByteBuffer nextBlockIndex = ByteBuffer.allocateDirect(BLOCK_INDEX_SIZE);
        writeNextBlockIndex(NULL_BLOCK_INDEX, nextBlockIndex, channel);

        for (int i = FIRST_BLOCK_INITIAL_NEXT_BLOCK_INDEX; Integer.compareUnsigned(i, blockCount) < 0; i++) {
            writeNextBlockIndex(i, nextBlockIndex, channel);
        }

        writeNextBlockIndex(NULL_BLOCK_INDEX, nextBlockIndex, channel);

        ByteBuffer rootDirectoryEntry = ByteBuffer.allocateDirect(BLOCK_SIZE);
        rootDirectoryEntryFormatter.accept(rootDirectoryEntry);
        flipBufferAndWrite(rootDirectoryEntry, channel, Messages.ROOT_DIRECTORY_ENTRY_WRITE_ERROR);
    }

    private static long getTotalSize(int blockCount) {
        return getTotalSize(Integer.toUnsignedLong(blockCount));
    }

    @SuppressWarnings("WeakerAccess")
    public static long getTotalSize(long blockCountLong) {
        return FIXED_SIZE_DATA_SIZE + blockCountLong * BLOCK_SIZE_PLUS_BLOCK_INDEX_SIZE;
    }

    private static void flipBufferAndWrite(ByteBuffer source, FileChannel channel, String errorMessage)
            throws FileFileSystemException {

        IOUtilities.flipBufferAndWrite(source, src -> performOperation(src, channel::write, errorMessage));
    }

    private static void writeNextBlockIndex(int index, ByteBuffer buffer, FileChannel channel)
            throws FileFileSystemException {

        buffer.putInt(index);
        flipBufferAndWrite(buffer, channel, Messages.NEXT_BLOCK_INDEX_WRITE_ERROR);
    }

    public static IBlockManager mount(Path path) throws FileFileSystemException {
        return ErrorHandlingHelper.getWithCloseableArgument(
                () -> FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE),
                Messages.FILE_CHANNEL_OPEN_ERROR, BlockManager::mount);
    }

    private static IBlockManager mount(FileChannel channel) throws FileFileSystemException {
        ByteBuffer fixedSizeData =
                createReadAndFlipBuffer(FIXED_SIZE_DATA_SIZE, channel, Messages.FIXED_SIZE_DATA_READ_ERROR);
        if (fixedSizeData.getShort() != SIGNATURE_VALUE) {
            throw new FileFileSystemException(Messages.BAD_SIGNATURE_ERROR);
        }

        if (fixedSizeData.get() != BLOCK_SIZE_RATIO_VALUE) {
            throw new FileFileSystemException(Messages.BAD_BLOCK_SIZE_RATIO_ERROR);
        }

        if (fixedSizeData.get() != BLOCK_INDEX_SIZE_EXPONENT_VALUE) {
            throw new FileFileSystemException(Messages.BAD_BLOCK_INDEX_SIZE_EXPONENT_ERROR);
        }

        if (fixedSizeData.get() != CONTENT_SIZE_SIZE_EXPONENT_VALUE) {
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

        long size = ErrorHandlingHelper.get(channel::size, Messages.FILE_CHANNEL_SIZE_GET_ERROR);
        if (getTotalSize(blockCount) != size) {
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
            throws FileFileSystemException {

        return IOUtilities.createReadAndFlipBuffer(size, dst -> performOperation(dst, channel::read, errorMessage));
    }
}
