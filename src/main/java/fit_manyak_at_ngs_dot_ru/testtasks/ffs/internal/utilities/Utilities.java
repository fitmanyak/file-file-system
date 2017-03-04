package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class Utilities {
    public static <T> T create(ICreator<T> creator, String errorMessage) throws IOException, IllegalArgumentException {
        try {
            return creator.create();
        } catch (Throwable t) {
            throw new FileFileSystemException(errorMessage, t);
        }
    }

    public static <R, T extends Closeable> R createWithCloseableArgument(ICreator<T> argumentCreator,
                                                                         ICreatorWithArgument<R, T> creator)
            throws IOException, IllegalArgumentException {

        T argument = null;
        try {
            argument = argumentCreator.create();

            return creator.create(argument);
        } catch (Throwable t) {
            if (argument != null) {
                try {
                    argument.close();
                } catch (Throwable closeThrowable) {
                    t.addSuppressed(closeThrowable);
                }
            }

            throw t;
        }
    }

    public static void readAndFlipBuffer(ByteBuffer destination, INoReturnValueIOOperation readOperation)
            throws IOException, IllegalArgumentException {

        readOperation.perform(destination);

        destination.flip();
    }

    public static void flipBufferAndWrite(ByteBuffer source, INoReturnValueIOOperation writeOperation)
            throws IOException, IllegalArgumentException {

        source.flip();

        writeOperation.perform(source);

        source.clear();
    }

    public static void flipBufferAndWrite(ByteBuffer source, INoReturnValueIOOperation writeOperation,
                                          String errorMessage) throws IOException, IllegalArgumentException {

        performIOAction(() -> flipBufferAndWrite(source, writeOperation), errorMessage);
    }

    private static void performIOAction(IIOAction action, String errorMessage) throws FileFileSystemException {
        try {
            action.perform();
        } catch (Throwable t) {
            throw new FileFileSystemException(errorMessage, t);
        }
    }
}
