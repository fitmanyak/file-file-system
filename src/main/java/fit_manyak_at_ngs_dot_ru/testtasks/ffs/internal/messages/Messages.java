package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages;

import java.util.ResourceBundle;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 22.02.2017.
 */

public class Messages {
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(Messages.class.getName());

    public static final String BAD_SIZE_FOR_FORMAT_ERROR = BUNDLE.getString("BAD_SIZE_FOR_FORMAT_ERROR");

    public static final String FIXED_SIZE_DATA_READ_ERROR = BUNDLE.getString("FIXED_SIZE_DATA_READ_ERROR");
    public static final String BAD_SIGNATURE_ERROR = BUNDLE.getString("BAD_SIGNATURE_ERROR");
    public static final String BAD_BLOCK_SIZE_RATIO_ERROR = BUNDLE.getString("BAD_BLOCK_SIZE_RATIO_ERROR");
    public static final String BAD_BLOCK_INDEX_SIZE_EXPONENT_ERROR =
            BUNDLE.getString("BAD_BLOCK_INDEX_SIZE_EXPONENT_ERROR");
    public static final String BAD_CONTENT_SIZE_SIZE_EXPONENT_ERROR =
            BUNDLE.getString("BAD_CONTENT_SIZE_SIZE_EXPONENT_ERROR");
    public static final String BAD_BLOCK_COUNT_ERROR = BUNDLE.getString("BAD_BLOCK_COUNT_ERROR");
    public static final String BAD_FREE_BLOCK_COUNT_ERROR = BUNDLE.getString("BAD_FREE_BLOCK_COUNT_ERROR");
    public static final String BAD_FREE_BLOCK_CHAIN_HEAD_ERROR = BUNDLE.getString("BAD_FREE_BLOCK_CHAIN_HEAD_ERROR");

    public static final String BAD_SIZE_ERROR = BUNDLE.getString("BAD_SIZE_ERROR");

    public static final String NEXT_BLOCK_INDEX_READ_ERROR = BUNDLE.getString("NEXT_BLOCK_INDEX_READ_ERROR");
    public static final String BAD_ROOT_DIRECTORY_ENTRY_BLOCK_NEXT_BLOCK_INDEX_ERROR =
            BUNDLE.getString("BAD_ROOT_DIRECTORY_ENTRY_BLOCK_NEXT_BLOCK_INDEX_ERROR");

    public static final String BLOCK_READ_ERROR = BUNDLE.getString("BLOCK_READ_ERROR");

    public static final String BAD_ROOT_DIRECTORY_ENTRY_FLAGS_ERROR =
            BUNDLE.getString("BAD_ROOT_DIRECTORY_ENTRY_FLAGS_ERROR");
    public static final String BAD_ROOT_DIRECTORY_ENTRY_CONTENT_BLOCK_CHAIN_HEAD_ERROR =
            BUNDLE.getString("BAD_ROOT_DIRECTORY_ENTRY_CONTENT_BLOCK_CHAIN_HEAD_ERROR");
    public static final String BAD_ROOT_DIRECTORY_ENTRY_NAME_ERROR =
            BUNDLE.getString("BAD_ROOT_DIRECTORY_ENTRY_NAME_ERROR");

    public static final String BAD_DIRECTORY_ENTRY_FLAGS_ERROR = BUNDLE.getString("BAD_DIRECTORY_ENTRY_FLAGS_ERROR");
    public static final String BAD_DIRECTORY_ENTRY_CONTENT_BLOCK_CHAIN_HEAD_ERROR =
            BUNDLE.getString("BAD_DIRECTORY_ENTRY_CONTENT_BLOCK_CHAIN_HEAD_ERROR");
    public static final String BAD_DIRECTORY_ENTRY_NAME_ERROR = BUNDLE.getString("BAD_DIRECTORY_ENTRY_NAME_ERROR");

    public static final String BAD_BLOCK_FILE_POSITION_ERROR = BUNDLE.getString("BAD_BLOCK_FILE_POSITION_ERROR");
    public static final String BIG_BLOCK_FILE_POSITION_ERROR = BUNDLE.getString("BIG_BLOCK_FILE_POSITION_ERROR");
    public static final String READ_ONLY_BUFFER_ERROR = BUNDLE.getString("READ_ONLY_BUFFER_ERROR");
    public static final String FILE_CHANNEL_CLOSE_ERROR = BUNDLE.getString("FILE_CHANNEL_CLOSE_ERROR");
    public static final String BAD_BLOCK_FILE_SIZE_ERROR = BUNDLE.getString("BAD_BLOCK_FILE_SIZE_ERROR");
    public static final String NOT_ENOUGH_FREE_BLOCKS_ERROR = BUNDLE.getString("NOT_ENOUGH_FREE_BLOCKS_ERROR");
    public static final String UNEXPECTED_END_BLOCK_CHAIN_ERROR = BUNDLE.getString("UNEXPECTED_END_BLOCK_CHAIN_ERROR");
    public static final String FREE_BLOCK_DATA_WRITE_ERROR = BUNDLE.getString("FREE_BLOCK_DATA_WRITE_ERROR");
    public static final String NEXT_BLOCK_INDEX_WRITE_ERROR = BUNDLE.getString("NEXT_BLOCK_INDEX_WRITE_ERROR");
    public static final String TOO_MANY_RELEASED_BLOCKS_ERROR = BUNDLE.getString("TOO_MANY_RELEASED_BLOCKS_ERROR");
    public static final String BLOCK_WRITE_ERROR = BUNDLE.getString("BLOCK_WRITE_ERROR");
    public static final String BAD_BLOCK_FILE_BLOCK_CHAIN_LENGTH_ERROR =
            BUNDLE.getString("BAD_BLOCK_FILE_BLOCK_CHAIN_LENGTH_ERROR");
    public static final String BAD_BLOCK_FILE_BLOCK_CHAIN_HEAD_ERROR =
            BUNDLE.getString("BAD_BLOCK_FILE_BLOCK_CHAIN_HEAD_ERROR");
    public static final String FILE_OPEN_ERROR = BUNDLE.getString("FILE_OPEN_ERROR");
    public static final String FILE_CLOSE_ERROR = BUNDLE.getString("FILE_CLOSE_ERROR");
    public static final String FILE_SIZE_SET_ERROR = BUNDLE.getString("FILE_SIZE_SET_ERROR");
    public static final String FILE_CHANNEL_GET_ERROR = BUNDLE.getString("FILE_CHANNEL_GET_ERROR");
    public static final String FIXED_SIZE_DATA_WRITE_ERROR = BUNDLE.getString("FIXED_SIZE_DATA_WRITE_ERROR");
    public static final String ROOT_DIRECTORY_ENTRY_WRITE_ERROR = BUNDLE.getString("ROOT_DIRECTORY_ENTRY_WRITE_ERROR");
    public static final String FILE_CHANNEL_OPEN_ERROR = BUNDLE.getString("FILE_CHANNEL_OPEN_ERROR");
    public static final String FILE_CHANNEL_SIZE_GET_ERROR = BUNDLE.getString("FILE_CHANNEL_SIZE_GET_ERROR");

    public static final String DIRECTORY_ENTRY_CONTENT_DATA_WRITE_ERROR =
            BUNDLE.getString("DIRECTORY_ENTRY_CONTENT_DATA_WRITE_ERROR");
    public static final String DIRECTORY_ENTRY_RENAME_ERROR = BUNDLE.getString("DIRECTORY_ENTRY_RENAME_ERROR");
    public static final String EMPTY_DIRECTORY_ENTRY_NAME_ERROR = BUNDLE.getString("EMPTY_DIRECTORY_ENTRY_NAME_ERROR");
    public static final String TOO_LONG_DIRECTORY_ENTRY_NAME_ERROR =
            BUNDLE.getString("TOO_LONG_DIRECTORY_ENTRY_NAME_ERROR");
    public static final String DIRECTORY_ENTRY_NAME_DATA_WRITE_ERROR =
            BUNDLE.getString("DIRECTORY_ENTRY_NAME_DATA_WRITE_ERROR");
    public static final String DIRECTORY_ENTRY_REMOVE_ERROR = BUNDLE.getString("DIRECTORY_ENTRY_REMOVE_ERROR");
    public static final String DIRECTORY_ENTRY_CONTENT_BLOCK_FILE_OPEN_ERROR =
            BUNDLE.getString("DIRECTORY_ENTRY_CONTENT_BLOCK_FILE_OPEN_ERROR");
    public static final String DIRECTORY_ENTRY_CREATE_ERROR = BUNDLE.getString("DIRECTORY_ENTRY_CREATE_ERROR");
    public static final String DIRECTORY_ENTRY_DATA_WRITE_ERROR = BUNDLE.getString("DIRECTORY_ENTRY_DATA_WRITE_ERROR");
    public static final String DIRECTORY_ENTRY_BLOCK_FILE_OPEN_ERROR =
            BUNDLE.getString("DIRECTORY_ENTRY_BLOCK_FILE_OPEN_ERROR");
    public static final String DIRECTORY_ENTRY_FIXED_SIZE_DATA_READ_ERROR =
            BUNDLE.getString("DIRECTORY_ENTRY_FIXED_SIZE_DATA_READ_ERROR");
    public static final String DIRECTORY_ENTRY_NAME_READ_ERROR = BUNDLE.getString("DIRECTORY_ENTRY_NAME_READ_ERROR");

    public static final String CANT_RENAME_ROOT_DIRECTORY_ERROR = BUNDLE.getString("CANT_RENAME_ROOT_DIRECTORY_ERROR");
    public static final String CANT_REMOVE_ROOT_DIRECTORY_ERROR = BUNDLE.getString("CANT_REMOVE_ROOT_DIRECTORY_ERROR");
    public static final String CANT_GET_ROOT_DIRECTORY_THROUGH_ENTRY_ERROR =
            BUNDLE.getString("CANT_GET_ROOT_DIRECTORY_THROUGH_ENTRY_ERROR");

    public static final String DIRECTORY_ITEM_RENAME_ERROR = BUNDLE.getString("DIRECTORY_ITEM_RENAME_ERROR");
    public static final String DIRECTORY_ITEM_REMOVE_ERROR = BUNDLE.getString("DIRECTORY_ITEM_REMOVE_ERROR");
    public static final String DIRECTORY_ITEM_CONTENT_GET_ERROR = BUNDLE.getString("DIRECTORY_ITEM_CONTENT_GET_ERROR");

    public static final String CANT_REMOVE_NOT_EMPTY_DIRECTORY_ERROR =
            BUNDLE.getString("CANT_REMOVE_NOT_EMPTY_DIRECTORY_ERROR");
    public static final String DIRECTORY_NOT_FILE_ERROR = BUNDLE.getString("DIRECTORY_NOT_FILE_ERROR");
    public static final String DIRECTORY_CONTENT_WRITE_ERROR = BUNDLE.getString("DIRECTORY_CONTENT_WRITE_ERROR");
    public static final String DIRECTORY_SUB_ENTRY_MISSING_ERROR =
            BUNDLE.getString("DIRECTORY_SUB_ENTRY_MISSING_ERROR");
    public static final String DIRECTORY_SUB_ENTRY_OPEN_ERROR = BUNDLE.getString("DIRECTORY_SUB_ENTRY_OPEN_ERROR");
    public static final String DIRECTORY_SUB_ENTRY_EXISTS_ERROR = BUNDLE.getString("DIRECTORY_SUB_ENTRY_EXISTS_ERROR");
    public static final String BAD_REMOVED_ENTRY_BLOCK_CHAIN_HEAD_ERROR =
            BUNDLE.getString("BAD_REMOVED_ENTRY_BLOCK_CHAIN_HEAD_ERROR");
    public static final String REMOVED_ENTRY_MISSING_ERROR = BUNDLE.getString("REMOVED_ENTRY_MISSING_ERROR");
    public static final String BAD_DIRECTORY_CONTENT_SIZE_ERROR = BUNDLE.getString("BAD_DIRECTORY_CONTENT_SIZE_ERROR");

    public static final String DIRECTORY_CREATE_ERROR = BUNDLE.getString("DIRECTORY_CREATE_ERROR");
}
