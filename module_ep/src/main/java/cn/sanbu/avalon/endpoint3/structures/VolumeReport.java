package cn.sanbu.avalon.endpoint3.structures;

import com.sanbu.tools.LogUtil;
import com.sanbu.tools.StringUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VolumeReport {

    private static final String TAG = VolumeReport.class.getSimpleName();

    public static class ReportValue {
        public final int mediaId;
        public final List<Float> dBFSs;

        public ReportValue(int mediaId, List<Float> dBFSs) {
            this.mediaId = mediaId;
            this.dBFSs = dBFSs;
        }

        public float getAvgdBFS() {
            if (dBFSs == null || dBFSs.size() == 0)
                return -70.0f;
            float sum = 0;
            for (float value : dBFSs)
                sum += value;
            return sum / (float) (dBFSs.size());
        }

        public float getMaxdBFS() {
            float max = -70.0f;
            if (dBFSs != null) {
                for (float value : dBFSs)
                    max = Math.max(max, value);
            }
            return max;
        }
    }

    public final List<ReportValue> sourceReport;
    public final List<ReportValue> sceneReport;

    private VolumeReport() {
        this.sourceReport = new LinkedList<>();
        this.sceneReport = new LinkedList<>();
    }

    private static final int PREFIX_SOURCE_SIZE = "sources:".length();
    private static final int PREFIX_SCENE_SIZE = "scenes:".length();

    public static VolumeReport parse(String report) {
        // report likes:
        //  sources:3=-39.182682,-39.628212,;|scenes:1=-39.182682,-39.628212,;2=-90.308731,-90.308731,;
        //  sources:|scenes:
        //  sources:|scenes:1=-39.182682,-39.628212,;2=-90.308731,-90.308731,;
        //  sources:3=-39.182682,-39.628212,;|scenes:
        //  sources:3=;|scenes:1=;2=-90.308731,-90.308731,-90.308731,;

        VolumeReport result = new VolumeReport();

        try {
            // check 'sources:'
            if (!report.startsWith("sou"))
                throw new RuntimeException();

            // set offset for 'sources'
            int []offset = {PREFIX_SOURCE_SIZE};

            // get end of 'sources'
            int end = report.indexOf('|');
            if (end < 0)
                throw new RuntimeException();

            // parse 'sources' in loop
            int mediaId;
            while ((mediaId = getMediaId(report, offset, end - 1)) >= 0) {
                List<Float> dBFSs = getdBFSs(report, offset, end - 1);
                result.sourceReport.add(new ReportValue(mediaId, dBFSs));
                if (report.charAt(offset[0]) == ';')
                    ++offset[0];
            }

            // check 'scenes:'
            if (report.charAt(end + 1) != 's' ||
                    report.charAt(end + 2) != 'c' ||
                    report.charAt(end + 3) != 'e')
                throw new RuntimeException();

            // set offset for 'scenes'
            offset[0] = end + 1 + PREFIX_SCENE_SIZE;

            // get end of 'scenes'
            end = report.length();

            // parse 'scenes' in loop
            while ((mediaId = getMediaId(report, offset, end - 1)) >= 0) {
                List<Float> dBFSs = getdBFSs(report, offset, end - 1);
                result.sceneReport.add(new ReportValue(mediaId, dBFSs));
                if (report.charAt(offset[0]) == ';')
                    ++offset[0];
            }

            return result;
        } catch (RuntimeException e) {
            LogUtil.w(TAG, "invalid volume report: " + report);
            return result;
        }
    }

    private static int getMediaId(String src, int[] offset, int endIndex) {
        int index = StringUtil.indexOf(src, '=', offset[0], endIndex);
        if (index < 0)
            return -1;

        try {
            int id = Integer.valueOf(src.substring(offset[0], index));
            offset[0] = index + 1;
            return id;
        } catch (Exception e) {
            LogUtil.w(TAG, "invalid volume report: " + src);
            return -1;
        }
    }

    private static List<Float> getdBFSs(String src, int[] offset, int endIndex) {
        //  ;...
        //  -39.1,;...
        //  -39.1,-39.1,;...
        //  -90.1,-90.1,-90.1,;...

        List<Float> dbFSs = new ArrayList<>(5);

        endIndex = StringUtil.indexOf(src, ';', offset[0], endIndex) - 1;
        if (endIndex < 0 || endIndex < offset[0])
            return dbFSs;

        int index;
        while ((index = StringUtil.indexOf(src, ',', offset[0], endIndex)) >= 0) {
            try {
                float dBFS = Float.valueOf(src.substring(offset[0], index));
                dbFSs.add(dBFS);
                offset[0] = index + 1;
            } catch (Exception e) {
                LogUtil.w(TAG, "invalid volume report: " + src);
                return dbFSs;
            }
        }

        return dbFSs;
    }
}
