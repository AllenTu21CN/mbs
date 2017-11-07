package sanp.avalon.libs.media.base;

import java.nio.ByteBuffer;
import sanp.avalon.libs.media.base.AVDefines.DataType;
import sanp.avalon.libs.media.base.AVDefines.DataFlag;

public class AVPacket {
    public static final int TIMESTAMP_NO_VALUE = -1;
    public static final int MAXIMUM_PAYLOAD_LENGTH = 1920 * 1080 * 3 / 2; // 1080p yuv420

    private long mPts = TIMESTAMP_NO_VALUE;
    private long mDts = TIMESTAMP_NO_VALUE;

    private DataType mMediaType = DataType.UNKNOWN;
    private DataFlag mDataFlag = DataFlag.NONE;

    private int mTrackIndex = -1;

    private boolean mIsKey = false;
    
    private ByteBuffer mPayload;

    private int mCodecFlags = 0; //refer to MediaCodec.BufferInfo.flags
    
    public AVPacket() {
        mPayload = ByteBuffer.allocate(MAXIMUM_PAYLOAD_LENGTH);
    }

    public AVPacket(int payloadLength) {
        mPayload = ByteBuffer.allocate(payloadLength);
    }

    public void reset() {
        mPts = mDts = TIMESTAMP_NO_VALUE;
        mMediaType = DataType.UNKNOWN;
        mDataFlag = DataFlag.NONE;
        mTrackIndex = -1;
        mIsKey = false;
        mCodecFlags = 0;
        mPayload.clear();
    }
    
    public long getPts() {
        return mPts;
    }

    public long getDts() {
        return mDts;
    }

    public DataType getMediaType() {
        return mMediaType;
    }

    public int getTrackIndex() {
        return mTrackIndex;
    }

    public ByteBuffer getPayload() {
        return mPayload;
    }

    public void setPts(long pts) {
        mPts = pts;
    }

    public void setDts(long dts) {
        mDts = dts;
    }

    public void setMediaType(DataType mediaType) {
        mMediaType = mediaType;
    }

    public void setTrackIndex(int trackIndex) {
        mTrackIndex = trackIndex;
    }
    
    public boolean isEmpty() {
        if(mPts != TIMESTAMP_NO_VALUE)
            return false;
        if(mDts != TIMESTAMP_NO_VALUE)
            return false;
        if(mPayload.limit() != mPayload.capacity())
            return false;
        return true;
    }
    
    public boolean isKeyFrame() {
        return mIsKey;
    }
    
    public void setIsKeyFrame(boolean v) {
        mIsKey = v;
    }
    
    public DataFlag getDataFlag() {
        return mDataFlag;
    }
    
    public void setDataFlag(DataFlag flag) {
        mDataFlag = flag;
    }
    
    public int getCodecFlags() {
        return mCodecFlags;
    }
    
    public void setCodecFlags(int flags) {
        mCodecFlags = flags;
    }
}
