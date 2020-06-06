package cn.lx.media;

public class FrameRateStatist {
    public static final int MAXIMUM_CACHED_FRAMES = 60;

    private long mFrames[] = new long[MAXIMUM_CACHED_FRAMES];
    private int mIter;
    private long mTotalFrameCount;

    public FrameRateStatist() {
        reset();
    }

    public void incomingFrame() {
        mFrames[mIter] = System.currentTimeMillis();
        if (++mIter == MAXIMUM_CACHED_FRAMES)
            mIter = 0;

        mTotalFrameCount++;
    }

    public void incomingFrame(long timestampMs) {
        mFrames[mIter] = timestampMs;
        if (++mIter == MAXIMUM_CACHED_FRAMES)
            mIter = 0;

        mTotalFrameCount++;
    }

    public float averageFrameRate() {
        float frameRate = 0;
        long oldestTs = mFrames[mIter];
        if (oldestTs > 0) {
            int latestIter = (mIter + MAXIMUM_CACHED_FRAMES - 1) % MAXIMUM_CACHED_FRAMES;
            long latestTs = mFrames[latestIter];
            if (latestTs > 0 && System.currentTimeMillis() - latestTs < 1000) {
                long duration = latestTs - oldestTs;
                frameRate = (float)(MAXIMUM_CACHED_FRAMES - 2) * 1000.0f / (float)duration;
            }
        }

        return frameRate;
    }

    public long lastFrameTimestamp() {
        if (mTotalFrameCount > 0) {
            int latestIter = (mIter + MAXIMUM_CACHED_FRAMES - 1) % MAXIMUM_CACHED_FRAMES;
            return mFrames[latestIter];
        } else {
            return -1;
        }
    }

    public long totalFrameCount() {
        return mTotalFrameCount;
    }

    public void reset()
    {
        for (int i = 0; i < MAXIMUM_CACHED_FRAMES; ++i) {
            mFrames[i] = 0;
        }
        mIter = 0;
        mTotalFrameCount = 0;
    }
}
