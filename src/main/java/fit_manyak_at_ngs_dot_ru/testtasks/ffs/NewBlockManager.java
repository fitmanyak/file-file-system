package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 26.02.2017.
 */

public class NewBlockManager {
    private static final int BLOCK_SIZE = 512;

    public class BlockFile {
        private long size;
        private int blockChainHead;

        private long position;
        private int inBlockPosition;
        private int blockIndex;
        private final ByteBuffer buffer;
        private boolean bufferValid;

        private BlockFile(long size, int blockChainHead) {
            this.size = size;
            this.blockChainHead = blockChainHead;

            this.position = 0L;
            this.inBlockPosition = 0;
            this.blockIndex = this.blockChainHead;
            this.buffer = ByteBuffer.allocateDirect(BLOCK_SIZE);
            this.bufferValid = false;
        }

        public long getSize() {
            return size;
        }

        public void setCalculatedSize(long calculatedSize) {
            size = calculatedSize;
            if (position > size) {
                position = size;
                inBlockPosition = (int) (position % BLOCK_SIZE);
                invalidateBuffer();
            }
        }

        private void invalidateBuffer() {
            buffer.clear();
            bufferValid = false;
        }

        public int getBlockChainHead() {
            return blockChainHead;
        }

        public int read(ByteBuffer destination) throws IOException, IllegalArgumentException {
            if (destination.isReadOnly()) {
                throw new IllegalArgumentException("Read-only buffer");// TODO
            }

            int destinationRemaining = destination.remaining();
            int totalRead = 0;
            while (destinationRemaining != 0 && position != size) {
                readToBuffer();

                int inBlockRemaining = BLOCK_SIZE - inBlockPosition;
                if (inBlockRemaining > destinationRemaining) {
                    byte[] b = buffer.array();
                    destination.put(b, inBlockPosition, destinationRemaining);

                    position += destinationRemaining;
                    inBlockPosition += destinationRemaining;
                    totalRead += destinationRemaining;
                    destinationRemaining = 0;

                    buffer.position(inBlockPosition);
                }
                else {
                    destination.put(buffer);

                    position += inBlockRemaining;
                    inBlockPosition = 0;
                    totalRead += inBlockRemaining;
                    destinationRemaining -= inBlockRemaining;

                    invalidateBuffer();
                }
            }

            return totalRead;
        }

        private void readToBuffer() throws IOException {
            if (!bufferValid) {
                boolean blockStart = (inBlockPosition == 0);
                if (position != 0 && blockStart) {
                    blockIndex = getNextBlockIndex(blockIndex);
                }

                try {
                    readBlock(blockIndex, buffer);
                    bufferValid = true;
                }
                catch (Throwable t) {
                    invalidateBuffer();

                    throw  t;
                }

                if (!blockStart) {
                    buffer.position(inBlockPosition);
                }
            }
        }

        public int write(ByteBuffer source) throws IOException {
            return 0;// TODO
        }
    }

    private int getNextBlockIndex(int blockIndex) throws IOException {
        return 0;// TODO
    }

    private void readBlock(int blockIndex, ByteBuffer buffer) throws IOException {
        // TODO
    }

    public BlockFile createBlockFile(long size, int blockChainHead) {
        return new BlockFile(size, blockChainHead);
    }
}
