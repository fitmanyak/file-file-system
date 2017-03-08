package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IAction;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOUtilities;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOperationWithArgument;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public abstract class DirectoryEntry<T extends IInternalDirectoryItem> implements IDirectoryEntry<T> {
    private static final int FLAGS_SIZE = 4;
    static final int FILE_FLAGS_VALUE = 0;
    static final int DIRECTORY_FLAGS_VALUE = 1;

    private static final int CONTENT_DATA_SIZE = IBlockManager.CONTENT_SIZE_SIZE + IBlockManager.BLOCK_INDEX_SIZE;
    private static final long CONTENT_DATA_POSITION = FLAGS_SIZE;

    private static final int NAME_SIZE_SIZE = 2;
    private static final int NAME_MAXIMAL_SIZE = (1 << (8 * NAME_SIZE_SIZE)) - 1;

    private static final int WITHOUT_NAME_DATA_SIZE = FLAGS_SIZE + CONTENT_DATA_SIZE;
    private static final int FIXED_SIZE_DATA_SIZE = WITHOUT_NAME_DATA_SIZE + NAME_SIZE_SIZE;

    private static final long NAME_DATA_POSITION = WITHOUT_NAME_DATA_SIZE;

    private class Content implements IDirectFile {
        private final IBlockFile delegate;

        private Content(IBlockFile delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public void setSize(long newSize) throws FileFileSystemException {
            performUpdatedContentDataAction(() -> delegate.setSize(newSize));
        }

        private void performUpdatedContentDataAction(IAction action) throws FileFileSystemException {
            action.perform();

            updateContentData();
        }

        @Override
        public long getPosition() {
            return delegate.getPosition();
        }

        @Override
        public void setPosition(long newPosition) throws FileFileSystemException {
            delegate.setPosition(newPosition);
        }

        @Override
        public void reset() {
            delegate.reset();
        }

        @Override
        public void savePosition(Position saved) {
            delegate.savePosition(saved);
        }

        @Override
        public void setPosition(Position newPosition) {
            delegate.setPosition(newPosition);
        }

        @Override
        public int write(Position newPosition, ByteBuffer source) throws FileFileSystemException {
            return performUpdatedContentDataOperation(source, src -> delegate.write(newPosition, src));
        }

        private int performUpdatedContentDataOperation(ByteBuffer buffer, IOperationWithArgument<ByteBuffer> operation)
                throws FileFileSystemException {

            int result = operation.perform(buffer);

            updateContentData();

            return result;
        }

        @Override
        public int read(ByteBuffer destination) throws FileFileSystemException {
            return delegate.read(destination);
        }

        @Override
        public int read(long newPosition, ByteBuffer destination) throws FileFileSystemException {
            return delegate.read(newPosition, destination);
        }

        @Override
        public int write(ByteBuffer source) throws FileFileSystemException {
            return performUpdatedContentDataOperation(source, delegate::write);
        }

        @Override
        public int write(long newPosition, ByteBuffer source) throws FileFileSystemException {
            return performUpdatedContentDataOperation(source, src -> delegate.write(newPosition, src));
        }

        private int getBlockChainHead() {
            return delegate.getBlockChainHead();
        }

        private void remove() throws FileFileSystemException {
            delegate.remove();
        }
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    interface ICreator<T extends IDirectoryEntry<? extends IInternalDirectoryItem>> {
        public T create(IBlockFile entry, String name, IBlockManager blockManager);
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    interface IOpener<T extends IDirectoryEntry<? extends IInternalDirectoryItem>> {
        public T open(IBlockFile entry, boolean isDirectory, long contentSize, int contentBlockChainHead, int nameSize,
                      IBlockManager blockManager) throws FileFileSystemException;
    }

    private final IBlockFile entry;

    private Content content;
    private long contentSize;
    private int contentBlockChainHead;
    private final ByteBuffer contentData;

    private String name;

    private final IBlockManager blockManager;

    DirectoryEntry(IBlockFile entry, String name, IBlockManager blockManager) {
        this(entry, 0L, IBlockManager.NULL_BLOCK_INDEX, name, blockManager);
    }

    DirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
                   IBlockManager blockManager) {

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

            flipBufferAndWrite(CONTENT_DATA_POSITION, contentData, Messages.DIRECTORY_ENTRY_CONTENT_DATA_WRITE_ERROR);

            contentSize = newContentSize;
            contentBlockChainHead = newContentBlockChainHead;
        }
    }

    private void flipBufferAndWrite(long position, ByteBuffer source, String errorMessage)
            throws FileFileSystemException {

        IOUtilities.flipBufferAndWrite(source, src -> entry.write(position, src), errorMessage);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) throws FileFileSystemException {
        if (!name.equals(newName)) {
            checkNameNotEmpty(newName);

            byte[] newNameBytes = getNameBytes(newName);
            ErrorHandlingHelper.performAction(() -> changeName(newNameBytes), Messages.DIRECTORY_ENTRY_RENAME_ERROR);

            name = newName;
        }
    }

    public static void checkNameNotEmpty(String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException(Messages.EMPTY_DIRECTORY_ENTRY_NAME_ERROR);
        }
    }

    private static byte[] getNameBytes(String name) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        if (nameBytes.length > NAME_MAXIMAL_SIZE) {
            throw new IllegalArgumentException(Messages.TOO_LONG_DIRECTORY_ENTRY_NAME_ERROR);
        }

        return nameBytes;
    }

    private void changeName(byte[] newNameBytes) throws FileFileSystemException {
        int newEntrySize = getEntrySize(newNameBytes);
        entry.setSize(newEntrySize);

        int newNameDataSize = newEntrySize - WITHOUT_NAME_DATA_SIZE;
        ByteBuffer newNameData = ByteBuffer.allocateDirect(newNameDataSize);
        fillNewNameData(newNameData, newNameBytes);
        flipBufferAndWrite(NAME_DATA_POSITION, newNameData, Messages.DIRECTORY_ENTRY_NAME_DATA_WRITE_ERROR);
    }

    private static int getEntrySize(byte[] nameBytes) {
        return getEntrySize(nameBytes.length);
    }

    private static int getEntrySize(int nameSize) {
        return FIXED_SIZE_DATA_SIZE + nameSize;
    }

    private static void fillNewNameData(ByteBuffer newNameData, byte[] nameBytes) {
        newNameData.putShort((short) nameBytes.length);
        newNameData.put(nameBytes);
    }

    @Override
    public void remove() throws FileFileSystemException {
        ErrorHandlingHelper.performAction(this::performRemove, Messages.DIRECTORY_ENTRY_REMOVE_ERROR);
    }

    private void performRemove() throws FileFileSystemException {
        getContentInternal().remove();
        entry.remove();
    }

    private Content getContentInternal() throws FileFileSystemException {
        if (content == null) {
            content = ErrorHandlingHelper
                    .get(() -> new Content(blockManager.openBlockFile(contentSize, contentBlockChainHead)),
                            Messages.DIRECTORY_ENTRY_CONTENT_BLOCK_FILE_OPEN_ERROR);
        }

        return content;
    }

    @Override
    public int getBlockChainHead() {
        return entry.getBlockChainHead();
    }

    @Override
    public IDirectFile getContent() throws FileFileSystemException {
        return getContentInternal();
    }

    @Override
    public IBlockManager getBlockManager() {
        return blockManager;
    }

    static <T extends IDirectoryEntry<? extends IInternalDirectoryItem>> T createNamed(int flags, String name,
                                                                                       IBlockManager blockManager,
                                                                                       ICreator<T> creator)
            throws FileFileSystemException {

        checkNameNotEmpty(name);

        return create(flags, name, blockManager, creator);
    }

    private static <T extends IDirectoryEntry<? extends IInternalDirectoryItem>> T create(int flags, String name,
                                                                                          IBlockManager blockManager,
                                                                                          ICreator<T> creator)
            throws FileFileSystemException {

        byte[] nameBytes = getNameBytes(name);
        int entrySize = getEntrySize(nameBytes);

        return ErrorHandlingHelper.getWithCloseableArgument(() -> blockManager.createBlockFile(entrySize),
                Messages.DIRECTORY_ENTRY_CREATE_ERROR,
                entry -> create(flags, name, nameBytes, entrySize, entry, blockManager, creator));
    }

    private static <T extends IDirectoryEntry<? extends IInternalDirectoryItem>> T create(int flags, String name,
                                                                                          byte[] nameBytes,
                                                                                          int entrySize,
                                                                                          IBlockFile entry,
                                                                                          IBlockManager blockManager,
                                                                                          ICreator<T> creator)
            throws FileFileSystemException {

        ByteBuffer entryData = ByteBuffer.allocateDirect(entrySize);
        fillNewEntryData(entryData, flags, nameBytes);
        IOUtilities.flipBufferAndWrite(entryData, entry::write, Messages.DIRECTORY_ENTRY_DATA_WRITE_ERROR);

        return creator.create(entry, name, blockManager);
    }

    static void fillNewEntryData(ByteBuffer entryData, int flags, byte[] nameBytes) {
        entryData.putInt(flags);
        entryData.putLong(0L);
        entryData.putInt(IBlockManager.NULL_BLOCK_INDEX);
        fillNewNameData(entryData, nameBytes);
    }

    public static IDirectoryEntry<? extends IInternalDirectoryItem> openAny(int blockChainHead,
                                                                            IBlockManager blockManager)
            throws FileFileSystemException {

        return open(blockChainHead, false, blockManager, DirectoryEntry::openAny);
    }

    static <T extends IDirectoryEntry<? extends IInternalDirectoryItem>> T open(int blockChainHead,
                                                                                boolean skipCheckBlockChainHead,
                                                                                IBlockManager blockManager,
                                                                                IOpener<T> opener)
            throws FileFileSystemException {

        IBlockFile entry = ErrorHandlingHelper
                .get(() -> blockManager.openBlockFile(FIXED_SIZE_DATA_SIZE, blockChainHead, skipCheckBlockChainHead),
                        Messages.DIRECTORY_ENTRY_BLOCK_FILE_OPEN_ERROR);
        ByteBuffer entryFixedSizeData = createReadAndFlipBuffer(FIXED_SIZE_DATA_SIZE, entry,
                Messages.DIRECTORY_ENTRY_FIXED_SIZE_DATA_READ_ERROR);

        int flags = entryFixedSizeData.getInt();
        boolean isFile = flags == FILE_FLAGS_VALUE;
        boolean isDirectory = flags == DIRECTORY_FLAGS_VALUE;
        if (!isFile && !isDirectory) {
            throw new FileFileSystemException(Messages.BAD_DIRECTORY_ENTRY_FLAGS_ERROR);
        }

        long contentSize = entryFixedSizeData.getLong();
        int contentBlockChainHead = entryFixedSizeData.getInt();
        int nameSize = Short.toUnsignedInt(entryFixedSizeData.getShort());

        return opener.open(entry, isDirectory, contentSize, contentBlockChainHead, nameSize, blockManager);
    }

    private static ByteBuffer createReadAndFlipBuffer(int size, IBlockFile file, String errorMessage)
            throws FileFileSystemException {

        return IOUtilities.createReadAndFlipBuffer(size, file::read, errorMessage);
    }

    private static IDirectoryEntry<? extends IInternalDirectoryItem> openAny(IBlockFile entry, boolean isDirectory,
                                                                             long contentSize,
                                                                             int contentBlockChainHead, int nameSize,
                                                                             IBlockManager blockManager)
            throws FileFileSystemException {

        if (nameSize == 0) {
            throw new FileFileSystemException(Messages.BAD_DIRECTORY_ENTRY_NAME_ERROR);
        }

        entry.setCalculatedSize(getEntrySize(nameSize));

        ByteBuffer entryName = createReadAndFlipBuffer(nameSize, entry, Messages.DIRECTORY_ENTRY_NAME_READ_ERROR);
        byte[] nameBytes = new byte[nameSize];
        entryName.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);

        return isDirectory ?
                new DirectoryDirectoryEntry(entry, contentSize, contentBlockChainHead, name, blockManager) :
                new FileDirectoryEntry(entry, contentSize, contentBlockChainHead, name, blockManager);
    }
}
