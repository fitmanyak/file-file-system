package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 06.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IDirectoryItem extends ICommonDirectoryEntry, ICloseable {
    public IDirectory getParentDirectory();
}
