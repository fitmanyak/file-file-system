package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import java.io.IOException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class DirectoryDirectoryEntry extends NamedDirectoryEntry {
    private DirectoryDirectoryEntry(IBlockFile entry, IBlockFile contentFile, String name) {
        super(entry, contentFile, name);
    }

    public static DirectoryDirectoryEntry create(String name, BlockManager blockManager)
            throws IOException, IllegalArgumentException {

        return createNamed(DIRECTORY_FLAGS, name, blockManager, DirectoryDirectoryEntry::new);
    }
}
