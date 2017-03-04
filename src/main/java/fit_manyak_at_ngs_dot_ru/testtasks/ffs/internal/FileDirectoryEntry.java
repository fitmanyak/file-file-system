package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import java.io.IOException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class FileDirectoryEntry extends NamedDirectoryEntry {
    private FileDirectoryEntry(IBlockFile entry, IBlockFile contentFile, String name) {
        super(entry, contentFile, name);
    }

    public static FileDirectoryEntry create(String name, BlockManager blockManager)
            throws IOException, IllegalArgumentException {

        return createNamed(FILE_FLAGS, name, blockManager, FileDirectoryEntry::new);
    }
}
