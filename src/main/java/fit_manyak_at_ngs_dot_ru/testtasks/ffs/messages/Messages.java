package fit_manyak_at_ngs_dot_ru.testtasks.ffs.messages;

import java.util.ResourceBundle;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 22.02.2017.
 */

public class Messages {
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(Messages.class.getName());

    public static final String BAD_SIZE_FOR_FORMAT_ERROR = BUNDLE.getString("BAD_SIZE_FOR_FORMAT_ERROR");
    public static final String BAD_SIZE_OF_FIXED_SIZE_DATA_ERROR =
            BUNDLE.getString("BAD_SIZE_OF_FIXED_SIZE_DATA_ERROR");
    public static final String BAD_SIGNATURE_ERROR = BUNDLE.getString("BAD_SIGNATURE_ERROR");
    public static final String BAD_BLOCK_SIZE_RATIO_ERROR = BUNDLE.getString("BAD_BLOCK_SIZE_RATIO_ERROR");
    public static final String BAD_BLOCK_INDEX_SIZE_EXPONENT_ERROR =
            BUNDLE.getString("BAD_BLOCK_INDEX_SIZE_EXPONENT_ERROR");
    public static final String BAD_CONTENT_SIZE_SIZE_EXPONENT_ERROR =
            BUNDLE.getString("BAD_CONTENT_SIZE_SIZE_EXPONENT_ERROR");
    public static final String BAD_BLOCK_COUNT_ERROR = BUNDLE.getString("BAD_BLOCK_COUNT_ERROR");
    public static final String BAD_FREE_BLOCK_COUNT_ERROR = BUNDLE.getString("BAD_FREE_BLOCK_COUNT_ERROR");
    public static final String BAD_SIZE_ERROR = BUNDLE.getString("BAD_SIZE_ERROR");
}
