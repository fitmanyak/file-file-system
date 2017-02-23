package fit_manyak_at_ngs_dot_ru.testtasks.ffs;

import fit_manyak_at_ngs_dot_ru.testtasks.ffs.messages.Messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author Ivan Buryak {@literal fit_manyak@ngs.ru}
 *         Created on 19.02.2017.
 */

@SuppressWarnings("WeakerAccess")
public class FileFileSystem {
    public static void main(String[] args) {
        try (FileChannel channel = FileChannel.open(Paths.get("test.ffs"),
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {

            ByteBuffer buffer = ByteBuffer.allocateDirect(2 + 1 + 1 + 1);
            buffer.putShort((short) 0xFFF5);
            buffer.put((byte) 0);
            buffer.put((byte) 0);
            buffer.put((byte) 0);
            buffer.flip();

            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(Messages.HELLO_WORLD);
    }
}
