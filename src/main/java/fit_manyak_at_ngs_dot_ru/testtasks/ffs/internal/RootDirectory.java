package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IDirectory;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.IFile;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class RootDirectory implements IDirectory {
    private final BlockManager blockManager;

    private RootDirectory(BlockManager blockManager) {
        this.blockManager = blockManager;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public IFile createFile(String name) throws IOException, IllegalArgumentException {
        /*long contentSize = entry.getContentSize();
        long newContentSize = contentSize + BlockManager.BLOCK_INDEX_SIZE;
        int contentBlockChainHead = entry.getContentBlockChainHead();
        if (contentSize == 0) {
            contentBlockChainHead = blockManager.allocBlock();
            entry.updateContentData(newContentSize, contentBlockChainHead);
        }
        else {
            entry.updateContentSize(newContentSize);
        }

        DirectoryEntry fileEntry = DirectoryEntry.createFile(name, blockManager);

        ByteBuffer fileEntryBlockChainHead = ByteBuffer.allocateDirect(BlockManager.BLOCK_INDEX_SIZE);
        fileEntryBlockChainHead.putInt(fileEntry.getBlockChainHead());
        blockManager.writeDataInBlock(contentBlockChainHead, contentSize, fileEntryBlockChainHead);

        return new File(fileEntry);*/
        return null;
    }

    @Override
    public IFile openFile(String name) throws IOException, IllegalArgumentException {
        return null;
    }

    public static void format(ByteBuffer block) {
        RootDirectoryDirectoryEntry.format(block);
    }

    public static RootDirectory open(BlockManager blockManager) throws IOException, IllegalArgumentException {
        return new RootDirectory(blockManager);// TODO
    }
}
