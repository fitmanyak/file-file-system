package fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IIOAction;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.IIOOperation;
import fit_manyak_at_ngs_dot_ru.testtasks.ffs.internal.utilities.Utilities;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 04.03.2017.
 */

public abstract class NewDirectoryEntry {
    private static final int FLAGS_SIZE = 4;

    private static final int CONTENT_DATA_SIZE = BlockManager.CONTENT_SIZE_SIZE + BlockManager.BLOCK_INDEX_SIZE;
    private static final long CONTENT_DATA_POSITION = FLAGS_SIZE;

    private class Content implements IBlockFile {
        private final IBlockFile delegate;

        private Content(IBlockFile delegate) {
            this.delegate = delegate;
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public void setSize(long newSize) throws IOException, IllegalArgumentException {
            performUpdatedContentDataIOAction(() -> delegate.setSize(newSize));
        }

        private void performUpdatedContentDataIOAction(IIOAction action) throws IOException, IllegalArgumentException {
            action.perform();

            updateContentData();
        }

        @Override
        public long getPosition() {
            return delegate.getPosition();
        }

        @Override
        public void setPosition(long newPosition) throws IOException, IllegalArgumentException {
            delegate.setPosition(newPosition);
        }

        @Override
        public void reset() {
            delegate.reset();
        }

        @Override
        public void clear() throws IOException, IllegalArgumentException {
            performUpdatedContentDataIOAction(delegate::clear);
        }

        @Override
        public int read(ByteBuffer destination) throws IOException, IllegalArgumentException {
            return delegate.read(destination);
        }

        @Override
        public int read(long newPosition, ByteBuffer destination) throws IOException, IllegalArgumentException {
            return delegate.read(newPosition, destination);
        }

        @Override
        public int write(ByteBuffer source) throws IOException, IllegalArgumentException {
            return performUpdatedContentDataIOOperation(source, delegate::write);
        }

        private int performUpdatedContentDataIOOperation(ByteBuffer buffer, IIOOperation operation)
                throws IOException, IllegalArgumentException {

            int result = operation.perform(buffer);

            updateContentData();

            return result;
        }

        @Override
        public int write(long newPosition, ByteBuffer source) throws IOException, IllegalArgumentException {
            return performUpdatedContentDataIOOperation(source, src -> delegate.write(newPosition, src));
        }

        @Override
        public int getBlockChainHead() {
            return delegate.getBlockChainHead();
        }
    }

    private final IBlockFile entry;

    private final IBlockFile content;
    private long contentSize;
    private int contentBlockChainHead;
    private ByteBuffer contentData;

    private NewDirectoryEntry(IBlockFile entry, IBlockFile content) {
        this.entry = entry;

        this.content = content;
        this.contentSize = content.getSize();
        this.contentBlockChainHead = content.getBlockChainHead();
        this.contentData = ByteBuffer.allocateDirect(CONTENT_DATA_SIZE);
    }

    private void updateContentData() throws IOException, IllegalArgumentException {
        long newContentSize = content.getSize();
        int newContentBlockChainHead = content.getBlockChainHead();
        boolean contentBlockChainHeadChanged = newContentBlockChainHead != contentBlockChainHead;
        if (newContentSize != contentSize || contentBlockChainHeadChanged) {
            contentData.putLong(newContentSize);

            if (contentBlockChainHeadChanged) {
                contentData.putInt(newContentBlockChainHead);
            }

            Utilities.performIOAction(
                    () -> Utilities.flipBufferAndWrite(contentData, src -> entry.write(CONTENT_DATA_POSITION, src)),
                    "Content data write error");// TODO

            contentSize = newContentSize;
            contentBlockChainHead = newContentBlockChainHead;
        }
    }
}
