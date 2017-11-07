package sanp.mpx.mc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ScreenLayout {

    public enum LayoutMode {
        UNSPECIFIED(0, "未定义"),
        SYMMETRICAL(1, "等分屏"),
        ASYMMETRY_FIXED(2, "不等分屏"),
        ASYMMETRY_OVERLAPPING(3, "画中画"),
        @Deprecated
        FREE_STYLE(10, "任意");

        private int value;
        private String dsp;
        private LayoutMode(int value, String dsp) {
            this.value = value;
            this.dsp = dsp;
        }
        public String toString() {
            return dsp;
        }
        public int toValue() {
            return value;
        }
    }

    public enum FillPattern {
        FILL_PATTERN_NONE(0, "无"),
        FILL_PATTERN_ADAPTING(1, "适应/完整"),
        FILL_PATTERN_STRETCHED(2, "拉伸/充满"),
        FILL_PATTERN_CROPPING(3, "裁剪");

        private int value;
        private String dsp;
        private FillPattern(int value, String dsp) {
            this.value = value;
            this.dsp = dsp;
        }
        public String toString() {
            return dsp;
        }
        public int toValue() {
            return value;
        }
    }

    static public int getSubScreenCnt(LayoutMode mode, int sourceCnt) {
        Map<Integer, String[]> layouts = mLayouts.get(mode);
        if(layouts.containsKey(sourceCnt))
            return sourceCnt;
        Integer[] cnts = new Integer[layouts.size()];
        cnts = layouts.keySet().toArray(cnts);
        Arrays.sort(cnts);
        for(Integer cnt: cnts) {
            if(cnt >= sourceCnt)
                return cnt;
        }
        return cnts[-1];
    }

    static public String[] getLayouts(LayoutMode mode, int subScreenCnt) {
        return mLayouts.get(mode).get(subScreenCnt);
    }

    static public String getSubScreenPosition(LayoutMode mode, int subScreenCnt, int subScreenIndex) {
        return mLayouts.get(mode).get(subScreenCnt)[subScreenIndex];
    }

    static public String getBackground(LayoutMode mode, int subScreenCnt) {
        Map<Integer, String> bgs = mBackgrounds.get(mode);
        if(bgs == null)
            return null;
        return bgs.get(subScreenCnt);
    }

    static private Map<LayoutMode, Map<Integer, String>> mBackgrounds = new HashMap<LayoutMode, Map<Integer, String>>() {{
        put(LayoutMode.SYMMETRICAL,
                new HashMap<Integer, String>() {{
                    // put(1,  "video_bg.jpg");        //单分屏
                    put(2,  "symmetrical_2.jpg");   //两 等分屏
                }}
        );

        put(LayoutMode.ASYMMETRY_FIXED,
                new HashMap<Integer, String>() {{
                    // put(1,  "video_bg.jpg");        //单分屏
                    put(3,  "asymmetric_3.jpg");    //一大两小 不等分屏
                }}
        );

        put(LayoutMode.ASYMMETRY_OVERLAPPING,
                new HashMap<Integer, String>() {{
                    /*
                    put(1, "overlapping_1.jpg");   //画中画 左上
                    put(2, "overlapping_2.jpg");   //画中画 右上
                    put(3, "overlapping_3.jpg");   //画中画 左下
                    put(4, "overlapping_4.jpg");   //画中画 右下
                    */
                }}
        );

        put(LayoutMode.FREE_STYLE,
                new HashMap<Integer, String>() {{
                    put(-1,  "video_bg.jpg");        //单分屏
                }}
        );
    }};

    static private Map<LayoutMode, Map<Integer, String[]>> m16_9Layouts = new HashMap<LayoutMode, Map<Integer, String[]>>() {{
        put(LayoutMode.SYMMETRICAL,
            new HashMap<Integer, String[]>() {{
                put(1,  new String[] {      //单分屏
                        "0.0:0.0:1.0:1.0"
                });

                put(2,  new String[] {      //两 等分屏
                        "0.0052:0.2537:0.4917:0.4917", "0.5021:0.2537:0.4917:0.4917"
                });

                put(3,  new String[] {      //三 等分屏
                        "0.2583:0.0093:0.4833:0.4833",
                        "0.0115:0.5093:0.4833:0.4833", "0.5052:0.5093:0.4833:0.4833"
                });

                put(4,  new String[] {      //四 等分屏
                        "0.0052:0.0037:0.4917:0.4917", "0.5010:0.0037:0.4917:0.4917",
                        "0.0052:0.5037:0.4917:0.4917", "0.5010:0.5037:0.4917:0.4917"
                });

                put(9,  new String[] {      //九 等分屏
                        "0.0198:0.0167:0.3167:0.3167", "0.3417:0.0167:0.3167:0.3167", "0.6635:0.0167:0.3167:0.3167",
                        "0.0198:0.3426:0.3167:0.3167", "0.3417:0.3426:0.3167:0.3167", "0.6635:0.3426:0.3167:0.3167",
                        "0.0198:0.6685:0.3167:0.3167", "0.3417:0.6685:0.3167:0.3167", "0.6635:0.6685:0.3167:0.3167"
                });
            }}
        );

        put(LayoutMode.ASYMMETRY_FIXED,
            new HashMap<Integer, String[]>() {{
                put(1,  new String[] {      //单分屏
                        "0.0:0.0:1.0:1.0"
                });

                put(2,  new String[] {      //一大一小 不等分屏
                        "0.0094:0.1778:0.6530:0.6530", "0.6719:0.5111:0.3187:0.3187"
                });

                put(3,  new String[] {      //一大两小 不等分屏
                        "0.0094:0.1778:0.6530:0.6530",
                        "0.6719:0.1778:0.3187:0.3187", "0.6719:0.5111:0.3187:0.3187"
                });

                put(4,  new String[] {      //一大三小 不等分屏
                        "0.0094:0.1352:0.7353:0.7353",
                        "0.7553:0.1352:0.2353:0.2353", "0.7553:0.3852:0.2353:0.2353", "0.7553:0.6352:0.2353:0.2353"
                });

                put(5,  new String[] {      //一大四小 不等分屏
                        "0.1417:0.0074:0.7167:0.7167",
                        "0.0146:0.7444:0.2333:0.2333", "0.2604:0.7444:0.2333:0.2333", "0.5063:0.7444:0.2333:0.2333", "0.7521:0.7444:0.2333:0.2333"
                });

                put(10,  new String[] {     //一大九小 不等分屏
                        "0.2135:0.0204:0.7677:0.7667",
                        "0.0198:0.0204:0.1833:0.1833", "0.0198:0.2148:0.1833:0.1833", "0.0198:0.4093:0.1833:0.1833",
                        "0.0198:0.6037:0.1833:0.1833", "0.0198:0.7981:0.1833:0.1833", "0.2135:0.7981:0.1833:0.1833",
                        "0.4083:0.7981:0.1833:0.1833", "0.6031:0.7981:0.1833:0.1833", "0.7969:0.7981:0.1833:0.1833"
                });
            }}
        );

        put(LayoutMode.ASYMMETRY_OVERLAPPING,
            new HashMap<Integer, String[]>() {{
                put(5,  new String[] {      //1+4角落 画中画
                        "0.0:0.0:1.0:1.0",
                        "0.0:0.0:0.25:0.25", "0.75:0.0:0.25:0.25",
                        "0.0:0.75:0.25:0.25", "0.75:0.75:0.25:0.25",
                });
            }}
        );
    }};

    static private Map<LayoutMode, Map<Integer, String[]>> mOptimizedLayouts = new HashMap<LayoutMode, Map<Integer, String[]>>() {{
        put(LayoutMode.SYMMETRICAL,
            new HashMap<Integer, String[]>() {{
                put(1,  new String[] {      //单分屏
                        "0.0:0.0:1.0:1.0"
                });
                put(2,  new String[] {      //两 等分屏
                        "0.0:0.1760:0.4948:0.6600", "0.5052:0.1760:0.4948:0.6600"
                });
                put(3,  new String[] {      //三 等分屏
                        "0.2583:0.0093:0.4833:0.4833",
                        "0.0115:0.5093:0.4833:0.4833", "0.5052:0.5093:0.4833:0.4833"
                });
                put(9,  new String[] {      //九 等分屏
                        "0.0198:0.0167:0.3167:0.3167", "0.3417:0.0167:0.3167:0.3167", "0.6635:0.0167:0.3167:0.3167",
                        "0.0198:0.3426:0.3167:0.3167", "0.3417:0.3426:0.3167:0.3167", "0.6635:0.3426:0.3167:0.3167",
                        "0.0198:0.6685:0.3167:0.3167", "0.3417:0.6685:0.3167:0.3167", "0.6635:0.6685:0.3167:0.3167"
                });
            }}
        );

        put(LayoutMode.ASYMMETRY_FIXED,
            new HashMap<Integer, String[]>() {{
                put(1,  new String[] {      //单分屏
                        "0.0:0.0:1.0:1.0"
                });
                put(3,  new String[] {      //一大两小 不等分屏
                        "0.0:0.1694:0.6666:0.6620",
                        "0.6766:0.1694:0.3239:0.3240", "0.6766:0.5075:0.3239:0.3240"
                });
                put(10,  new String[] {     //一大九小 不等分屏
                        "0.2135:0.0204:0.7677:0.7667",
                        "0.0198:0.0204:0.1833:0.1833", "0.0198:0.2148:0.1833:0.1833", "0.0198:0.4093:0.1833:0.1833",
                        "0.0198:0.6037:0.1833:0.1833", "0.0198:0.7981:0.1833:0.1833", "0.2135:0.7981:0.1833:0.1833",
                        "0.4083:0.7981:0.1833:0.1833", "0.6031:0.7981:0.1833:0.1833", "0.7969:0.7981:0.1833:0.1833"
                });
            }}
        );

        put(LayoutMode.ASYMMETRY_OVERLAPPING,
            new HashMap<Integer, String[]>() {{
                put(5,  new String[] {      //1+4角落 画中画
                        "0.0:0.0:1.0:1.0",
                        "0.0:0.0:0.2344:0.2352",
                        "0.7656:0.0:0.2344:0.2352",
                        "0.0:0.7648:0.2344:0.2352",
                        "0.7656:0.7648:0.2344:0.2352"
                });
            }}
        );
    }};

    static private Map<LayoutMode, Map<Integer, String[]>> mLayouts = mOptimizedLayouts;
}
