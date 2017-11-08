package sanp.javalon.media.base;

import android.media.MediaFormat;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import sanp.tools.utils.LogManager;

public class AVChannel {
    private MediaFormat mMediaFormat;
    private ByteBuffer mMetaData;

    private LinkedBlockingQueue<AVPacket> mBusyQueue = null;
    private LinkedBlockingQueue<AVPacket> mIdleQueue = null;

    public AVChannel(int bufferedPacketCount) {
        mMediaFormat = null;

        initQueue(bufferedPacketCount, -1);
    }

    public AVChannel(MediaFormat mediaFormat, int bufferedPacketCount) {
        mMediaFormat = mediaFormat;

        initQueue(bufferedPacketCount, -1);
    }

    public AVChannel(int bufferedPacketCount, int buffMaxSize) {
        mMediaFormat = null;

        initQueue(bufferedPacketCount, buffMaxSize);
    }

    private void initQueue(int bufferedPacketCount, int buffMaxSize) {
        mBusyQueue = new LinkedBlockingQueue<>();
        mIdleQueue = new LinkedBlockingQueue<>();

        for (int i = 0; i < bufferedPacketCount; ++i) {
            try {
                if(buffMaxSize > 0)
                    mIdleQueue.add(new AVPacket(buffMaxSize));
                else
                    mIdleQueue.add(new AVPacket());
            } catch (Exception e) {
                LogManager.e(e);
            }
        }
    }

    public MediaFormat getMediaFormat() {
        return mMediaFormat;
    }

    public void setMediaFormat(MediaFormat mediaFormat) {
        mMediaFormat = mediaFormat;
    }

    public ByteBuffer getMetaData() {
        return mMetaData;
    }

    public void setMetaData(ByteBuffer metaData) {
        mMetaData = metaData;
    }

    public AVPacket takeIdlePacket() {
        try {
            return mIdleQueue.take();
        } catch (Exception e) {
            LogManager.e(e);
        }

        return null;
    }

    public AVPacket pollIdlePacket(long timeoutMs) throws InterruptedException {
        return mIdleQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public AVPacket pollIdlePacket() {
        return mIdleQueue.poll();
    }

    public boolean putIdlePacket(AVPacket packet) {
        try {
            mIdleQueue.put(packet);
            return true;
        } catch (Exception e) {
            LogManager.e(e);
        }
        return false;
    }

    public boolean offerIdlePacket(AVPacket packet, long timeout_ms)
            throws InterruptedException {
        return mIdleQueue.offer(packet, timeout_ms, TimeUnit.MILLISECONDS);
    }

    public boolean offerIdlePacket(AVPacket packet) {
        return mIdleQueue.offer(packet);
    }

    public AVPacket takeBusyPacket() {
        try {
            return mBusyQueue.take();
        } catch (Exception e) {
            LogManager.e(e);
        }
        return null;
    }
    public AVPacket pollBusyPacket(long timeoutMs) throws InterruptedException {
        return mBusyQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public AVPacket pollBusyPacket() {
        return mBusyQueue.poll();
    }

    public void putBusyPacket(AVPacket packet) {
        try {
            mBusyQueue.put(packet);
        } catch (Exception e) {
            LogManager.e(e);
        }
    }

    public boolean offerBusyPacket(AVPacket packet, long timeoutMs)
            throws InterruptedException {
        return mBusyQueue.offer(packet, timeoutMs, TimeUnit.MILLISECONDS);
    }

    public boolean offerBusyPacket(AVPacket packet) {
        return mBusyQueue.offer(packet);
    }

    public void clearBusyPackets() {
        while(mBusyQueue.size() > 0) {
            AVPacket packet = takeBusyPacket();
            if(packet != null) {
                packet.reset();
                putIdlePacket(packet);
            }
        }
    }
}
