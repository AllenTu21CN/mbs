package cn.sanbu.avalon.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Packet {
    public long     dts;
    public long     pts;
    public int      mediaType;
    public int      codecType;
    public int      streamIndex;
    public boolean  isKeyFrame;
    public boolean  isBeginOfFrame;
    public boolean  isEndOfFrame;
    public boolean  isAvccNal;
    public int      size;

    public byte[]   data = null;

    public static Packet unserialize(byte[] bytes) {
        if (bytes ==  null || bytes.length < 24) {
            return null;
        }

        Packet pkt = new Packet();
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        pkt.dts = buf.getLong();
        pkt.pts = buf.getLong();

        pkt.mediaType = buf.get();
        pkt.codecType = buf.get();
        pkt.streamIndex = buf.get();

        int flags = buf.get();
        pkt.isKeyFrame = ((flags & 0x01) != 0);
        pkt.isBeginOfFrame = ((flags & 0x02) != 0);
        pkt.isEndOfFrame = ((flags & 0x04) != 0);
        pkt.isAvccNal = ((flags & 0x08) != 0);

        pkt.size = buf.getInt();

        if (pkt.size > 0) {
            pkt.data = Arrays.copyOfRange(bytes, 24, 24 + pkt.size);
        }

        return pkt;
    }

    public byte[] serialize() {
        if (data == null) {
            size = 0;
        } else {
            size = data.length;
        }

        byte[] bytes = new byte[24 + size];
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        buf.putLong(dts)
           .putLong(pts)
           .put((byte)mediaType)
           .put((byte)codecType)
           .put((byte)streamIndex);

        byte flags = 0;
        flags |= (isKeyFrame ? 0x01 : 0x00);       // 0000 0001
        flags |= (isBeginOfFrame ? 0x02 : 0x00);   // 0000 0010
        flags |= (isEndOfFrame ? 0x04 : 0x00);     // 0000 0100
        flags |= (isAvccNal ? 0x08 : 0x00);        // 0000 1000
        buf.put(flags);

        buf.putInt(size);

        // Copy payload
        System.arraycopy(data, 0, bytes, 24, size);

        return bytes;
    }
}
