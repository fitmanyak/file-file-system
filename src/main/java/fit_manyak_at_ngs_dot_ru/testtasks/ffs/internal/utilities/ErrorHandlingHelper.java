package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

import java.io.Closeable;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public class ErrorHandlingHelper {
    public static <T> T get(ICommonProvider<T> provider, String errorMessage) throws FileFileSystemException {
        try {
            return provider.get();
        } catch (Throwable t) {
            throw new FileFileSystemException(errorMessage, t);
        }
    }

    public static <R, T extends Closeable> R getWithCloseableArgument(IProvider<T> argumentProvider,
                                                                      IProviderWithArgument<R, T> provider)
            throws FileFileSystemException {

        T argument = null;
        try {
            argument = argumentProvider.get();

            return provider.get(argument);
        } catch (Throwable finalThrowable) {
            if (argument != null) {
                try {
                    argument.close();
                } catch (Throwable t) {
                    finalThrowable.addSuppressed(t);
                }
            }

            throw finalThrowable;
        }
    }

    public static <R, T extends Closeable> R getWithCloseableArgument(ICommonProvider<T> argumentProvider,
                                                                      String argumentGetErrorMessage,
                                                                      IProviderWithArgument<R, T> provider)
            throws FileFileSystemException {

        return getWithCloseableArgument(() -> get(argumentProvider, argumentGetErrorMessage), provider);
    }

    public static void performAction(ICommonAction action, String errorMessage) throws FileFileSystemException {
        try {
            action.perform();
        } catch (Throwable t) {
            throw new FileFileSystemException(errorMessage, t);
        }
    }

    public static <T extends Closeable> void performActionWithCloseableArgument(ICommonProvider<T> argumentProvider,
                                                                                String argumentGetErrorMessage,
                                                                                IActionWithArgument<T> action,
                                                                                String customCloseErrorMessage)
            throws FileFileSystemException {

        performActionWithCloseableArgument(() -> get(argumentProvider, argumentGetErrorMessage), action,
                customCloseErrorMessage);
    }

    private static <T extends Closeable> void performActionWithCloseableArgument(IProvider<T> argumentProvider,
                                                                                 IActionWithArgument<T> action,
                                                                                 String customCloseErrorMessage)
            throws FileFileSystemException {

        Throwable finalThrowable = null;
        T argument = null;
        try {
            argument = argumentProvider.get();
            action.perform(argument);
        } catch (Throwable t) {
            finalThrowable = t;

            throw t;
        } finally {
            if (argument != null) {
                try {
                    argument.close();
                } catch (Throwable t) {
                    if (finalThrowable != null) {
                        finalThrowable.addSuppressed(t);
                    } else {
                        throw new FileFileSystemException(customCloseErrorMessage, t);
                    }
                }
            }
        }
    }

    public static int performOperation(IOperation operation, String errorMessage) throws FileFileSystemException {
        try {
            return operation.perform();
        } catch (Throwable t) {
            throw new FileFileSystemException(errorMessage, t);
        }
    }
}
