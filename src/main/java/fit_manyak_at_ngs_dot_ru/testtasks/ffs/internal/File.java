package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFile;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IActionWithArgument;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOperationWithArgument;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class File implements IFile {
    private final IFileDirectoryEntry entry;

    private final IParentDirectory parentDirectory;

    private File(IFileDirectoryEntry entry, IParentDirectory parentDirectory) {
        this.entry = entry;

        this.parentDirectory = parentDirectory;
    }

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public void setName(String newName) throws FileFileSystemException {
        ErrorHandlingHelper.performAction(() -> entry.setName(newName), "File name change error");// TODO
    }

    @Override
    public void remove() throws FileFileSystemException {
        ErrorHandlingHelper.performAction(this::performRemove, "File remove error");// TODO
    }

    private void performRemove() throws FileFileSystemException {
        int entryBlockChainHead = entry.getBlockChainHead();
        entry.remove();

        parentDirectory.removeItem(entryBlockChainHead);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isEmpty() throws FileFileSystemException {
        return ErrorHandlingHelper.get(entry::isEmpty, "File empty check error");// TODO
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
    public long getSize() throws FileFileSystemException {
        return getContent().getSize();
    }

    private IDirectFile getContent() throws FileFileSystemException {
        return ErrorHandlingHelper.get(entry::getContent, "File get content error");// TODO
    }

    @Override
    public void setSize(long newSize) throws FileFileSystemException {
        performAction(content -> content.setSize(newSize), "File change size error");// TODO
    }

    private void performAction(IActionWithArgument<IDirectFile> action, String errorMessage)
            throws FileFileSystemException {

        IDirectFile content = getContent();
        ErrorHandlingHelper.performAction(() -> action.perform(content), errorMessage);
    }

    @Override
    public long getPosition() throws FileFileSystemException {
        return getContent().getPosition();
    }

    @Override
    public void setPosition(long newPosition) throws FileFileSystemException {
        performAction(content -> content.setPosition(newPosition), "File change position error");// TODO
    }

    @Override
    public void reset() throws FileFileSystemException {
        getContent().reset();
    }

    @Override
    public int read(ByteBuffer destination) throws FileFileSystemException {
        return performReadOperation(content -> content.read(destination));
    }

    private int performReadOperation(IOperationWithArgument<IDirectFile> readOperation) throws FileFileSystemException {
        return performOperation(readOperation, "File read error");// TODO
    }

    private int performOperation(IOperationWithArgument<IDirectFile> operation, String errorMessage)
            throws FileFileSystemException {

        IDirectFile content = getContent();
        return ErrorHandlingHelper.performOperation(() -> operation.perform(content), errorMessage);
    }

    @Override
    public int read(long newPosition, ByteBuffer destination) throws FileFileSystemException {
        return performReadOperation(content -> content.read(newPosition, destination));
    }

    @Override
    public int write(ByteBuffer source) throws FileFileSystemException {
        return performWriteOperation(content -> content.write(source));
    }

    private int performWriteOperation(IOperationWithArgument<IDirectFile> writeOperation)
            throws FileFileSystemException {

        return performOperation(writeOperation, "File write error");// TODO
    }

    @Override
    public int write(long newPosition, ByteBuffer source) throws FileFileSystemException {
        return performWriteOperation(content -> content.write(newPosition, source));
    }
}
