package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities;

import java.io.IOException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
@FunctionalInterface
public interface ICreatorWithArgument<R, T> {
    public R create(T argument) throws IOException, IllegalArgumentException;
}
