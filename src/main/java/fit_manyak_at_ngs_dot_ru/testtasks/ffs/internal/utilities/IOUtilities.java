package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class IOUtilities {
    public static void readAndFlipBuffer(ByteBuffer destination, IActionWithArgument<ByteBuffer> readAction)
            throws FileFileSystemException {

        readAction.perform(destination);

        destination.flip();
    }

    public static void readAndFlipBuffer(ByteBuffer destination, IActionWithArgument<ByteBuffer> readAction,
                                         String errorMessage) throws FileFileSystemException {

        ErrorHandlingHelper.performAction(() -> readAction.perform(destination), errorMessage);
    }

    public static ByteBuffer createReadAndFlipBuffer(int size, IActionWithArgument<ByteBuffer> readAction)
            throws FileFileSystemException {

        ByteBuffer destination = ByteBuffer.allocateDirect(size);
        readAndFlipBuffer(destination, readAction);

        return destination;
    }

    public static ByteBuffer createReadAndFlipBuffer(int size, IActionWithArgument<ByteBuffer> readAction,
                                                     String errorMessage) throws FileFileSystemException {

        return ErrorHandlingHelper.get(() -> createReadAndFlipBuffer(size, readAction), errorMessage);
    }

    public static void flipBufferAndWrite(ByteBuffer source, IActionWithArgument<ByteBuffer> writeAction)
            throws FileFileSystemException {

        source.flip();

        writeAction.perform(source);

        source.clear();
    }

    public static void flipBufferAndWrite(ByteBuffer source, IActionWithArgument<ByteBuffer> writeAction,
                                          String errorMessage) throws FileFileSystemException {

        ErrorHandlingHelper.performAction(() -> flipBufferAndWrite(source, writeAction), errorMessage);
    }
}
