package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class Utilities {
    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    public interface ICreator<T> {
        public T create() throws IOException, IllegalArgumentException;
    }

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @FunctionalInterface
    public interface ICreatorWithArgument<R, T> {
        public R create(T argument) throws IOException, IllegalArgumentException;
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
}
