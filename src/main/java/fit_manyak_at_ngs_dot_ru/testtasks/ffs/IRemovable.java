package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 05.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
@FunctionalInterface
public interface IRemovable {
    public void remove() throws FileFileSystemException;
}
