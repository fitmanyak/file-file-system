package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.messages.Messages;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 25.02.2017.
 */

public class DirectoryEntry {
    private static final int DIRECTORY_ENTRY_FLAGS_SIZE = 4;
    private static final int DIRECTORY_ENTRY_FILE_FLAGS = 0;
    private static final int DIRECTORY_ENTRY_DIRECTORY_FLAGS = 1;

    private static final int DIRECTORY_ENTRY_NAME_SIZE = 2;
    private static final int DIRECTORY_ENTRY_NAME_MAXIMAL_SIZE = (1 << (8 * DIRECTORY_ENTRY_NAME_SIZE)) - 1;

    private static final int DIRECTORY_ENTRY_FIXED_SIZE_DATA_SIZE =
            BlockManager.BLOCK_INDEX_SIZE + DIRECTORY_ENTRY_FLAGS_SIZE + BlockManager.CONTENT_SIZE_SIZE +
                    BlockManager.BLOCK_INDEX_SIZE + DIRECTORY_ENTRY_NAME_SIZE;

    private static final int DIRECTORY_ENTRY_FIRST_BLOCK_NAME_AREA_SIZE =
            BlockManager.BLOCK_SIZE - DIRECTORY_ENTRY_FIXED_SIZE_DATA_SIZE;

    private static final long DIRECTORY_ENTRY_INITIAL_CONTENT_SIZE = 0L;

    private static final short ROOT_DIRECTORY_NAME_SIZE = 0;

    public static void formatRootDirectoryEntry(ByteBuffer rootDirectoryEntry) {
        rootDirectoryEntry.putInt(BlockManager.NULL_BLOCK_INDEX);
        rootDirectoryEntry.putInt(DIRECTORY_ENTRY_DIRECTORY_FLAGS);
        rootDirectoryEntry.putLong(DIRECTORY_ENTRY_INITIAL_CONTENT_SIZE);
        rootDirectoryEntry.putInt(BlockManager.NULL_BLOCK_INDEX);
        rootDirectoryEntry.putShort(ROOT_DIRECTORY_NAME_SIZE);
    }

    public static void checkRootDirectoryEntry(ByteBuffer rootDirectoryEntry) throws FileFileSystemException {
        if (rootDirectoryEntry.getInt() != BlockManager.NULL_BLOCK_INDEX) {
            throw new FileFileSystemException(
                    Messages.BAD_ROOT_DIRECTORY_ENTRY_PARENT_DIRECTORY_ENTRY_BLOCK_CHAIN_HEAD_ERROR);
        }

        if (rootDirectoryEntry.getInt() != DIRECTORY_ENTRY_DIRECTORY_FLAGS) {
            throw new FileFileSystemException(Messages.BAD_ROOT_DIRECTORY_ENTRY_FLAGS_ERROR);
        }

        long rootDirectoryContentSize = rootDirectoryEntry.getLong();
        int rootDirectoryContentBlockChainHead = rootDirectoryEntry.getInt();
        BlockManager.checkBlockChainHead(() -> (rootDirectoryContentSize == 0), rootDirectoryContentBlockChainHead,
                Messages.BAD_ROOT_DIRECTORY_ENTRY_CONTENT_BLOCK_CHAIN_HEAD_ERROR);

        if (rootDirectoryEntry.getShort() != ROOT_DIRECTORY_NAME_SIZE) {
            throw new FileFileSystemException(Messages.BAD_ROOT_DIRECTORY_ENTRY_NAME_ERROR);
        }
    }
}
