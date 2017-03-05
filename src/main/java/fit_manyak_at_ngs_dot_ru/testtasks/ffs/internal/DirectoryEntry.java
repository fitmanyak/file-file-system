package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.ICommonFile;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IAction;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOUtilities;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOperation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public abstract class DirectoryEntry {
    private static final int FLAGS_SIZE = 4;
    protected static final int FILE_FLAGS = 0;
    protected static final int DIRECTORY_FLAGS = 1;

    private static final int CONTENT_DATA_SIZE = BlockManager.CONTENT_SIZE_SIZE + BlockManager.BLOCK_INDEX_SIZE;
    private static final long CONTENT_DATA_POSITION = FLAGS_SIZE;

    private static final int NAME_SIZE_SIZE = 2;
    private static final int NAME_MAXIMAL_SIZE = (1 << (8 * NAME_SIZE_SIZE)) - 1;

    private static final int WITHOUT_NAME_DATA_SIZE = FLAGS_SIZE + CONTENT_DATA_SIZE;
    private static final int FIXED_SIZE_DATA_SIZE = WITHOUT_NAME_DATA_SIZE + NAME_SIZE_SIZE;

    private static final long NAME_DATA_POSITION = WITHOUT_NAME_DATA_SIZE;

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

        private int performUpdatedContentDataOperation(ByteBuffer buffer, IOperation operation)
                throws FileFileSystemException {

            int result = operation.perform(buffer);

            updateContentData();

            return result;
        }

        @Override
        public int write(long newPosition, ByteBuffer source) throws FileFileSystemException {
            return performUpdatedContentDataOperation(source, src -> delegate.write(newPosition, src));
        }

        private int getBlockChainHead() {
            return delegate.getBlockChainHead();
        }

        private void remove() throws FileFileSystemException {
            performUpdatedContentDataAction(delegate::remove);
        }
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    protected interface ICreator<T extends DirectoryEntry> {
        public T create(IBlockFile entry, String name, BlockManager blockManager);
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    protected interface IKindChecker {
        public void check(boolean isDirectory) throws FileFileSystemException;
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    protected interface IOpener<T extends DirectoryEntry> {
        public T open(IBlockFile entry, boolean isDirectory, long contentSize, int contentBlockChainHead, int nameSize,
                      BlockManager blockManager) throws FileFileSystemException;
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    protected interface ICreatorForOpen<T extends DirectoryEntry> {
        public T create(IBlockFile entry, boolean isDirectory, long contentSize, int contentBlockChainHead, String name,
                        BlockManager blockManager);
    }

    private final IBlockFile entry;

    private Content content;
    private long contentSize;
    private int contentBlockChainHead;
    private ByteBuffer contentData;

    private String name;

    private final BlockManager blockManager;

    protected DirectoryEntry(IBlockFile entry, String name, BlockManager blockManager) {
        this(entry, 0L, BlockManager.NULL_BLOCK_INDEX, name, blockManager);
    }

    protected DirectoryEntry(IBlockFile entry, long contentSize, int contentBlockChainHead, String name,
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

            flipBufferAndWrite(CONTENT_DATA_POSITION, contentData, "Directory entry content data write error");// TODO

            contentSize = newContentSize;
            contentBlockChainHead = newContentBlockChainHead;
        }
    }

    private void flipBufferAndWrite(long position, ByteBuffer source, String errorMessage)
            throws FileFileSystemException {

        IOUtilities.flipBufferAndWrite(source, src -> entry.write(position, src), errorMessage);
    }

    public int getBlockChainHead() {
        return entry.getBlockChainHead();
    }

    public abstract boolean isDirectory();

    public ICommonFile getContent() throws FileFileSystemException {
        if (content == null) {
            content = ErrorHandlingHelper
                    .get(() -> new Content(blockManager.openBlockFile(contentSize, contentBlockChainHead)),
                            "Directory entry content block file open error");// TODO
        }

        return content;
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) throws FileFileSystemException {
        if (!name.equals(newName)) {
            checkNameNotEmpty(newName);

            byte[] newNameBytes = getNameBytes(newName);
            ErrorHandlingHelper
                    .performAction(() -> changeName(newNameBytes), "Directory entry name change error");// TODO

            name = newName;
        }
    }

    private static void checkNameNotEmpty(String name) throws FileFileSystemException {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Empty directory entry name");// TODO
        }
    }

    private static byte[] getNameBytes(String name) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        if (nameBytes.length > NAME_MAXIMAL_SIZE) {
            throw new IllegalArgumentException("Too long directory entry name");// TODO
        }

        return nameBytes;
    }

    private void changeName(byte[] newNameBytes) throws FileFileSystemException {
        int newEntrySize = getEntrySize(newNameBytes);
        entry.setSize(newEntrySize);

        int newNameDataSize = newEntrySize - WITHOUT_NAME_DATA_SIZE;
        ByteBuffer newNameData = ByteBuffer.allocateDirect(newNameDataSize);
        fillNewNameData(newNameData, newNameBytes);
        flipBufferAndWrite(NAME_DATA_POSITION, newNameData, "Directory entry name data write error");// TODO
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

    protected static <T extends DirectoryEntry> T createNamed(int flags, String name, BlockManager blockManager,
                                                              ICreator<T> creator) throws FileFileSystemException {

        checkNameNotEmpty(name);

        return create(flags, name, blockManager, creator);
    }

    protected static <T extends DirectoryEntry> T create(int flags, String name, BlockManager blockManager,
                                                         ICreator<T> creator) throws FileFileSystemException {

        byte[] nameBytes = getNameBytes(name);
        int entrySize = getEntrySize(nameBytes);

        return ErrorHandlingHelper
                .getWithCloseableArgument(() -> blockManager.createBlockFile(entrySize), "Directory entry create error",
                        entry -> create(flags, name, nameBytes, entrySize, entry, blockManager, creator));// TODO
    }

    private static <T extends DirectoryEntry> T create(int flags, String name, byte[] nameBytes, int entrySize,
                                                       IBlockFile entry, BlockManager blockManager, ICreator<T> creator)
            throws FileFileSystemException {

        ByteBuffer entryData = ByteBuffer.allocateDirect(entrySize);
        fillNewEntryData(entryData, flags, nameBytes);
        IOUtilities.flipBufferAndWrite(entryData, src -> entry.write(src), "Directory entry data write error");// TODO

        return creator.create(entry, name, blockManager);
    }

    protected static void fillNewEntryData(ByteBuffer entryData, int flags, byte[] nameBytes) {
        entryData.putInt(flags);
        entryData.putLong(0L);
        entryData.putInt(BlockManager.NULL_BLOCK_INDEX);
        fillNewNameData(entryData, nameBytes);
    }

    public static DirectoryEntry openAny(int blockChainHead, BlockManager blockManager) throws FileFileSystemException {
        return open(blockChainHead, blockManager, DirectoryEntry::checkAny, getOpenerAny());
    }

    protected static <T extends DirectoryEntry> T openTyped(int blockChainHead, BlockManager blockManager,
                                                            IKindChecker checker, ICreatorForOpen<T> creator)
            throws FileFileSystemException {

        return open(blockChainHead, blockManager, checker, getOpener(creator));
    }

    protected static <T extends DirectoryEntry> T open(int blockChainHead, BlockManager blockManager,
                                                       IKindChecker checker, IOpener<T> opener)
            throws FileFileSystemException {

        IBlockFile entry = blockManager.openBlockFile(FIXED_SIZE_DATA_SIZE, blockChainHead);
        ByteBuffer entryFixedSizeData = createReadAndFlipBuffer(FIXED_SIZE_DATA_SIZE, entry,
                "Directory entry fixed-size data read error");// TODO

        int flags = entryFixedSizeData.getInt();
        boolean isFile = flags == FILE_FLAGS;
        boolean isDirectory = flags == DIRECTORY_FLAGS;
        if (!isFile && !isDirectory) {
            throw new FileFileSystemException(Messages.BAD_DIRECTORY_ENTRY_FLAGS_ERROR);
        }

        checker.check(isDirectory);

        long contentSize = entryFixedSizeData.getLong();
        int contentBlockChainHead = entryFixedSizeData.getInt();
        int nameSize = Short.toUnsignedInt(entryFixedSizeData.getShort());

        return opener.open(entry, isDirectory, contentSize, contentBlockChainHead, nameSize, blockManager);
    }

    private static ByteBuffer createReadAndFlipBuffer(int size, IBlockFile file, String errorMessage)
            throws FileFileSystemException {

        return IOUtilities.createReadAndFlipBuffer(size, file::read, errorMessage);
    }

    private static void checkAny(boolean isDirectory) throws FileFileSystemException {
    }

    protected static void checkIsDirectory(boolean isDirectory) throws FileFileSystemException {
        if (!isDirectory) {
            throw new FileFileSystemException("Directory entry is file directory entry");// TODO
        }
    }

    private static IOpener<DirectoryEntry> getOpenerAny() {
        return getOpener(DirectoryEntry::createForOpenAny);
    }

    private static <T extends DirectoryEntry> IOpener<T> getOpener(ICreatorForOpen<T> creator) {
        return (entry, isDirectory, contentSize, contentBlockChainHead, nameSize, blockManager) -> open(entry,
                isDirectory, contentSize, contentBlockChainHead, nameSize, blockManager, creator);
    }

    private static <T extends DirectoryEntry> T open(IBlockFile entry, boolean isDirectory, long contentSize,
                                                     int contentBlockChainHead, int nameSize, BlockManager blockManager,
                                                     ICreatorForOpen<T> creator) throws FileFileSystemException {

        if (nameSize == 0) {
            throw new FileFileSystemException(Messages.BAD_DIRECTORY_ENTRY_NAME_ERROR);
        }

        entry.setCalculatedSize(getEntrySize(nameSize));

        ByteBuffer entryName = createReadAndFlipBuffer(nameSize, entry, "Directory entry name read error");// TODO
        byte[] nameBytes = new byte[nameSize];
        entryName.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);

        return creator.create(entry, isDirectory, contentSize, contentBlockChainHead, name, blockManager);
    }

    private static DirectoryEntry createForOpenAny(IBlockFile entry, boolean isDirectory, long contentSize,
                                                   int contentBlockChainHead, String name, BlockManager blockManager) {

        return isDirectory ?
                new DirectoryDirectoryEntry(entry, contentSize, contentBlockChainHead, name, blockManager) :
                new FileDirectoryEntry(entry, contentSize, contentBlockChainHead, name, blockManager);
    }
}
