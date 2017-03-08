package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 06.03.2017.
 */

public abstract class DirectoryItem<TItem extends IInternalDirectoryItem, TEntry extends IDirectoryEntry<TItem>>
        implements IInternalDirectoryItem {

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    interface IEntryCreator<TItem extends IInternalDirectoryItem, TEntry extends IDirectoryEntry<TItem>> {
        public TEntry create(String name, IBlockManager blockManager) throws FileFileSystemException;
    }

    private final TEntry entry;

    private final IInternalDirectory parentDirectory;

    DirectoryItem(TEntry entry, IInternalDirectory parentDirectory) {
        this.entry = entry;

        this.parentDirectory = parentDirectory;
    }

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public void setName(String newName) throws FileFileSystemException {
        ErrorHandlingHelper.performAction(() -> changeName(newName), Messages.DIRECTORY_ITEM_RENAME_ERROR);
    }

    private void changeName(String newName) throws FileFileSystemException {
        parentDirectory.checkNameUnique(newName);

        entry.setName(newName);
    }

    @Override
    public void remove() throws FileFileSystemException {
        ErrorHandlingHelper.performAction(this::performRemove, Messages.DIRECTORY_ITEM_REMOVE_ERROR);
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

    IDirectFile getContent() throws FileFileSystemException {
        return ErrorHandlingHelper.get(entry::getContent, Messages.DIRECTORY_ITEM_CONTENT_GET_ERROR);
    }

    @Override
    public void close() throws FileFileSystemException {
        // TODO Necessary to support parallel operations
    }

    @Override
    public IDirectory getParentDirectory() {
        return parentDirectory;
    }

    @Override
    public int getBlockChainHead() {
        return entry.getBlockChainHead();
    }

    IBlockManager getBlockManger() {
        return entry.getBlockManager();
    }

    static <TItem extends IInternalDirectoryItem, TEntry extends IDirectoryEntry<TItem>> TItem createItem(String name,
                                                                                                          IInternalDirectory parentDirectory,
                                                                                                          IBlockManager blockManager,
                                                                                                          IEntryCreator<TItem, TEntry> creator,
                                                                                                          String errorMessage)
            throws FileFileSystemException {

        return ErrorHandlingHelper.get(() -> creator.create(name, blockManager), errorMessage).getItem(parentDirectory);
    }
}
