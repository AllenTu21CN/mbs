package sanp.avalon.libs;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import sanp.avalon.libs.media.audio.AudioCapturer;
import sanp.avalon.libs.media.audio.AudioEncoder;
import sanp.avalon.libs.media.audio.AudioPlayTesting;
import sanp.avalon.libs.network.protocol.RTMPBandwidthTest;

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


    private List<Tester> mTesters = new ArrayList<>();

    private SimpleTesting() {
//        mTesters.add(new RTMPBandwidthTest.Tester());
//        mTesters.add(new AudioCapturer.Tester());
//        mTesters.add(new AudioEncoder.Tester());
//        mTesters.add(new AudioPlayTesting.Tester());
    }

    public void addTester(Tester tester) {
        mTesters.add(tester);
    }

    public void test(Context context) {
        for(Tester tester: mTesters)
            tester.start(context);
    }

    public void next() {
        for(Tester tester: mTesters)
            tester.next();
    }
}
