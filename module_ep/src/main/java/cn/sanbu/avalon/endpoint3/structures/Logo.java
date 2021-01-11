package cn.sanbu.avalon.endpoint3.structures;

import com.sanbu.media.Alignment;
import com.sanbu.media.Region;
import com.sanbu.tools.CompareHelper;

import cn.sanbu.avalon.endpoint3.structures.jni.DisplayOverlay;

public class Logo {
    public final String imagePath;
    public final int logoWidth;
    public final int logoHeight;
    public final Alignment alignment;

    public float transparency;
    public Region srcRegion;
    public int sceneWidth;
    public int sceneHeight;

    public Logo(String path, int width, int height, Alignment alignment) {
        this.imagePath = path;
        this.logoWidth = width;
        this.logoHeight = height;
        this.alignment = alignment;
        this.transparency = 0.0f;
        this.srcRegion = Region.buildFull();
        this.sceneWidth = 0;
        this.sceneHeight = 0;
    }

    public void attachSurface(int width, int height) {
        this.sceneWidth = width;
        this.sceneHeight = height;
    }

    public boolean isValid() {
        return (imagePath != null && logoWidth > 0 && logoHeight > 0 &&
                alignment != null && alignment != Alignment.UNDEFINED);
    }

    public boolean isEqual(Logo other) {
        if (other == null)
            return false;
        return (CompareHelper.isEqual(imagePath, other.imagePath) &&
                logoWidth == other.logoWidth && logoHeight == other.logoHeight &&
                CompareHelper.isEqual(alignment, other.alignment) &&
                transparency == other.transparency &&
                CompareHelper.isEqual(srcRegion, other.srcRegion, (src, dst) -> srcRegion.isEqual(other.srcRegion)) &&
                sceneWidth == other.sceneWidth && sceneHeight == other.sceneHeight);
    }

    public DisplayOverlay toOverlay() {
        int sceneWidth = this.sceneWidth > 0 ? this.sceneWidth : 674;
        int sceneHeight = this.sceneHeight > 0 ? this.sceneHeight : 344;
        int logoWidth = this.logoWidth > 0 ? this.logoWidth : 30;
        int logoHeight = this.logoHeight > 0 ? this.logoHeight : 30;
        Region dst = transformDst(sceneWidth, sceneHeight, logoWidth, logoHeight, alignment);

        DisplayOverlay image = DisplayOverlay.buildImage(imagePath, dst);
        image.setTransparency(transparency);
        image.setSrcRegion(srcRegion);
        return image;
    }

    private static Region transformDst(int sceneWidth, int sceneHeight,
                                       int logoWidth, int logoHeight, Alignment alignment) {
        double x = 0.0;
        double y = 0.0;
        //base::_info("test transform dst, scene_width = %d, scene_height = %d, logo width = %d, logo hight = %d",
        //scene_width, scene_height, m_width, m_height);

        double width_scale = (double)logoWidth / (double)sceneWidth;
        double height_scale = (double)logoHeight / (double)sceneHeight;

        //base::_info("test transform dst, width scale = %lf, hight scale = %lf", width_scale, hight_scale);

        // max scale
        double max_scale = 0.12;

        double max_logo_scale = Math.max(width_scale, height_scale);

        if (max_logo_scale > max_scale) {
            double d_value = 0.0;
            if (width_scale > height_scale) {
                d_value = width_scale / height_scale;
                width_scale = max_scale;
                height_scale = width_scale / d_value;
            }
            else {
                d_value = height_scale / width_scale;
                height_scale = max_scale;
                width_scale = height_scale / d_value;
            }
        } else {
        }

        //base::_info("test transform dst, finish width_scale = %lf, hight_scale = %lf", width_scale, hight_scale);
        double width_radio = width_scale;
        double height_radio = height_scale;

        //base::_info("test transform dst, width_radio = %lf, height_radio = %lf", width_radio, height_radio);
        switch (alignment) {
            case POS0:
                /// m_dst = "0.02:0.02:0.1:0.1";
                x = 0.02;
                y = 0.02;
                break;
            case POS1:
                /// m_dst = "0.45:0.02:0.1:0.1";
                x = 0.500 - (width_radio / 2.000);
                y = 0.020;
                break;
            case POS2:
                /// m_dst = "0.88:0.02:0.1:0.1";
                x = 1.000 - width_radio - 0.020;
                y = 0.020;
                break;
            case POS6:
                /// m_dst = "0.02:0.88:0.1:0.1";
                x = 0.020;
                y = 1.000 - height_radio - 0.020;
                break;
            case POS7:
                /// m_dst = "0.45:0.88:0.1:0.1";
                x = 0.500 - (width_radio / 2.000);
                y = 1.000 - height_radio - 0.020;
                break;
            case POS8:
                /// m_dst = "0.88:0.88:0.1:0.1";
                x = 1.000 - width_radio - 0.020;
                y = 1.000 - height_radio - 0.020;
                break;
            case POS3:
            case POS4:
            case POS5:
            default:
                /// m_dst = "0.02:0.02:0.1:0.1";
                x = 0.02;
                y = 0.02;
                break;
        }

        return new Region(x, y, width_radio, height_radio);
    }
}
