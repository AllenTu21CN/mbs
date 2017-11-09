package sanp.test;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import sanp.javalon.media.audio.AudioCapturer;
import sanp.javalon.media.audio.AudioEncoder;
import sanp.javalon.media.audio.AudioPlayTesting;
import sanp.javalon.network.protocol.RTMPBandwidthTest;
import sanp.mpx.MediaController;
import sanp.mpx.MediaEngine;

/**
 * Created by Tuyj on 2017/6/7.
 */

public class SimpleTesting {
    static public final boolean Enabled = false;

    public interface Tester {
        void start(Object obj);
        void next();
    }

    static private SimpleTesting mAdvancedTester = null;
    public static SimpleTesting getInstance() {
        if (mAdvancedTester == null) {
            synchronized (SimpleTesting.class) {
                if (mAdvancedTester == null) {
                    mAdvancedTester = new SimpleTesting();
                }
            }
        }
        return mAdvancedTester;
    }

    private Tester mCurrentTester = null;
    private List<Tester> mTesters = new ArrayList<>();

    private SimpleTesting() {
    }

    public void addTester(Tester tester) {
        mTesters.add(tester);
    }

    public void test(Context context) {
        mTesters.clear();
        mCurrentTester = new MediaController.Tester();
        mCurrentTester.start(context);
    }

    public void testAll(Context context) {
        mCurrentTester = null;

        mTesters.add(new MediaEngine.Tester());
        mTesters.add(new MediaController.Tester());

        mTesters.add(new AudioResearch());
        mTesters.add(new AudioEncoder.Tester());
        mTesters.add(new AudioCapturer.Tester());
        mTesters.add(new AudioPlayTesting.Tester());

        mTesters.add(new RTMPBandwidthTest.Tester());

        for(Tester tester: mTesters)
            tester.start(context);
    }

    public void next() {
        if(mCurrentTester != null) {
            mCurrentTester.next();
        } else {
            for (Tester tester : mTesters)
                tester.next();
        }
    }
}
