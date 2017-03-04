package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.ICommonFile;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.Creator;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IIOAction;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IIOOperation;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOUtilities;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public abstract class NewDirectoryEntry {
    private static final int FLAGS_SIZE = 4;
    protected static final int FILE_FLAGS = 0;
    protected static final int DIRECTORY_FLAGS = 1;

    private static final int CONTENT_DATA_SIZE = BlockManager.CONTENT_SIZE_SIZE + BlockManager.BLOCK_INDEX_SIZE;
    private static final long CONTENT_DATA_POSITION = FLAGS_SIZE;

    private static final int NAME_SIZE_SIZE = 2;
    private static final int NAME_MAXIMAL_SIZE = (1 << (8 * NAME_SIZE_SIZE)) - 1;
    private static final short NULL_NAME_SIZE = 0;

    private static final int FIXED_SIZE_DATA_SIZE = FLAGS_SIZE + CONTENT_DATA_SIZE + NAME_SIZE_SIZE;

    private class Content implements ICommonFile {
        private final IBlockFile delegate;

        private Content(IBlockFile delegate) {
            this.delegate = delegate;
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public void setSize(long newSize) throws IOException, IllegalArgumentException {
            performUpdatedContentDataIOAction(() -> delegate.setSize(newSize));
        }

        private void performUpdatedContentDataIOAction(IIOAction action) throws IOException, IllegalArgumentException {
            action.perform();

            updateContentData();
        }

        @Override
        public long getPosition() {
            return delegate.getPosition();
        }

        @Override
        public void setPosition(long newPosition) throws IOException, IllegalArgumentException {
            delegate.setPosition(newPosition);
        }

        @Override
        public void reset() {
            delegate.reset();
        }

        @Override
        public void clear() throws IOException, IllegalArgumentException {
            performUpdatedContentDataIOAction(delegate::clear);
        }

        @Override
        public int read(ByteBuffer destination) throws IOException, IllegalArgumentException {
            return delegate.read(destination);
        }

        @Override
        public int read(long newPosition, ByteBuffer destination) throws IOException, IllegalArgumentException {
            return delegate.read(newPosition, destination);
        }

        @Override
        public int write(ByteBuffer source) throws IOException, IllegalArgumentException {
            return performUpdatedContentDataIOOperation(source, delegate::write);
        }

        private int performUpdatedContentDataIOOperation(ByteBuffer buffer, IIOOperation operation)
                throws IOException, IllegalArgumentException {

            int result = operation.perform(buffer);

            updateContentData();

            return result;
        }

        @Override
        public int write(long newPosition, ByteBuffer source) throws IOException, IllegalArgumentException {
            return performUpdatedContentDataIOOperation(source, src -> delegate.write(newPosition, src));
        }

        private int getBlockChainHead() {
            return delegate.getBlockChainHead();
        }
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    protected interface ICreator<T extends NewDirectoryEntry> {
        public T create(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                        BlockManager blockManager);
    }

    private final IBlockFile entry;

    private Content content;
    private long contentSize;
    private int contentBlockChainHead;
    private ByteBuffer contentData;

    private final String name;

    private final BlockManager blockManager;

    protected NewDirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                                BlockManager blockManager) {

        this.entry = entry;

        this.contentSize = contentSize;
        this.contentBlockChainHead = contentBlockChainHead;
        this.contentData = ByteBuffer.allocateDirect(CONTENT_DATA_SIZE);

        this.name = name;

        this.blockManager = blockManager;
    }

    private void updateContentData() throws FileFileSystemException {
        long newContentSize = content.getSize();
        int newContentBlockChainHead = content.getBlockChainHead();
        boolean contentBlockChainHeadChanged = newContentBlockChainHead != contentBlockChainHead;
        if (newContentSize != contentSize || contentBlockChainHeadChanged) {
            contentData.putLong(newContentSize);

            if (contentBlockChainHeadChanged) {
                contentData.putInt(newContentBlockChainHead);
            }

            IOUtilities.flipBufferAndWrite(contentData, src -> entry.write(CONTENT_DATA_POSITION, src),
                    "Directory entry content data write error");// TODO

            contentSize = newContentSize;
            contentBlockChainHead = newContentBlockChainHead;
        }
    }

    public int getBlockChainHead() {
        return entry.getBlockChainHead();
    }

    public ICommonFile getContent() throws FileFileSystemException {
        if (content == null) {
            IOUtilities.performIOAction(
                    () -> content = new Content(blockManager.openBlockFile(contentSize, contentBlockChainHead)),
                    "Directory entry content block file open error");// TODO
        }

        return content;
    }

    public String getName() {
        return name;
    }

    protected static <T extends NewDirectoryEntry> T create(int flags, String name, BlockManager blockManager,
                                                            ICreator<T> creator)
            throws IOException, IllegalArgumentException {

        byte[] nameBytes = getNameBytes(name);
        int entrySize = getEntrySize(nameBytes.length);

        return Creator.createWithCloseableArgument(
                () -> Creator.create(() -> blockManager.createBlockFile(entrySize), "Directory entry create error"),
                entry -> create(flags, name, nameBytes, entrySize, entry, blockManager, creator));// TODO
    }

    private static byte[] getNameBytes(String name) throws IllegalArgumentException {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        if (nameBytes.length > NAME_MAXIMAL_SIZE) {
            throw new IllegalArgumentException("Too long directory entry name");// TODO
        }

        return nameBytes;
    }

    private static int getEntrySize(int nameSize) {
        return FIXED_SIZE_DATA_SIZE + nameSize;
    }

    private static <T extends NewDirectoryEntry> T create(int flags, String name, byte[] nameBytes, int entrySize,
                                                          IBlockFile entry, BlockManager blockManager,
                                                          ICreator<T> creator)
            throws FileFileSystemException {

        ByteBuffer entryData = ByteBuffer.allocateDirect(entrySize);
        fillNewEntryData(entryData, flags, nameBytes);
        IOUtilities.flipBufferAndWrite(entryData, src -> entry.write(src), "Directory entry data write error");// TODO

        return creator.create(entry, 0L, BlockManager.NULL_BLOCK_INDEX, name, blockManager);
    }

    protected static void fillNewEntryData(ByteBuffer entryData, int flags, byte[] nameBytes) {
        entryData.putInt(flags);
        entryData.putLong(0L);
        entryData.putInt(BlockManager.NULL_BLOCK_INDEX);
        entryData.putShort((short) nameBytes.length);
        entryData.put(nameBytes);
    }

    public static NewDirectoryEntry open(int blockChainHead, BlockManager blockManager)
            throws IOException, IllegalArgumentException {

        IBlockFile entry = blockManager.openBlockFile(FIXED_SIZE_DATA_SIZE, blockChainHead);
        ByteBuffer entryFixedSizeData = createReadAndFlipBuffer(FIXED_SIZE_DATA_SIZE, entry,
                "Directory entry fixed-size data read error");// TODO

        int flags = entryFixedSizeData.getInt();
        boolean isFile = flags == FILE_FLAGS;
        boolean isDirectory = flags == DIRECTORY_FLAGS;
        if (!isFile && !isDirectory) {
            throw new FileFileSystemException(Messages.BAD_DIRECTORY_ENTRY_FLAGS_ERROR);
        }

        long contentSize = entryFixedSizeData.getLong();
        int contentBlockChainHead = entryFixedSizeData.getInt();

        int nameSize = Short.toUnsignedInt(entryFixedSizeData.getShort());
        if (nameSize == 0) {
            throw new FileFileSystemException(Messages.BAD_DIRECTORY_ENTRY_NAME_ERROR);
        }

        entry.setCalculatedSize(getEntrySize(nameSize));

        ByteBuffer entryName = createReadAndFlipBuffer(nameSize, entry, "Directory entry name read error");// TODO
        byte[] nameBytes = new byte[nameSize];
        entryName.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);

        return isFile ? new FileDirectoryEntry(entry, contentSize, contentBlockChainHead, name, blockManager) :
                new DirectoryDirectoryEntry(entry, contentSize, contentBlockChainHead, name, blockManager);
    }

    private static ByteBuffer createReadAndFlipBuffer(int size, IBlockFile file, String errorMessage)
            throws FileFileSystemException {
        ByteBuffer destination = ByteBuffer.allocateDirect(size);
        IOUtilities.readAndFlipBuffer(destination, file::read, errorMessage);

        return destination;
    }
}
