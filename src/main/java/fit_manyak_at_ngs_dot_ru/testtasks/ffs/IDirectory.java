package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import java.util.Collection;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

@SuppressWarnings({"UnnecessaryInterfaceModifier", "unused"})
public interface IDirectory extends IDirectoryItem {
    public IFile createFile(String name) throws FileFileSystemException;
    public IFile openFile(String name) throws FileFileSystemException;

    public IDirectory createSubDirectory(String name) throws FileFileSystemException;
    public IDirectory openSubDirectory(String name) throws FileFileSystemException;

    public IDirectoryItem openItem(String name) throws FileFileSystemException;

    public Collection<String> getNames() throws FileFileSystemException;
}
