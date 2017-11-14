package sanp.tools.utils;

import java.nio.ByteBuffer;

/**
 * Created by Tuyj on 2017/11/14.
 */

public class ByteBufferUtils {

    public static boolean contain(ByteBuffer src, byte[] dst) {
        int length = dst.length;
        if (src.remaining() < length)
            return false;

        int pos = src.position();
        for (int i=0; i<length; i++) {
            if (dst[i] != src.get()) {
                src.position(pos);
                return false;
            }
        }

        src.position(pos);
        return true;
    }

    public static void move(ByteBuffer buf, int offset) {
        /*
        if(offset == 0) { // nothing to do
            return;
        } else if(offset > 0) { // move to right with offset

        } else {  // move to left with (0-offset)
            offset = 0 - offset;
            if(buf.position() >= offset) { // move and keep the whole data
                byte[] tmp = new byte[buf.remaining()];
            } else { // overflow
                int overflow = offset - buf.position();
                if(overflow >= buf.remaining()) { // overflow the whole data
                    buf.position(0);
                    buf.limit(0);
                } else { // overflow part of data
                    buf.position(buf.position()+overflow);
                    byte[] tmp = new byte[buf.remaining()];
                    buf.get(tmp);
                    buf.clear();
                    buf.put(tmp);
                    buf.flip();
                }
            }
        }
        */
    }
}
