package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class RootDirectoryDirectoryEntry extends NewDirectoryEntry {
    private static final String NAME = "";
    private static final byte[] NAME_BYTES = new byte[0];

    private RootDirectoryDirectoryEntry(IBlockFile entry, IBlockFile contentFile) {
        super(entry, contentFile, NAME);
    }

    public static void format(ByteBuffer block) {
        fillNewEntryData(block, DIRECTORY_FLAGS, NAME_BYTES);
    }
}
