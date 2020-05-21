package cn.sanbu.avalon.media;

public class BitrateStatist {
    public static final int MAXIMUM_CACHED_PACKET = 128;

    private class _Packet {
        int size = 0;
        long timestamp = -1;
        void reset() { size = 0; timestamp = -1; }
    }

    private _Packet mPackets[] = new _Packet[MAXIMUM_CACHED_PACKET];
    private int mIter;
    private long mTotalPacketSize;
    private long mTotalPacketCount;

    public BitrateStatist() {
        for (int i = 0; i < MAXIMUM_CACHED_PACKET; ++i) {
            mPackets[i] = new _Packet();
        }
    }

    public void incomingPacket(int payloadSize) {
        mPackets[mIter].size = payloadSize;
        mPackets[mIter].timestamp = System.currentTimeMillis();
        if (++mIter == MAXIMUM_CACHED_PACKET)
            mIter = 0;

        mTotalPacketSize += payloadSize;
        mTotalPacketCount++;
    }

    public void incomingPacket(int payloadSize, long timestampMs) {
        mPackets[mIter].size = payloadSize;
        mPackets[mIter].timestamp = timestampMs;
        if (++mIter == MAXIMUM_CACHED_PACKET)
            mIter = 0;

        mTotalPacketSize += payloadSize;
        mTotalPacketCount++;
    }

    public long averageBitrate() {
        long bitrate = 0;

        if (mPackets[mIter].size > 0 && mPackets[mIter].timestamp > 0) {
            long oldestTs = mPackets[mIter].timestamp;
            int latestIter = (mIter + MAXIMUM_CACHED_PACKET - 1) % MAXIMUM_CACHED_PACKET;
            long latestTs = mPackets[latestIter].timestamp;
            if (latestTs > 0 && System.currentTimeMillis() - latestTs < 1000) {
                long totalSize = 0;
                for (int i = 0; i < MAXIMUM_CACHED_PACKET; ++i) {
                    totalSize += mPackets[i].size;
                }

                long duration = latestTs - oldestTs;
                bitrate = totalSize * 1000 /* ms */ * 8 /* bit */ / duration;
            }
        }

        return bitrate;
    }

    public long totalPacketSize() {
        return mTotalPacketSize;
    }

    public long totalPacketCount() {
        return mTotalPacketCount;
    }

    public void reset() {
        for (int i = 0; i < MAXIMUM_CACHED_PACKET; ++i) {
            mPackets[i].reset();
        }

        mIter = 0;
        mTotalPacketSize = 0;
        mTotalPacketCount = 0;
    }
}
