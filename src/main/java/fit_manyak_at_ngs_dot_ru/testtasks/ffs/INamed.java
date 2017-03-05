package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 05.03.2017.
 */

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface INamed {
    public String getName();
    public void setName(String newName) throws FileFileSystemException;
}
