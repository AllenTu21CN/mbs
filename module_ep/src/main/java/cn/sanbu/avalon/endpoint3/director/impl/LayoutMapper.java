package cn.sanbu.avalon.endpoint3.director.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sanbu.base.BaseError;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import cn.sanbu.avalon.endpoint3.director.structures.Event;
import cn.sanbu.avalon.endpoint3.director.structures.Layout;
import cn.sanbu.avalon.endpoint3.director.structures.StaticEvent;
import cn.sanbu.avalon.endpoint3.director.structures.Value;
import cn.sanbu.avalon.endpoint3.director.structures.StateObserver;

public class LayoutMapper implements LogicHandler, StateObserver {

    private static final String TAG = LayoutMapper.class.getSimpleName();

    private String mName;
    private JsonObject mConfig;
    private List<String> mValidRoles;

    private List<StaticEvent> mRecvEvents;
    private List<String> mTokenPattern;
    private Map<String, List<DisplayConfig>> mSceneEnum;

    private Map<String, String> mTokens = new LinkedHashMap<>();
    private Map<String, List<Value>> mSubStatus = new LinkedHashMap<>();
    private Layout[] mLayout = null;

    @Override
    public int init(String owner, JsonObject config, StateObserver observer, List<String> validRoles) {
        if (mConfig != null)
            return 0;

        mName = TAG + "@" + owner;
        mConfig = config;
        mValidRoles = validRoles == null ? null : new ArrayList<>(validRoles);

        JsonElement recv = config.get("recvEvents");
        JsonElement tokenPattern = config.get("tokenPattern");
        JsonElement anEnum = config.get("enum");
        if (tokenPattern == null || anEnum == null ||
                recv == null || !recv.isJsonArray())
            return BaseError.INVALID_PARAM;

        mRecvEvents = new Gson().fromJson(recv, new TypeToken<List<StaticEvent>>() {
        }.getType());
        mTokenPattern = new Gson().fromJson(tokenPattern, new TypeToken<List<String>>() {
        }.getType());
        mSceneEnum = new Gson().fromJson(anEnum, new TypeToken<Map<String, List<DisplayConfig>>>() {
        }.getType());

        return 0;
    }

    @Override
    public void release() {
        mName = null;
        mConfig = null;
        mValidRoles = null;

        mRecvEvents = null;
        mTokenPattern = null;
        mSceneEnum = null;

        mLayout = null;
        mTokens.clear();
        mSubStatus.clear();
    }

    @Override
    public String getDescription() {
        return mConfig.get("desc").getAsString();
    }

    @Override
    public List<Event> pushEvent(Event event) {
        for (StaticEvent staticEvt: mRecvEvents) {
            if (staticEvt.id == event.id) {
                // gen scene token
                String token = genSceneToken();
                LogUtil.d(DIRUtils.TAG, mName, "scene token: " + token);

                // get scene by token
                List<DisplayConfig> scene = mSceneEnum.get(token);
                if (scene == null) {
                    LogUtil.w(DIRUtils.TAG, mName, "unknown scene: " + token);
                    break;
                }

                // gen display layout by config
                mLayout = new Layout[scene.size()];
                for (int i = 0 ; i < scene.size() ; ++i)
                    mLayout[i] = genLayout(mName, i, scene.get(i), mSubStatus, mValidRoles);

                LogUtil.d(DIRUtils.TAG, mName, "layout: " + new Gson().toJson(mLayout));
                break;
            }
        }

        return null;
    }

    @Override
    public void onChanged(String tokenName, String tokenValue, List<Value> status) {
        if (tokenValue != null)
            mTokens.put(tokenName, tokenValue);
        mSubStatus.put(tokenName, status);
        LogUtil.d(DIRUtils.TAG, mName, "mTokens: " + new Gson().toJson(mTokens));
        LogUtil.d(DIRUtils.TAG, mName, "mSubStatus: " + new Gson().toJson(mSubStatus));
    }

    public Layout[] getLayout() {
        return mLayout;
    }

    ///////////////// private functions

    private String genSceneToken() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mTokenPattern.size(); i++) {
            String token = mTokenPattern.get(i);
            String value = mTokens.get(token);
            if (i == mTokenPattern.size() - 1) {
                stringBuilder.append(token).append("=").append(value);
            } else {
                stringBuilder.append(token).append("=").append(value).append("&");
            }
        }
        return stringBuilder.toString();
    }

    ///////////////// static utils

    private static boolean isValidValue(final List<String> validRoles, String role) {
        return validRoles == null || role == null || validRoles.contains(role);
    }

    private static Layout genLayout(String name, int index, DisplayConfig config,
                                    Map<String, List<Value>> subStatus, final List<String> validRoles) {
        // get actual required and optional cells with current sub-states
        List<List<Layout.Cell>> required = getActualCandidates(name, subStatus, config.required, validRoles);
        List<List<Layout.Cell>> optional = getActualCandidates(name, subStatus, config.optional, validRoles);

        // move the first candidate from optional list to required list if that is empty
        if (required.size() == 0 && optional.size() > 0) {
            List<Layout.Cell> first = optional.remove(0);
            required.add(first);
        }

        // there is no actual cell, return empty layout
        if (required.size() == 0)
            return buildEmpty(name, index, config);

        // strip duplicate cell
        stripDuplication(Arrays.asList(required, optional));

        // select layout name
        String layoutName = selectLayoutName(name, config, required);
        if (layoutName == null)
            return null;

        // trans candidates to cells
        List<Layout.Cell> requiredCells = copyCandidates2Cells(required);
        List<Layout.Cell> optionalCells = copyCandidates2Cells(optional);

        return new Layout(index, layoutName, requiredCells, optionalCells);
    }

    private static List<List<Layout.Cell>> getActualCandidates(String name, Map<String, List<Value>> subStatus,
                                                               List<String> expectants, final List<String> validRoles) {
        List<List<Layout.Cell>> candidates = new LinkedList<>();
        for (String expectant: expectants) {
            if (DIRUtils.isExpectingFixedRole(expectant)) {
                // this is fixed role
                String role = DIRUtils.getExpectedFixedRole(expectant);

                if (isValidValue(validRoles, role))
                    candidates.add(Arrays.asList(new Layout.Cell(role, -1)));
            } else {
                // this is status name
                String statusName = expectant;

                List<Value> status = subStatus.get(statusName);
                if (status == null) {
                    LogUtil.i(DIRUtils.TAG, name, "can not get sub status values for " + statusName + ", maybe lost '#' for fixed role");
                    continue;
                }

                List<Layout.Cell> candidate = new LinkedList<>();
                for (Value value: status) {
                    if (StringUtil.isEmpty(value.eventValue))
                        continue;
                    candidate.add(new Layout.Cell(value.eventValue, value.objId));
                }

                if (candidate.size() == 0)
                    continue;
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private static Layout buildEmpty(String name, int index, DisplayConfig config) {
        String layoutName = selectLayoutName(name, config, null);
        if (layoutName == null)
            return null;
        return new Layout(index, layoutName, Collections.emptyList(), Collections.emptyList());
    }

    private static int compare(int s1, int s2) {
        return s1 == s2 ? 0 : (s1 > s2 ? 1 : -1);
    }

    private static final Comparator<Layout.Cell> gComparator = (s1, s2) -> {
        if (s1.isEqual(s2))
            return 0;
        int cmp = compare(s1.objId, s2.objId);
        if (cmp != 0)
            return cmp;
        return s1.role.compareTo(s2.role);
    };

    private static void stripDuplication(List<List<List<Layout.Cell>>> sources) {
        Set<Layout.Cell> cells = new TreeSet<>(gComparator);
        for (List<List<Layout.Cell>> source: sources) {

            Iterator<List<Layout.Cell>> itr1 = source.iterator();
            while (itr1.hasNext()) {
                List<Layout.Cell> crowd = itr1.next();

                Iterator<Layout.Cell> itr2 = crowd.iterator();
                while (itr2.hasNext()) {
                    Layout.Cell cell = itr2.next();
                    if (!cells.add(cell))
                        itr2.remove();
                }

                if (crowd.size() == 0)
                    itr1.remove();
            }
        }
    }

    private static String selectLayoutName(String name, DisplayConfig config, List<List<Layout.Cell>> candidates) {
        final boolean similar;
        Map<String, String> nameConfig;
        if (candidates == null || candidates.size() == 0 || candidates.size() == 1) {
            similar = true;
            nameConfig = config.layoutName.similar;
        } else {
            similar = false;
            nameConfig = config.layoutName.multiple;
        }

        // count of all cells
        int count = 0;
        if (candidates != null) {
            for (List<Layout.Cell> cells : candidates)
                count += cells.size();
        }
        if (count == 0)
            count = 1;

        // try to get layout name
        for (int i = count ; i > 0 ; --i) {
            String layoutName = nameConfig.get(String.valueOf(i));
            if (layoutName != null)
                return layoutName;
        }

        LogUtil.w(DIRUtils.TAG, name, String.format("selectLayoutName: can not get matched layout name with %d cells on %s group", count, similar ? "similar" : "multiple"));
        return null;
    }

    private static List<Layout.Cell> copyCandidates2Cells(List<List<Layout.Cell>> candidates) {
        List<Layout.Cell> cells = new LinkedList<>();
        for (List<Layout.Cell> candidate: candidates) {
            for (Layout.Cell cell: candidate)
                cells.add(cell);
        }
        return cells;
    }

    private static class DisplayConfig {
        public final LayoutNameConfig layoutName;
        public final List<String> required;
        public final List<String> optional;

        public DisplayConfig(LayoutNameConfig layoutName, List<String> required, List<String> optional) {
            this.layoutName = layoutName;
            this.required = required;
            this.optional = optional;
        }
    }

    private static class LayoutNameConfig {
        public final Map<String, String> similar;
        public final Map<String, String> multiple;

        public LayoutNameConfig(Map<String, String> similar, Map<String, String> multiple) {
            this.similar = similar;
            this.multiple = multiple;
        }
    }
}
