package cn.lx.media.gles;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class SharedTextureManager {
    private static final String TAG = SharedTextureManager.class.getName();

    public final int SINGLE_IMAGE_MAXIMUM_WIDTH = 1920;
    public final int SINGLE_IMAGE_MAXIMUM_HEIGHT = 1080;

    private final int mGridRowCount;
    private final int mGridColumnCount;
    private int[][] mSlots;
    private Bitmap[] mSourceImages;

    private Texture2dProgram mTexPgm;
    private int mTexId;
    private Sprite2d mRect;

    private float[] mDisplayProjectionMatrix = new float[16];

    public SharedTextureManager(int rowCount, int columnCount) {
        mGridRowCount = rowCount;
        mGridColumnCount = columnCount;
        mSlots = new int[mGridRowCount][mGridColumnCount];
        for (int y = 0; y < mGridRowCount; y++) {
            for (int x = 0; x < mGridColumnCount; x++) {
                mSlots[y][x] = 0;
            }
        }

        mSourceImages = new Bitmap[rowCount * columnCount];

        mTexPgm = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);

        // Create a texture object and bind it
        int[] values = new int[1];
        GLES20.glGenTextures(1, values, 0);
        GlUtil.checkGlError("glGenTextures");
        mTexId = values[0];   // expected > 0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexId);
        GlUtil.checkGlError("glBindTexture " + mTexId);

        // Create texture storage
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                getTotalWidth(), getTotalHeight(),
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // Set parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GlUtil.checkGlError("glTexParameter");

        ScaledDrawable2d rectDrawable = new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
        mRect = new Sprite2d(rectDrawable);
        mRect.setTexture(mTexId);

        GlUtil.checkGlError("prepareDisplayNameTexture done");

        Matrix.orthoM(mDisplayProjectionMatrix, 0, 0.0f, 1.0f, 0.0f, 1.0f, -1.0f, 1.0f);
    }

    public void release() {
        GLES20.glDeleteTextures(1, new int[] { mTexId }, 0);
    }

    public int getTotalWidth() {
        return SINGLE_IMAGE_MAXIMUM_WIDTH * mGridColumnCount;
    }

    public int getTotalHeight() {
        return SINGLE_IMAGE_MAXIMUM_HEIGHT * mGridRowCount;
    }

    private boolean validateSlot(int slot) {
        if (slot < 0 || slot >= mGridColumnCount * mGridRowCount) {
            Log.e(TAG, "Invalid slot index: " + slot + ", out of range.");
            return false;
        }

        int y = slot / mGridColumnCount;
        int x = slot - y * mGridColumnCount;

        if (x >= mGridColumnCount || y >= mGridRowCount) {
            Log.e(TAG, "Invalid slot index: " + slot + ", out of range.");
            return false;
        }

        if (0 == mSlots[y][x]) {
            Log.e(TAG, "slot [" + y + "][" + x + "] is idle.");
            return false;
        }

        return true;
    }

    public int getIdleSlot(int width, int height) {
        if (width > SINGLE_IMAGE_MAXIMUM_WIDTH || height > SINGLE_IMAGE_MAXIMUM_HEIGHT) {
            return -1;
        }

        for (int y = 0; y < mGridRowCount; y++) {
            for (int x = 0; x < mGridColumnCount; x++) {
                if (0 == mSlots[y][x]) {
                    mSlots[y][x] = 1;
                    //Log.d(TAG, "Got an idle slot[" + y + "][" + x + "]");
                    return y * mGridColumnCount + x;
                }
            }
        }

        return -1;
    }

    public boolean updateTexture(int slot, Bitmap image) {
        if (!validateSlot(slot)) {
            return false;
        }

        Bitmap dstImage = image;
        int dstWidth = image.getWidth();
        int dstHeight = image.getHeight();

        // Resize to maximum limited size
        if (image.getWidth() > SINGLE_IMAGE_MAXIMUM_WIDTH
                || image.getHeight() > SINGLE_IMAGE_MAXIMUM_HEIGHT) {
            float scale = Math.min(((float) SINGLE_IMAGE_MAXIMUM_WIDTH / image.getWidth()),
                                   ((float) SINGLE_IMAGE_MAXIMUM_HEIGHT / image.getHeight()));
            dstWidth = (int)(scale * image.getWidth());
            dstHeight = (int)(scale * image.getHeight());
            dstImage = Bitmap.createScaledBitmap(image, dstWidth, dstHeight, false);
        }

        // Allocate Raw buffer
        ByteBuffer imageBuffer = ByteBuffer.allocate(dstWidth * dstHeight * 4); // RGBA
        dstImage.copyPixelsToBuffer(imageBuffer);
        imageBuffer.position(0);

        // Update texture
        Point pos = getOffsetInPixel(slot);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexId);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0,
                pos.x, pos.y, dstWidth, dstHeight,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, imageBuffer);

        // Keep source image
        mSourceImages[slot] = dstImage;

        return true;
    }

    public boolean draw(int slot, RectF srcRect, RectF dstRect) {
        if (!validateSlot(slot)) {
            return false;
        }

        float posX = dstRect.left;
        float posY = 1.0f - dstRect.bottom;
        mRect.setPosition(posX, posY);
        mRect.setRotation(0.0f);
        mRect.setScale(dstRect.width(), dstRect.height());

        // TODO: Multiply texCoordArray with srcRect
        FloatBuffer texCoordArray = getTexCoordArray(slot, mSourceImages[slot].getWidth(), mSourceImages[slot].getHeight());
        mRect.draw(mTexPgm, mDisplayProjectionMatrix, texCoordArray);

        return true;
    }

    public boolean draw(int slot, float posX, float posY, float width, float height) {
        if (!validateSlot(slot)) {
            return false;
        }

        mRect.setPosition(posX, posY);
        mRect.setRotation(0.0f);
        mRect.setScale(width, height);

        // TODO: Multiply texCoordArray with srcRect
        FloatBuffer texCoordArray = getTexCoordArray(slot, mSourceImages[slot].getWidth(), mSourceImages[slot].getHeight());
        mRect.draw(mTexPgm, mDisplayProjectionMatrix, texCoordArray);

        return true;
    }

    public void releaseSlot(int slot) {
        int y = slot / mGridColumnCount;
        int x = slot - y * mGridColumnCount;

        if (x >= mGridColumnCount || y >= mGridRowCount) {
            Log.e(TAG, "Invalid slot index: " + slot);
            return;
        }

        if (0 == mSlots[y][x]) {
            Log.e(TAG, "Slot [" + y + "][" + x + "] already idle.");
            return;
        }

        //Log.d(TAG, "Slot [" + y + "][" + x + "] released!");
        mSourceImages[slot] = null;
        mSlots[y][x] = 0;
    }

    private PointF getOffset(int slot) {
        int y = slot / mGridColumnCount;
        int x = slot - y * mGridColumnCount;

        if (x >= mGridColumnCount || y >= mGridRowCount) {
            Log.e(TAG, "Invalid slot index: " + slot);
            return new PointF(-1.0f, -1.0f);
        }

        float fx = (float)x / (float) mGridColumnCount;
        float fy = (float)y / (float) mGridRowCount;
        return new PointF(fx, fy);
    }

    private Point getOffsetInPixel(int slot) {
        int y = slot / mGridColumnCount;
        int x = slot - y * mGridColumnCount;

        if (x >= mGridColumnCount || y >= mGridRowCount) {
            Log.e(TAG, "Invalid slot index: " + slot);
            return new Point(-SINGLE_IMAGE_MAXIMUM_WIDTH, -SINGLE_IMAGE_MAXIMUM_HEIGHT);
        }

        int ix = x * SINGLE_IMAGE_MAXIMUM_WIDTH;
        int iy = y * SINGLE_IMAGE_MAXIMUM_HEIGHT;
        return new Point(ix, iy);
    }

    private FloatBuffer getTexCoordArray(int slot, int width, int height) {
        PointF pos = getOffset(slot);
        RectF rect = new RectF(pos.x, pos.y,
                pos.x + (float)width / getTotalWidth(),
                pos.y + (float)height / getTotalHeight());
        float[] textureCoordArray = {
                rect.left, rect.bottom,   // 0 bottom left
                rect.right, rect.bottom,  // 1 bottom right
                rect.left, rect.top,      // 2 top left
                rect.right, rect.top      // 3 top right
        };

        return GlUtil.createFloatBuffer(textureCoordArray);
    }
}
