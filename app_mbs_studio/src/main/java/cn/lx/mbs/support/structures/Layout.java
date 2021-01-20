package cn.lx.mbs.support.structures;

import com.sanbu.media.TSLayout;
import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
* 该类描述一个布局和其内容,有如下说明和约束:
*
*  1. 通过`addCommonOverlays`可以以常规的方式添加一组Overlay,
*  其包含数据源和绘制描述信息(矩形坐标、透明度)
*
*  2. 通过`addCustomOverlays`可以以自定义方式添加一组Overlay,
*  其包含数据源和绘制程序名称(绘制‘描述’已经事先注册到engine中)
*
*  3. 对于常规Overlay和自定义Overlay的添加:可以2种同时添加,且每种
*  只能添加1次;也可以都不添加,表示清空
*
*  4. VideoEngine目前实际没有ZIndex的概念,是根据Overlay List的
*  顺序绘制,所以`addXXXOverlays`的调用顺序、Overlay List成员的
*  顺序将会影响叠加效果
*/
public class Layout {

    // 背景颜色,支持RGB码/ARGB码/颜色英文名
    private String mBackgroundColor;

    // 常规Overlay的数据源和绘制描述
    private List<CommonOverlay> mCommonOverlays;

    // 自定义Overlay的数据源和绘制程序名称
    private TSLayout mCustomLayout;
    private List<OverlaySrc> mCustomSrcList;

    // Overlay顺序: 0-None 1-Common/Custom 2-Custom/Common
    private int mFlag;

    public Layout() {
        mFlag = 0;
    }

    public String getBackgroundColor() {
        return mBackgroundColor;
    }

    public Layout setBackgroundColor(String codeOrName) {
        mBackgroundColor = codeOrName;
        return this;
    }

    public boolean isCommonAtHead() {
        return mFlag == 1;
    }

    public List<CommonOverlay> getCommonOverlays() {
        return mCommonOverlays;
    }

    public TSLayout getCustomLayout() {
        return mCustomLayout;
    }

    public List<OverlaySrc> getCustomSrcList() {
        return mCustomSrcList;
    }

    public Layout addCommonOverlays(List<CommonOverlay> overlays) {
        mFlag = 2;
        mCommonOverlays = overlays;
        return this;
    }

    public Layout addCustomOverlays(TSLayout layout, List<OverlaySrc> customSrcList) {
        for (OverlaySrc src : customSrcList) {
            if (src.type != OverlaySrc.Type.Stream)
                throw new RuntimeException("invalid custom overlay: not support " + src.type.name());
        }

        mFlag = 1;
        mCustomLayout = layout;
        mCustomSrcList = customSrcList;
        return this;
    }

    public Layout addOverlays(TSLayout layout, List<OverlaySrc> overlays) {
        if (layout.customProgram != null) {
            return addCustomOverlays(layout, overlays);
        } else {
            int size = overlays.size();
            if (size > layout.regionCount)
                size = layout.regionCount;

            List<CommonOverlay> list = new ArrayList<>(size);
            for (int i = 0 ; i < size ; ++i) {
                OverlaySrc src = overlays.get(i);
                OverlayDst dst = new OverlayDst(layout.regions.get(i));
                list.add(new CommonOverlay(src, dst));
            }

            return addCommonOverlays(list);
        }
    }

    public boolean isEqual(Layout other) {
        if (other == null)
            return false;

        return (CompareHelper.isEqual(other.mBackgroundColor, mBackgroundColor) &&
                mFlag == other.mFlag &&

                CompareHelper.isEqual4List(mCommonOverlays, other.mCommonOverlays,
                        (src, dst) -> src.isEqual(dst)) &&

                CompareHelper.isEqual(mCustomLayout, other.mCustomLayout) &&
                CompareHelper.isEqual4List(mCustomSrcList, other.mCustomSrcList,
                        (src, dst) -> src.isEqual(dst))
        );
    }

    public boolean isValid() {
        return mFlag != 0 || !StringUtil.isEmpty(mBackgroundColor);
    }

    public static Layout buildEmpty() {
        return new Layout().addCommonOverlays(Collections.emptyList());
    }
}
