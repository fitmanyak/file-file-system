package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 25.02.2017.
 */

public class DirectoryEntry {
    private static final int FLAGS_SIZE = 4;
    private static final int FILE_FLAGS = 0;
    private static final int DIRECTORY_FLAGS = 1;

    private static final int CONTENT_DATA_SIZE = NewBlockManager.CONTENT_SIZE_SIZE + NewBlockManager.BLOCK_INDEX_SIZE;

    private static final int NAME_SIZE = 2;
    private static final int NAME_MAXIMAL_SIZE = (1 << (8 * NAME_SIZE)) - 1;

    private static final int FIXED_SIZE_DATA_SIZE = FLAGS_SIZE + CONTENT_DATA_SIZE + NAME_SIZE;

    private static final int FIRST_BLOCK_NAME_AREA_SIZE = NewBlockManager.BLOCK_SIZE - FIXED_SIZE_DATA_SIZE;

    private static final long INITIAL_CONTENT_SIZE = 0L;

    private static final short ROOT_DIRECTORY_NAME_SIZE = 0;

    private final int blockChainHead;

    private final boolean isDirectory;
    private long contentSize;
    private int contentBlockChainHead;
    private final String name;

    private final NewBlockManager blockManager;

    private DirectoryEntry(int blockChainHead, boolean isDirectory, long contentSize, int contentBlockChainHead,
                           String name, NewBlockManager blockManager) {

        this.blockChainHead = blockChainHead;

        this.isDirectory = isDirectory;
        this.contentSize = contentSize;
        this.contentBlockChainHead = contentBlockChainHead;
        this.name = name;

        this.blockManager = blockManager;
    }

    public int getBlockChainHead() {
        return blockChainHead;
    }

    public long getContentSize() {
        return contentSize;
    }

    /*public void updateContentSize(long newContentSize) throws IOException, IllegalArgumentException {
        ByteBuffer contentSizeBuffer = ByteBuffer.allocateDirect(BlockManager.CONTENT_SIZE_SIZE);
        contentSizeBuffer.putLong(newContentSize);
        blockManager.writeDataInBlock(blockChainHead, FLAGS_SIZE, contentSizeBuffer);

        contentSize = newContentSize;
    }*/

    public int getContentBlockChainHead() {
        return contentBlockChainHead;
    }

    /*public void updateContentData(long newContentSize, int newContentBlockChainHead) throws IOException, IllegalArgumentException {
        ByteBuffer contentData = ByteBuffer.allocateDirect(CONTENT_DATA_SIZE);
        contentData.putLong(newContentSize);
        contentData.putInt(newContentBlockChainHead);
        blockManager.writeDataInBlock(blockChainHead, FLAGS_SIZE, contentData);

        contentSize = newContentSize;
        contentBlockChainHead = newContentBlockChainHead;
    }*/

    public static void formatRootDirectoryEntry(ByteBuffer rootDirectoryEntry) {
        rootDirectoryEntry.putInt(DIRECTORY_FLAGS);
        rootDirectoryEntry.putLong(INITIAL_CONTENT_SIZE);
        rootDirectoryEntry.putInt(NewBlockManager.NULL_BLOCK_INDEX);
        rootDirectoryEntry.putShort(ROOT_DIRECTORY_NAME_SIZE);
    }

    /*public static void checkRootDirectoryEntry(ByteBuffer rootDirectoryEntry) throws FileFileSystemException {
        if (rootDirectoryEntry.getInt() != DIRECTORY_FLAGS) {
            throw new FileFileSystemException(Messages.BAD_ROOT_DIRECTORY_ENTRY_FLAGS_ERROR);
        }

        long rootDirectoryContentSize = rootDirectoryEntry.getLong();
        int rootDirectoryContentBlockChainHead = rootDirectoryEntry.getInt();
        BlockManager.checkBlockChainHead((rootDirectoryContentSize == 0L), rootDirectoryContentBlockChainHead,
                Messages.BAD_ROOT_DIRECTORY_ENTRY_CONTENT_BLOCK_CHAIN_HEAD_ERROR);

        if (rootDirectoryEntry.getShort() != ROOT_DIRECTORY_NAME_SIZE) {
            throw new FileFileSystemException(Messages.BAD_ROOT_DIRECTORY_ENTRY_NAME_ERROR);
        }
    }

    public static DirectoryEntry open(ByteBuffer block, int blockChainHead, BlockManager blockManager)
            throws FileFileSystemException {

        int flags = block.getInt();
        if (flags != FILE_FLAGS && flags != DIRECTORY_FLAGS) {
            throw new FileFileSystemException(Messages.BAD_DIRECTORY_ENTRY_FLAGS_ERROR);
        }

        long contentSize = block.getLong();
        int contentBlockChainHead = block.getInt();
        BlockManager.checkBlockChainHead((contentSize == 0L), contentBlockChainHead,
                Messages.BAD_DIRECTORY_ENTRY_CONTENT_BLOCK_CHAIN_HEAD_ERROR);

        int nameSize = Short.toUnsignedInt(block.getShort());
        if (nameSize == 0 || nameSize > FIRST_BLOCK_NAME_AREA_SIZE) {
            throw new FileFileSystemException(Messages.BAD_DIRECTORY_ENTRY_NAME_ERROR);
        }

        byte[] nameBytes = new byte[nameSize];
        block.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);

        return new DirectoryEntry(blockChainHead, (flags == DIRECTORY_FLAGS), contentSize, contentBlockChainHead, name,
                blockManager);
    }

    public static DirectoryEntry createFile(String name, BlockManager blockManager) throws IOException, IllegalArgumentException {
        return create(false, name, blockManager);
    }

    private static DirectoryEntry create(boolean isDirectory, String name, BlockManager blockManager)
            throws IOException, IllegalArgumentException {

        int blockChainHead = blockManager.allocBlock();

        ByteBuffer block = ByteBuffer.allocateDirect(BlockManager.BLOCK_SIZE);
        block.putInt(isDirectory ? DIRECTORY_FLAGS : FILE_FLAGS);
        block.putLong(INITIAL_CONTENT_SIZE);
        block.putInt(BlockManager.NULL_BLOCK_INDEX);

        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        block.putShort((short) nameBytes.length);// TODO
        block.put(nameBytes);

        blockManager.writeBlock(blockChainHead, block);

        return new DirectoryEntry(blockChainHead, isDirectory, INITIAL_CONTENT_SIZE, BlockManager.NULL_BLOCK_INDEX,
                name, blockManager);
    }*/
}
