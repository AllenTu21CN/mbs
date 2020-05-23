package cn.lx.mbs.impl.structures;

import com.sanbu.tools.LogCollector;
import com.sanbu.tools.StringUtil;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class LogConfig {

    public enum Switch {
        DisabledAlways(false, false, -1, -1),
        EnabledAlways(true, false, -1, -1),
        EnabledUntilReboot(true, false, -1, -1),
        EnabledInOneDay(true, true, Calendar.DAY_OF_MONTH, 1),
        EnabledInOneWeek(true, true, Calendar.DAY_OF_MONTH, 7);

        public final boolean enabled;

        public final boolean timed;
        public final int field;
        public final int amount;

        Switch(boolean enabled, boolean timed, int field, int amount) {
            this.enabled = enabled;
            this.timed = timed;
            this.field = field;
            this.amount = amount;
        }
    }

    public Switch onOff;
    public String tagList;

    private String includedList;
    private String excludedList;

    public LogConfig(Switch onOff, String tagList, String includedList, String excludedList) {
        this.onOff = onOff;
        this.tagList = tagList;
        this.includedList = includedList;
        this.excludedList = excludedList;
    }

    public LogConfig(Switch onOff, LogCollector.Config config) {
        this.onOff = onOff;
        this.tagList = config.tagExpr;
        this.includedList = config.included == null ? "" : StringUtil.join(",", config.included);
        this.excludedList = config.excluded == null ? "" : StringUtil.join(",", config.excluded);
    }

    public boolean isValid() {
        return (onOff != null && !StringUtil.isEmpty(tagList));
    }

    public List<String> getIncludedList() {
        return String2List(includedList);
    }

    public List<String> getExcludedList() {
        return String2List(excludedList);
    }

    private static List<String> String2List(String str) {
        if (str == null)
            return null;

        String[] strings = str.split(",");
        List<String> result = new LinkedList<>();
        for (String item: strings) {
            String key = item.trim();
            if (!key.isEmpty())
                result.add(key);
        }

        return result;
    }
}
