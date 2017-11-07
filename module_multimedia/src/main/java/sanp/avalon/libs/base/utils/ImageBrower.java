package sanp.avalon.libs.base.utils;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import org.xutils.common.Callback;
import org.xutils.image.ImageOptions;
import org.xutils.x;

/**
 * Created by Tom on 2016/11/7.
 */
public class ImageBrower {
    static ImageOptions options;
    static ImageBrower imageBrower;

    public static ImageBrower getInstance() {
        options = new ImageOptions.Builder().setFadeIn(true).build(); //淡入效果
        if (imageBrower == null) {
            imageBrower = new ImageBrower();
        }
        return imageBrower;
    }

    public ImageBrower() {
        /**
         * 通过ImageOptions.Builder().set方法设置图片的属性
         */
        //options = new ImageOptions.Builder().setFadeIn(true).build(); //淡入效果
        //ImageOptions.Builder()的一些其他属性：
        //.setCircular(true) //设置图片显示为圆形
        //.setSquare(true) //设置图片显示为正方形
        //setCrop(true).setSize(200,200) //设置大小
        //.setAnimation(animation) //设置动画
        //.setFailureDrawable(Drawable failureDrawable) //设置加载失败的动画
        //.setFailureDrawableId(int failureDrawable) //以资源id设置加载失败的动画
        //.setLoadingDrawable(Drawable loadingDrawable) //设置加载中的动画
        //.setLoadingDrawableId(int loadingDrawable) //以资源id设置加载中的动画
        //.setIgnoreGif(false) //忽略Gif图片
        //.setParamsBuilder(ParamsBuilder paramsBuilder) //在网络请求中添加一些参数
        //.setRaduis(int raduis) //设置拐角弧度
        //.setUseMemCache(true) //设置使用MemCache，默认true
    }

    public static void showImg_1(ImageView v, String url) {
        x.image().bind(v, url);
    }

    public static void showImg_2(ImageView v, String url) {
        x.image().bind(v, url, options);
    }

    public static void showImg_3(ImageView v, String url, final XutilsCallback callback) {
        x.image().bind(v, url, options, new Callback.CommonCallback<Drawable>() {
            @Override
            public void onSuccess(Drawable result) {
                callback.callback("", true);
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                callback.callback("", false);
            }

            @Override
            public void onCancelled(CancelledException cex) {
            }

            @Override
            public void onFinished() {
            }
        });
    }
}
