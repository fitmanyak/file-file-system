package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.FileFileSystemException;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
@FunctionalInterface
public interface IOperationWithArgument<T> extends ICommonOperationWithArgument<T> {
    @Override
    public int perform(T argument) throws FileFileSystemException;
}
