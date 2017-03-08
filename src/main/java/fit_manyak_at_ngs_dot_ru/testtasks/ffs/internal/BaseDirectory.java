package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectoryItem;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFile;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.messages.Messages;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IActionWithArgument;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOUtilities;

import java.nio.ByteBuffer;
import java.util.ArrayList;
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

    private static class SubEntryIterationContext {
        private final IDirectFile content;
        private final long count;

        private long index;
        private int blockChainHead;

        private IDirectoryEntry<? extends IInternalDirectoryItem> entry;

        private SubEntryIterationContext(IDirectFile content, long count) {
            this.content = content;
            this.count = count;

            this.blockChainHead = IBlockManager.NULL_BLOCK_INDEX;
        }
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    private interface ISubEntryVisitor {
        public boolean visit(SubEntryIterationContext context) throws FileFileSystemException;
    }

    private final ByteBuffer subEntryBlockChainHead;

    private final Position writePosition;
    private final Position readPosition;

    protected BaseDirectory(TEntry entry, IInternalDirectory parentDirectory) {
        super(entry, parentDirectory);

        subEntryBlockChainHead = ByteBuffer.allocateDirect(IBlockManager.BLOCK_INDEX_SIZE);

        int blockChainHead = entry.getBlockChainHead();
        writePosition = new Position(blockChainHead);
        readPosition = new Position(blockChainHead);
    }

    @Override
    public void remove() throws FileFileSystemException {
        if (!isEmpty()) {
            throw new FileFileSystemException(Messages.CANT_REMOVE_NOT_EMPTY_DIRECTORY_ERROR);
        }

        super.remove();
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public IInternalFile getAsFile() throws FileFileSystemException {
        throw new FileFileSystemException(Messages.DIRECTORY_NOT_FILE_ERROR);
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
        writeSubEntryBlockChainHead(item.getBlockChainHead(), content::write);

        return item;
    }

    private void writeSubEntryBlockChainHead(int blockChainHead, IActionWithArgument<ByteBuffer> writeAction)
            throws FileFileSystemException {

        subEntryBlockChainHead.putInt(blockChainHead);
        IOUtilities.flipBufferAndWrite(subEntryBlockChainHead, writeAction, Messages.DIRECTORY_CONTENT_WRITE_ERROR);
    }

    @Override
    public IFile openFile(String name) throws FileFileSystemException {
        return openItemInternal(name).getAsFile();
    }

    private IInternalDirectoryItem openItemInternal(String name) throws FileFileSystemException {
        SubEntryIterationContext context = findSubEntryByName(name);
        if (context.entry == null) {
            throw new FileFileSystemException(String.format(Messages.DIRECTORY_SUB_ENTRY_MISSING_ERROR, name));
        }

        return context.entry.getItem(this);
    }

    private SubEntryIterationContext findSubEntryByName(String name) throws FileFileSystemException {
        DirectoryEntry.checkNameNotEmpty(name);

        return iterateOverSubEntries(context -> checkSubEntryNameEquals(name, context));
    }

    private SubEntryIterationContext iterateOverSubEntries(ISubEntryVisitor visitor) throws FileFileSystemException {
        IDirectFile content = getContent();
        content.reset();
        content.savePosition(writePosition);

        SubEntryIterationContext context =
                new SubEntryIterationContext(content, (content.getSize() >> IBlockManager.BLOCK_INDEX_SIZE_EXPONENT));
        iterateOverSubEntries(context, visitor);

        return context;
    }

    private void iterateOverSubEntries(SubEntryIterationContext context, ISubEntryVisitor visitor)
            throws FileFileSystemException {
        for (; context.index < context.count; context.index++) {
            IOUtilities
                    .readAndFlipBuffer(subEntryBlockChainHead, context.content::read, "Directory content read error");
            context.blockChainHead = subEntryBlockChainHead.getInt();
            subEntryBlockChainHead.clear();

            if (visitor.visit(context)) {
                break;
            }
        }
    }

    private boolean checkSubEntryNameEquals(String name, SubEntryIterationContext context)
            throws FileFileSystemException {
        IDirectoryEntry<? extends IInternalDirectoryItem> entry = getEntry(context);
        if (entry.getName().equals(name)) {
            context.entry = entry;

            return true;
        }

        return false;
    }

    private IDirectoryEntry<? extends IInternalDirectoryItem> getEntry(SubEntryIterationContext context)
            throws FileFileSystemException {

        return ErrorHandlingHelper.get(() -> DirectoryEntry.openAny(context.blockChainHead, getBlockManger()),
                Messages.DIRECTORY_SUB_ENTRY_OPEN_ERROR);
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
        SubEntryIterationContext context = findSubEntryByName(name);
        if (context.entry != null) {
            throw new FileFileSystemException(String.format(Messages.DIRECTORY_SUB_ENTRY_EXISTS_ERROR, name));
        }

        return context.content;
    }

    @Override
    public void removeItem(int removedEntryBlockChainHead) throws FileFileSystemException {
        if (removedEntryBlockChainHead == IBlockManager.NULL_BLOCK_INDEX) {
            throw new FileFileSystemException(Messages.BAD_REMOVED_ENTRY_BLOCK_CHAIN_HEAD_ERROR);
        }

        SubEntryIterationContext context =
                iterateOverSubEntries(ctx -> checkSubEntryBlockChainHeadEquals(removedEntryBlockChainHead, ctx));
        if (context.index == context.count) {
            throw new FileFileSystemException(Messages.REMOVED_ENTRY_MISSING_ERROR);
        }

        context.index++;

        iterateOverSubEntries(context, this::shiftSubEntryBlockChainHeadLeft);

        context.content.setSize(context.content.getSize() - IBlockManager.BLOCK_INDEX_SIZE);
    }

    private boolean checkSubEntryBlockChainHeadEquals(int entryBlockChainHead,
                                                      SubEntryIterationContext context) {

        if (context.blockChainHead == entryBlockChainHead) {
            return true;
        }

        context.content.savePosition(writePosition);

        return false;
    }

    @SuppressWarnings("SameReturnValue")
    private boolean shiftSubEntryBlockChainHeadLeft(SubEntryIterationContext context) throws FileFileSystemException {
        context.content.savePosition(readPosition);

        writeSubEntryBlockChainHead(context.blockChainHead, source -> context.content.write(writePosition, source));

        context.content.savePosition(writePosition);

        context.content.setPosition(readPosition);

        return false;
    }

    @Override
    public Collection<String> getNames() throws FileFileSystemException {
        Collection<String> names = new ArrayList<>();
        iterateOverSubEntries(context -> names.add(getEntry(context).getName()));

        return names;
    }

    @Override
    protected IDirectFile getContent() throws FileFileSystemException {
        IDirectFile content = super.getContent();
        if ((content.getSize() & BLOCK_INDEX_SIZE_MINUS_ONE) != 0L) {
            throw new FileFileSystemException(Messages.BAD_DIRECTORY_CONTENT_SIZE_ERROR);
        }

        return content;
    }
}
