package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectoryItem;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFile;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOUtilities;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 06.03.2017.
 */

public abstract class BaseDirectory<TItem extends IInternalDirectory, TEntry extends IBaseDirectoryDirectoryEntry<TItem>>
        extends DirectoryItem<TItem, TEntry> implements IInternalDirectory {

    private static final long BLOCK_INDEX_SIZE_MINUS_ONE = IBlockManager.BLOCK_INDEX_SIZE - 1L;

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface ICreator<T extends IInternalDirectoryItem> {
        public T create(String name, IInternalDirectory parentDirectory, IBlockManager blockManager)
                throws FileFileSystemException;
    }

    private final ByteBuffer subEntryBlockChainHead;

    protected BaseDirectory(TEntry entry, IInternalDirectory parentDirectory) {
        super(entry, parentDirectory);

        subEntryBlockChainHead = ByteBuffer.allocateDirect(IBlockManager.BLOCK_INDEX_SIZE);
    }

    @Override
    public void remove() throws FileFileSystemException {
        if (!isEmpty()) {
            throw new FileFileSystemException("Can't remove directory because it isn't empty");// TODO
        }

        super.remove();
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public IInternalFile getAsFile() throws FileFileSystemException {
        throw new FileFileSystemException("Directory isn't file");// TODO
    }

    @Override
    public IInternalDirectory getAsDirectory() throws FileFileSystemException {
        return this;
    }

    @Override
    public IFile createFile(String name) throws FileFileSystemException {
        return createItem(name, File::create);
    }

    private <T extends IInternalDirectoryItem> T createItem(String name, ICreator<T> creator)
            throws FileFileSystemException {

        IDirectFile content = checkNameUniqueInternal(name);

        T item = creator.create(name, this, getBlockManger());
        subEntryBlockChainHead.putInt(item.getBlockChainHead());
        IOUtilities.flipBufferAndWrite(subEntryBlockChainHead, source -> content.write(source),
                "Directory content write error");// TODO

        return item;
    }

    @Override
    public IFile openFile(String name) throws FileFileSystemException {
        return openItemInternal(name).getAsFile();
    }

    private IInternalDirectory openItemInternal(String name) throws FileFileSystemException {
        return null;
    }

    private static long getItemCount(long size) {
        return size >> IBlockManager.BLOCK_INDEX_SIZE_EXPONENT;
    }

    @Override
    public IDirectory createSubDirectory(String name) throws FileFileSystemException {
        return createItem(name, Directory::create);
    }

    @Override
    public IDirectory openSubDirectory(String name) throws FileFileSystemException {
        return openItemInternal(name).getAsDirectory();
    }

    @Override
    public IDirectoryItem openItem(String name) throws FileFileSystemException {
        return openItemInternal(name);
    }

    @Override
    public void checkNameUnique(String name) throws FileFileSystemException {
        checkNameUniqueInternal(name);
    }

    private IDirectFile checkNameUniqueInternal(String name) throws FileFileSystemException {
        return null;
    }

    @Override
    public void removeItem(int entryBlockChainHead) throws FileFileSystemException {
    }

    @Override
    public Collection<String> getNames() throws FileFileSystemException {
        return null;
    }

    @Override
    protected IDirectFile getContent() throws FileFileSystemException {
        IDirectFile content = super.getContent();
        if ((content.getSize() & BLOCK_INDEX_SIZE_MINUS_ONE) != 0L) {
            throw new FileFileSystemException("Bad directory content size");// TODO
        }

        return content;
    }
}
