package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectoryItem;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFile;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IRootDirectory;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class RootDirectory implements IRootParentDirectory {
    private final IRootDirectoryDirectoryEntry entry;

    private final IBlockManager blockManager;

    private RootDirectory(IRootDirectoryDirectoryEntry entry, IBlockManager blockManager) {
        this.entry = entry;

        this.blockManager = blockManager;
    }

    @Override
    public String getName() {
        return null;// TODO
    }

    @Override
    public void setName(String newName) throws FileFileSystemException {
        // TODO
    }

    @Override
    public void remove() throws FileFileSystemException {
        // TODO
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean isEmpty() throws FileFileSystemException {
        return false;// TODO
    }

    @Override
    public void close() throws FileFileSystemException {
        // TODO
    }

    @Override
    public IDirectory getParentDirectory() {
        return null;// TODO
    }

    @Override
    public IFile createFile(String name) throws FileFileSystemException {
        return null;// TODO
    }

    @Override
    public IFile openFile(String name) throws FileFileSystemException {
        return null;// TODO
    }

    @Override
    public IDirectory createSubDirectory(String name) throws FileFileSystemException {
        return null;// TODO
    }

    @Override
    public IDirectory openSubDirectory(String name) throws FileFileSystemException {
        return null;// TODO
    }

    @Override
    public IDirectoryItem openItem(String name) throws FileFileSystemException {
        return null;// TODO
    }

    @Override
    public void removeItem(String name) throws FileFileSystemException {
        // TODO
    }

    @Override
    public void removeItem(int entryBlockChainHead) throws FileFileSystemException {
        // TODO
    }

    public static void format(ByteBuffer block) {
        RootDirectoryDirectoryEntry.format(block);
    }

    public static IRootDirectory open(IBlockManager blockManager) throws FileFileSystemException {
        return new RootDirectory(RootDirectoryDirectoryEntry.open(blockManager), blockManager);
    }
}
