package sanp.tools.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.DrawableUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import java.util.List;

/**
 * Created by Tom on 2017/3/16.
 */

public class AnimatorUtils {

    private static AnimationDrawable animationDrawable;

    /**
     * @param view  动画的视图
     * @param index 动画的终点
     * @return
     */
    public static Animator setLeftOutAnim(View view, float index) {
        view.setVisibility(View.VISIBLE);
        AnimatorSet animatorSet = new AnimatorSet();
        float width = view.getMeasuredWidth();
        ObjectAnimator objectAnimator_1 = ObjectAnimator.ofFloat(view, "translationX", -width, index);
        ObjectAnimator objectAnimator_2 = ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f);
        objectAnimator_1.setDuration(300);
        objectAnimator_2.setDuration(300);
        animatorSet.play(objectAnimator_1).with(objectAnimator_2);
        animatorSet.start();
        return animatorSet;
    }

    /**
     * @param view  动画的视图
     * @param index 动画的初始点
     */
    public static void setLeftInAnim(View view, float index) {
        view.setVisibility(View.VISIBLE);
        AnimatorSet animatorSet = new AnimatorSet();
        float width = view.getMeasuredWidth();
        ObjectAnimator objectAnimator_1 = ObjectAnimator.ofFloat(view, "translationX", index, -width);
        ObjectAnimator objectAnimator_2 = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f);
        objectAnimator_1.setDuration(300);
        objectAnimator_2.setDuration(300);
        animatorSet.play(objectAnimator_1).with(objectAnimator_2);
        animatorSet.start();
    }

    /**
     * @param drawables  图片ids
     * @param time 播放间隔
     * @param image 播放背景
     */
    public static void startFrameAnim(List<Integer> drawables, int time, ImageView image) {
        animationDrawable = new AnimationDrawable();
        for (int id : drawables) {
            Drawable mdrawable = image.getResources().getDrawable(id);
            animationDrawable.addFrame(mdrawable, time);
        }
        animationDrawable.setOneShot(false);
        image.setBackgroundDrawable(animationDrawable);
        animationDrawable.start();
    }

    public static void stopFrameAnim() {
        animationDrawable.stop();
    }

    /**
     * @param view  闪烁图片
     * @param time 闪烁时间ms
     */
    public static void setFlashAnimStart(View view, int time)
    {
        AlphaAnimation mAlpha = new AlphaAnimation(1.0f, 0.3f);
        mAlpha.setDuration(time);
        mAlpha.setRepeatCount(Animation.INFINITE);//表示重复多次。 也可以设定具体重复的次数，比如alphaAnimation1.setRepeatCount(5);
        mAlpha.setRepeatMode(Animation.REVERSE);//表示动画结束后，反过来再执行。 该方法有两种值， RESTART 和 REVERSE。 RESTART表示从头开始，REVERSE表示从末尾倒播。
        mAlpha.setFillAfter(false);
        view.setAnimation(mAlpha);
        mAlpha.start();
    }

    public static void setFlashAnimStop(View view)
    {
        view.clearAnimation();
    }

    public static void setTranslateAnimShow(View view, int time)
    {
        TranslateAnimation mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        mShowAction.setDuration(time);
        view.startAnimation(mShowAction);
        view.setVisibility(View.VISIBLE);
    }

    public static void setTranslateAnimHide(View view, int time)
    {
        TranslateAnimation mHiddenAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f);
        mHiddenAction.setDuration(time);
        view.startAnimation(mHiddenAction);
        view.setVisibility(View.GONE);
    }
}
