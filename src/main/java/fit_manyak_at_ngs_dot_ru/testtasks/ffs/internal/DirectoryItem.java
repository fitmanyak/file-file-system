package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IProvider;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 06.03.2017.
 */

public abstract class DirectoryItem<TItem extends IInternalDirectoryItem, TEntry extends IDirectoryEntry<TItem>>
        implements IInternalDirectoryItem {

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    protected interface IEntryCreator<TItem extends IInternalDirectoryItem, TEntry extends IDirectoryEntry<TItem>> {
        public TEntry create(String name, IBlockManager blockManager) throws FileFileSystemException;
    }

    private final TEntry entry;

    private final IInternalDirectory parentDirectory;

    protected DirectoryItem(TEntry entry, IInternalDirectory parentDirectory) {
        this.entry = entry;

        this.parentDirectory = parentDirectory;
    }

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public void setName(String newName) throws FileFileSystemException {
        ErrorHandlingHelper.performAction(() -> changeName(newName), "Directory item name change error");// TODO
    }

    private void changeName(String newName) throws FileFileSystemException {
        parentDirectory.checkNameUnique(newName);

        entry.setName(newName);
    }

    @Override
    public void remove() throws FileFileSystemException {
        ErrorHandlingHelper.performAction(this::performRemove, "Directory item remove error");// TODO
    }

    private void performRemove() throws FileFileSystemException {
        int entryBlockChainHead = entry.getBlockChainHead();
        entry.remove();

        parentDirectory.removeItem(entryBlockChainHead);
    }

    @Override
    public boolean isEmpty() throws FileFileSystemException {
        return getContent().isEmpty();
    }

    protected IDirectFile getContent() throws FileFileSystemException {
        return ErrorHandlingHelper.get(entry::getContent, "Directory item get content error");// TODO
    }

    @Override
    public void close() throws FileFileSystemException {
        // TODO
    }

    @Override
    public IDirectory getParentDirectory() {
        return parentDirectory;
    }

    @Override
    public int getBlockChainHead() {
        return entry.getBlockChainHead();
    }

    protected IBlockManager getBlockManger() {
        return entry.getBlockManager();
    }

    protected static <TItem extends IInternalDirectoryItem, TEntry extends IDirectoryEntry<TItem>> TItem createItem(
            String name, IInternalDirectory parentDirectory, IBlockManager blockManager,
            IEntryCreator<TItem, TEntry> creator, String errorMessage) throws FileFileSystemException {

        return ErrorHandlingHelper.get(() -> creator.create(name, blockManager), errorMessage).getItem(parentDirectory);
    }
}
