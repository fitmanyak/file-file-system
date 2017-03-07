package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 07.03.2017.
 */

public class Position {
    private long position;
    private int blockIndex;

    public Position(int blockChainHead) {
        reset(blockChainHead);
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public void reset(int blockChainHead) {
        position = 0L;
        blockIndex = blockChainHead;
    }

    public void set(Position original) {
        position = original.position;
        blockIndex = original.blockIndex;
    }
}
