package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.ErrorHandlingHelper;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IActionWithArgument;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IOperationWithArgument;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class File extends DirectoryItem<IInternalFile, IFileDirectoryEntry> implements IInternalFile {
    public File(IFileDirectoryEntry entry, IInternalDirectory parentDirectory) {
        super(entry, parentDirectory);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public IInternalFile getAsFile() throws FileFileSystemException {
        return this;
    }

    @Override
    public IInternalDirectory getAsDirectory() throws FileFileSystemException {
        throw new FileFileSystemException("File isn't directory");// TODO
    }

    @Override
    public long getSize() throws FileFileSystemException {
        return getContent().getSize();
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

    protected static IInternalFile create(String name, IInternalDirectory parentDirectory, IBlockManager blockManager)
            throws FileFileSystemException {

        return createItem(name, parentDirectory, blockManager, FileDirectoryEntry::create, "File create error");// TODO
    }
}
