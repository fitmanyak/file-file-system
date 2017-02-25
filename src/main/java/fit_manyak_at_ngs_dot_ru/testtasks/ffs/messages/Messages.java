package fit_manyak_at_ngs_dot_ru.testtasks.ffs.messages;

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
    public static final String BAD_ROOT_DIRECTORY_ENTRY_PARENT_DIRECTORY_ENTRY_BLOCK_CHAIN_HEAD_ERROR =
            BUNDLE.getString("BAD_ROOT_DIRECTORY_ENTRY_PARENT_DIRECTORY_ENTRY_BLOCK_CHAIN_HEAD_ERROR");
    public static final String BAD_ROOT_DIRECTORY_ENTRY_FLAGS_ERROR =
            BUNDLE.getString("BAD_ROOT_DIRECTORY_ENTRY_FLAGS_ERROR");
    public static final String BAD_ROOT_DIRECTORY_ENTRY_CONTENT_BLOCK_CHAIN_HEAD_ERROR =
            BUNDLE.getString("BAD_ROOT_DIRECTORY_ENTRY_CONTENT_BLOCK_CHAIN_HEAD_ERROR");
    public static final String BAD_ROOT_DIRECTORY_ENTRY_NAME_ERROR =
            BUNDLE.getString("BAD_ROOT_DIRECTORY_ENTRY_NAME_ERROR");
}
