/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.sanbu.avalon.media.gles;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.sanbu.tools.LogUtil;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
public class CustomProgram {
    private static final String TAG = GlUtil.TAG;

    private static final int MAX_TEXTURE_ELEMENTS = 8;
    private static final int MAX_VARIABLE_ELEMENTS = 8;

    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
            "}\n";

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES texArray[" + MAX_TEXTURE_ELEMENTS + "];\n" +
            "uniform float varArray[" + MAX_VARIABLE_ELEMENTS + "];\n" +
            "\n" +
            "${CUSTOM_FUNC}\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = custom(vTextureCoord);\n" +
            "}\n";

    private String mName;
    private String mCustomFunc;

    // Handles to the GL program and various components of it.
    private int mProgramHandle;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int mTexArrayLoc;
    private int mVarArrayLoc;

    private Drawable2d mDrawable = new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
    private Sprite2d mRect = new Sprite2d(mDrawable);
    private float[] mScratchMatrix = new float[16];
    private int[] mTexArray = new int[MAX_TEXTURE_ELEMENTS];

    /**
     * Prepares the program in the current EGL context.
     */
    public CustomProgram(String name, String customFunc) {
        mName = name;
        mCustomFunc = customFunc;

        mRect.setPosition(0.0f, 0.0f);
        mRect.setRotation(0.0f);
        mRect.setScale(1.0f, 1.0f);

        // Compute model/view/projection matrix.
        float[] projectionMatrix = new float[16];
        Matrix.orthoM(projectionMatrix, 0, 0.0f, 1.0f, 0.0f, 1.0f, -1.0f, 1.0f);
        Matrix.multiplyMM(mScratchMatrix, 0, projectionMatrix, 0, mRect.getModelViewMatrix(), 0);

        String fragmentShader = FRAGMENT_SHADER_2D.replaceAll("\\$\\{CUSTOM_FUNC\\}", mCustomFunc);
        mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, fragmentShader);
        if (mProgramHandle == 0) {
            throw new RuntimeException("Unable to create transition program. name=" + mName + ", custom_func=" + mCustomFunc);
        }

        // Get locations of attributes and uniforms
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        GlUtil.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        GlUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");

        mTexArrayLoc = GLES20.glGetUniformLocation(mProgramHandle, "texArray");
        GlUtil.checkLocation(mTexArrayLoc, "texArray");
        mVarArrayLoc = GLES20.glGetUniformLocation(mProgramHandle, "varArray");
        //GlUtil.checkLocation(mVarArrayLoc, "varArray");

        for (int i = 0; i < MAX_TEXTURE_ELEMENTS; i++) {
            mTexArray[i] = i;
        }
    }

    /**
     * Releases the program.
     * <p>
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    public void release() {
        Log.d(TAG, "deleting program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    /**
     * Issues the draw call.  Does the full setup on every call.
     */
    public void draw(int[] textures, float[] variables) {
        GlUtil.checkGlError("draw start");

        if (textures.length > MAX_TEXTURE_ELEMENTS) {
            LogUtil.e(TAG, "Texture elements exceed maximum limit.");
            return;
        }

        //float[] mvpMatrix = GlUtil.IDENTITY_MATRIX;
        FloatBuffer vertexBuffer = mDrawable.getVertexArray();
        int firstVertex = 0;
        int vertexCount = mDrawable.getVertexCount();
        int coordsPerVertex = mDrawable.getCoordsPerVertex();
        int vertexStride = mDrawable.getVertexStride();
        float[] texMatrix = GlUtil.IDENTITY_MATRIX;
        FloatBuffer texBuffer = mDrawable.getTexCoordArray();
        int texStride = mDrawable.getTexCoordStride();

        // Select the program
        GLES20.glUseProgram(mProgramHandle);
        GlUtil.checkGlError("glUseProgram");

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mScratchMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
            GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GlUtil.checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, texStride, texBuffer);
            GlUtil.checkGlError("glVertexAttribPointer");

        // Set the texture
        for (int i = 0; i < textures.length; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[i]);
            GlUtil.checkGlError("glBindTexture");
        }

        GLES20.glUniform1iv(mTexArrayLoc, textures.length, mTexArray, 0);
        GlUtil.checkGlError("glUniform1iv");

        if (variables != null) {
            GLES20.glUniform1fv(mVarArrayLoc, variables.length, variables, 0);
            GlUtil.checkGlError("glUniform1fv");
        }

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
        GlUtil.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        for (int i = 0; i < textures.length; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }
        GLES20.glUseProgram(0);
    }
}
