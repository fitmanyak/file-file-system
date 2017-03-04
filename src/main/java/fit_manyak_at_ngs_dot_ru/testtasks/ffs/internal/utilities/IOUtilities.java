package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class IOUtilities {

    public static void readAndFlipBuffer(ByteBuffer destination, INoReturnValueIOOperation readOperation)
            throws IOException, IllegalArgumentException {

        readOperation.perform(destination);

        destination.flip();
    }

    public static void readAndFlipBuffer(ByteBuffer destination, INoReturnValueIOOperation readOperation,
                                         String errorMessage) throws FileFileSystemException {

        performIOAction(() -> readAndFlipBuffer(destination, readOperation), errorMessage);
    }

    public static void flipBufferAndWrite(ByteBuffer source, INoReturnValueIOOperation writeOperation)
            throws IOException, IllegalArgumentException {

        source.flip();

        writeOperation.perform(source);

        source.clear();
    }

    public static void flipBufferAndWrite(ByteBuffer source, INoReturnValueIOOperation writeOperation,
                                          String errorMessage) throws FileFileSystemException {

        performIOAction(() -> flipBufferAndWrite(source, writeOperation), errorMessage);
    }

    public static void performIOAction(IIOAction action, String errorMessage) throws FileFileSystemException {
        try {
            action.perform();
        } catch (Throwable t) {
            throw new FileFileSystemException(errorMessage, t);
        }
    }
}
