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
import android.util.Log;

import java.nio.FloatBuffer;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
public class Texture2dProgram {
    private static final String TAG = GlUtil.TAG;

    public enum ProgramType {
        TEXTURE_2D,
        TEXTURE_2D_BILINEAR,
        TEXTURE_2D_BICUBIC,
        TEXTURE_EXT,
        TEXTURE_EXT_BW,
        TEXTURE_EXT_FILT,
        TEXTURE_EXT_BILINEAR,
        TEXTURE_EXT_BICUBIC,
        TEXTURE_EXT_LANCZOS
    }

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

    private static final String VERTEX_SHADER_BILINEAR =
            "#version 300 es\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "in vec4 aPosition;\n" +
            "in vec4 aTextureCoord;\n" +
            "out vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
            "}\n";

    private static final String VERTEX_SHADER_BICUBIC =
            "#version 300 es\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "in vec4 aPosition;\n" +
            "in vec4 aTextureCoord;\n" +
            "out vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
            "}\n";

    private static final String VERTEX_SHADER_LANCZOS =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "const float texelWidth = 1.0f / 1920.0f;\n" +
            "const float texelHeight = 1.0f / 1080.0f;\n" +
            "\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "\n" +
            "varying vec2 centerTextureCoordinate;\n" +
            "varying vec2 oneStepLeftTextureCoordinate;\n" +
            "varying vec2 twoStepsLeftTextureCoordinate;\n" +
            "varying vec2 threeStepsLeftTextureCoordinate;\n" +
            "varying vec2 fourStepsLeftTextureCoordinate;\n" +
            "varying vec2 oneStepRightTextureCoordinate;\n" +
            "varying vec2 twoStepsRightTextureCoordinate;\n" +
            "varying vec2 threeStepsRightTextureCoordinate;\n" +
            "varying vec2 fourStepsRightTextureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
            "    \n" +
            "    vec2 firstOffset = vec2(texelWidth, texelHeight);\n" +
            "    vec2 secondOffset = vec2(2.0 * texelWidth, 2.0 * texelHeight);\n" +
            "    vec2 thirdOffset = vec2(3.0 * texelWidth, 3.0 * texelHeight);\n" +
            "    vec2 fourthOffset = vec2(4.0 * texelWidth, 4.0 * texelHeight);\n" +
            "    \n" +
            "    centerTextureCoordinate = vTextureCoord;\n" +
            "    oneStepLeftTextureCoordinate = vTextureCoord - firstOffset;\n" +
            "    twoStepsLeftTextureCoordinate = vTextureCoord - secondOffset;\n" +
            "    threeStepsLeftTextureCoordinate = vTextureCoord - thirdOffset;\n" +
            "    fourStepsLeftTextureCoordinate = vTextureCoord - fourthOffset;\n" +
            "    oneStepRightTextureCoordinate = vTextureCoord + firstOffset;\n" +
            "    twoStepsRightTextureCoordinate = vTextureCoord + secondOffset;\n" +
            "    threeStepsRightTextureCoordinate = vTextureCoord + thirdOffset;\n" +
            "    fourStepsRightTextureCoordinate = vTextureCoord + fourthOffset;\n" +
            "}\n";

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER_2D_BILINEAR =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "\n" +
            "in vec2 vTextureCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform sampler2D sTexture;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 texSize = vec2(textureSize(sTexture, 0));\n" +
            "    vec2 invTexSize = 1.0 / texSize;\n" +
            "    vec2 pixel = vTextureCoord * texSize + 0.5;\n" +
            "\n" +
            "    vec2 frac = fract(pixel);\n" +
            "    pixel = (floor(pixel) / texSize) - vec2(invTexSize / 2.0);\n" +
            "\n" +
            "    vec3 C11 = texture(sTexture, pixel).rgb;\n" +
            "    vec3 C21 = texture(sTexture, pixel + vec2(invTexSize.x, 0.0)).rgb;\n" +
            "    vec3 C12 = texture(sTexture, pixel + vec2(0.0, invTexSize.y)).rgb;\n" +
            "    vec3 C22 = texture(sTexture, pixel + invTexSize).rgb;\n" +
            "\n" +
            "    vec3 x1 = mix(C11, C21, frac.x);\n" +
            "    vec3 x2 = mix(C12, C22, frac.x);\n" +
            "    fragColor = vec4(mix(x1, x2, frac.y), 1.0);\n" +
            "    fragColor.r = 1.0;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER_2D_BICUBIC =
            "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +
            "in vec2 vTextureCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform sampler2D sTexture;\n" +
            "\n" +
            "vec4 CubicHermite(vec4 A, vec4 B, vec4 C, vec4 D, float t)\n" +
            "{\n" +
            "    vec4 a = -A / 2.0f + (3.0f * B) / 2.0f - (3.0f * C) / 2.0f + D / 2.0f;\n" +
            "    vec4 b = A - (5.0f * B) / 2.0f + 2.0f * C - D / 2.0f;\n" +
            "    vec4 c = -A / 2.0f + C / 2.0f;\n" +
            "    vec4 d = B;\n" +
            "\n" +
            "    return a * t * t * t + b * t * t + c * t + d;\n" +
            "}\n" +
            "\n" +
            "vec4 textureBicubic(sampler2D tex, vec2 texCoords)\n" +
            "{\n" +
            "    vec2 texSize = vec2(textureSize(tex, 0));\n" +
            "    vec2 invTexSize = 1.0 / texSize;\n" +
            "\n" +
            "    // calculate coordinates -> also need to offset by half a pixel\n" +
            "    // to keep image from shifting down and left half a pixel\n" +
            "    float x = (texCoords.x * texSize.x) - 0.5;\n" +
            "    int xint = int(x);\n" +
            "    float xfract = x - floor(x);\n" +
            "\n" +
            "    float y = (texCoords.y * texSize.y) - 0.5;\n" +
            "    int yint = int(y);\n" +
            "    float yfract = y - floor(y);\n" +
            "\n" +
            "    // 1st row\n" +
            "    vec4 p00 = texelFetch(tex, ivec2(xint - 1, yint - 1), 0);\n" +
            "    vec4 p10 = texelFetch(tex, ivec2(xint,     yint - 1), 0);\n" +
            "    vec4 p20 = texelFetch(tex, ivec2(xint + 1, yint - 1), 0);\n" +
            "    vec4 p30 = texelFetch(tex, ivec2(xint + 2, yint - 1), 0);\n" +
            "\n" +
            "    // 2nd row\n" +
            "    vec4 p01 = texelFetch(tex, ivec2(xint - 1, yint), 0);\n" +
            "    vec4 p11 = texelFetch(tex, ivec2(xint,     yint), 0);\n" +
            "    vec4 p21 = texelFetch(tex, ivec2(xint + 1, yint), 0);\n" +
            "    vec4 p31 = texelFetch(tex, ivec2(xint + 2, yint), 0);\n" +
            "\n" +
            "    // 3rd row\n" +
            "    vec4 p02 = texelFetch(tex, ivec2(xint - 1, yint + 1), 0);\n" +
            "    vec4 p12 = texelFetch(tex, ivec2(xint,     yint + 1), 0);\n" +
            "    vec4 p22 = texelFetch(tex, ivec2(xint + 1, yint + 1), 0);\n" +
            "    vec4 p32 = texelFetch(tex, ivec2(xint + 2, yint + 1), 0);\n" +
            "\n" +
            "    // 4th row\n" +
            "    vec4 p03 = texelFetch(tex, ivec2(xint - 1, yint + 2), 0);\n" +
            "    vec4 p13 = texelFetch(tex, ivec2(xint,     yint + 2), 0);\n" +
            "    vec4 p23 = texelFetch(tex, ivec2(xint + 1, yint + 2), 0);\n" +
            "    vec4 p33 = texelFetch(tex, ivec2(xint + 2, yint + 2), 0);\n" +
            "\n" +
            "    // interpolate bi-cubically!\n" +
            "    vec4 col0 = CubicHermite(p00, p10, p20, p30, xfract);\n" +
            "    vec4 col1 = CubicHermite(p01, p11, p21, p31, xfract);\n" +
            "    vec4 col2 = CubicHermite(p02, p12, p22, p32, xfract);\n" +
            "    vec4 col3 = CubicHermite(p03, p13, p23, p33, xfract);\n" +
            "\n" +
            "    vec4 value = CubicHermite(col0, col1, col2, col3, yfract);\n" +
            "    value.a = 1.0;\n" +
            "    return value;\n" +
            "}\n" +
            "void main() {\n" +
            "    fragColor = textureBicubic(sTexture, vTextureCoord);\n" +
            "}\n";

    // Simple fragment shader for use with external 2D textures (e.g. what we get from
    // SurfaceTexture).
    private static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER_EXT_BILINEAR =
            "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +
            "in vec2 vTextureCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 texSize = vec2(textureSize(sTexture, 0));\n" +
            "    vec2 invTexSize = 1.0 / texSize;\n" +
            "    vec2 pixel = vTextureCoord * texSize + 0.5;\n" +
            "\n" +
            "    vec2 frac = fract(pixel);\n" +
            "    pixel = (floor(pixel) / texSize) - vec2(invTexSize / 2.0);\n" +
            "\n" +
            "    vec3 C11 = texture(sTexture, pixel).rgb;\n" +
            "    vec3 C21 = texture(sTexture, pixel + vec2(invTexSize.x, 0.0)).rgb;\n" +
            "    vec3 C12 = texture(sTexture, pixel + vec2(0.0, invTexSize.y)).rgb;\n" +
            "    vec3 C22 = texture(sTexture, pixel + invTexSize).rgb;\n" +
            "\n" +
            "    vec3 x1 = mix(C11, C21, frac.x);\n" +
            "    vec3 x2 = mix(C12, C22, frac.x);\n" +
            "    fragColor = vec4(mix(x1, x2, frac.y), 1.0);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER_EXT_BICUBIC =
            "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +
            "in vec2 vTextureCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "\n" +
                    "vec4 CubicHermite(vec4 A, vec4 B, vec4 C, vec4 D, float t)\n" +
                    "{\n" +
                    "    vec4 a = -A / 2.0f + (3.0f * B) / 2.0f - (3.0f * C) / 2.0f + D / 2.0f;\n" +
                    "    vec4 b = A - (5.0f * B) / 2.0f + 2.0f * C - D / 2.0f;\n" +
                    "    vec4 c = -A / 2.0f + C / 2.0f;\n" +
                    "    vec4 d = B;\n" +
                    "\n" +
                    "    return a * t * t * t + b * t * t + c * t + d;\n" +
                    "}\n" +
                    "\n" +
                    "vec4 textureBicubic(sampler2D tex, vec2 texCoords)\n" +
                    "{\n" +
                    "    vec2 texSize = vec2(textureSize(tex, 0));\n" +
                    "    vec2 invTexSize = 1.0 / texSize;\n" +
                    "\n" +
                    "    // calculate coordinates -> also need to offset by half a pixel\n" +
                    "    // to keep image from shifting down and left half a pixel\n" +
                    "    float x = (texCoords.x * texSize.x) - 0.5;\n" +
                    "    int xint = int(x);\n" +
                    "    float xfract = x - floor(x);\n" +
                    "\n" +
                    "    float y = (texCoords.y * texSize.y) - 0.5;\n" +
                    "    int yint = int(y);\n" +
                    "    float yfract = y - floor(y);\n" +
                    "\n" +
                    "    // 1st row\n" +
                    "    vec4 p00 = texelFetch(tex, ivec2(xint - 1, yint - 1), 0);\n" +
                    "    vec4 p10 = texelFetch(tex, ivec2(xint,     yint - 1), 0);\n" +
                    "    vec4 p20 = texelFetch(tex, ivec2(xint + 1, yint - 1), 0);\n" +
                    "    vec4 p30 = texelFetch(tex, ivec2(xint + 2, yint - 1), 0);\n" +
                    "\n" +
                    "    // 2nd row\n" +
                    "    vec4 p01 = texelFetch(tex, ivec2(xint - 1, yint), 0);\n" +
                    "    vec4 p11 = texelFetch(tex, ivec2(xint,     yint), 0);\n" +
                    "    vec4 p21 = texelFetch(tex, ivec2(xint + 1, yint), 0);\n" +
                    "    vec4 p31 = texelFetch(tex, ivec2(xint + 2, yint), 0);\n" +
                    "\n" +
                    "    // 3rd row\n" +
                    "    vec4 p02 = texelFetch(tex, ivec2(xint - 1, yint + 1), 0);\n" +
                    "    vec4 p12 = texelFetch(tex, ivec2(xint,     yint + 1), 0);\n" +
                    "    vec4 p22 = texelFetch(tex, ivec2(xint + 1, yint + 1), 0);\n" +
                    "    vec4 p32 = texelFetch(tex, ivec2(xint + 2, yint + 1), 0);\n" +
                    "\n" +
                    "    // 4th row\n" +
                    "    vec4 p03 = texelFetch(tex, ivec2(xint - 1, yint + 2), 0);\n" +
                    "    vec4 p13 = texelFetch(tex, ivec2(xint,     yint + 2), 0);\n" +
                    "    vec4 p23 = texelFetch(tex, ivec2(xint + 1, yint + 2), 0);\n" +
                    "    vec4 p33 = texelFetch(tex, ivec2(xint + 2, yint + 2), 0);\n" +
                    "\n" +
                    "    // interpolate bi-cubically!\n" +
                    "    vec4 col0 = CubicHermite(p00, p10, p20, p30, xfract);\n" +
                    "    vec4 col1 = CubicHermite(p01, p11, p21, p31, xfract);\n" +
                    "    vec4 col2 = CubicHermite(p02, p12, p22, p32, xfract);\n" +
                    "    vec4 col3 = CubicHermite(p03, p13, p23, p33, xfract);\n" +
                    "\n" +
                    "    vec4 value = CubicHermite(col0, col1, col2, col3, yfract);\n" +
                    "    value.a = 1.0;\n" +
                    "    return value;\n" +
                    "}\n" +
            "void main() {\n" +
            "    fragColor = textureBicubic(sTexture, vTextureCoord);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER_EXT_LANCZOS =
            "#extension GL_OES_EGL_image_external : require\n" +
            "\n" +
            "precision highp float;\n" +
            "\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "\n" +
            "varying vec2 centerTextureCoordinate;\n" +
            "varying vec2 oneStepLeftTextureCoordinate;\n" +
            "varying vec2 twoStepsLeftTextureCoordinate;\n" +
            "varying vec2 threeStepsLeftTextureCoordinate;\n" +
            "varying vec2 fourStepsLeftTextureCoordinate;\n" +
            "varying vec2 oneStepRightTextureCoordinate;\n" +
            "varying vec2 twoStepsRightTextureCoordinate;\n" +
            "varying vec2 threeStepsRightTextureCoordinate;\n" +
            "varying vec2 fourStepsRightTextureCoordinate;\n" +
            "\n" +
            "// sinc(x) * sinc(x/a) = (a * sin(pi * x) * sin(pi * x / a)) / (pi^2 * x^2)\n" +
            "// Assuming a Lanczos constant of 2.0, and scaling values to max out at x = +/- 1.5\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    lowp vec4 fragmentColor = texture2D(sTexture, centerTextureCoordinate) * 0.38026;\n" +
            "    \n" +
            "    fragmentColor += texture2D(sTexture, oneStepLeftTextureCoordinate) * 0.27667;\n" +
            "    fragmentColor += texture2D(sTexture, oneStepRightTextureCoordinate) * 0.27667;\n" +
            "    \n" +
            "    fragmentColor += texture2D(sTexture, twoStepsLeftTextureCoordinate) * 0.08074;\n" +
            "    fragmentColor += texture2D(sTexture, twoStepsRightTextureCoordinate) * 0.08074;\n" +
            "\n" +
            "    fragmentColor += texture2D(sTexture, threeStepsLeftTextureCoordinate) * -0.02612;\n" +
            "    fragmentColor += texture2D(sTexture, threeStepsRightTextureCoordinate) * -0.02612;\n" +
            "\n" +
            "    fragmentColor += texture2D(sTexture, fourStepsLeftTextureCoordinate) * -0.02143;\n" +
            "    fragmentColor += texture2D(sTexture, fourStepsRightTextureCoordinate) * -0.02143;\n" +
            "\n" +
            "    gl_FragColor = fragmentColor;\n" +
            "}\n";

    // Fragment shader that converts color to black & white with a simple transformation.
    private static final String FRAGMENT_SHADER_EXT_BW =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
            "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
            "    gl_FragColor = vec4(color, color, color, 1.0);\n" +
            "}\n";

    // Fragment shader with a convolution filter.  The upper-left half will be drawn normally,
    // the lower-right half will have the filter applied, and a thin red line will be drawn
    // at the border.
    //
    // This is not optimized for performance.  Some things that might make this faster:
    // - Remove the conditionals.  They're used to present a half & half view with a red
    //   stripe across the middle, but that's only useful for a demo.
    // - Unroll the loop.  Ideally the compiler does this for you when it's beneficial.
    // - Bake the filter kernel into the shader, instead of passing it through a uniform
    //   array.  That, combined with loop unrolling, should reduce memory accesses.
    public static final int KERNEL_SIZE = 9;
    private static final String FRAGMENT_SHADER_EXT_FILT =
            "#extension GL_OES_EGL_image_external : require\n" +
            "#define KERNEL_SIZE " + KERNEL_SIZE + "\n" +
            "precision highp float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform float uKernel[KERNEL_SIZE];\n" +
            "uniform vec2 uTexOffset[KERNEL_SIZE];\n" +
            "uniform float uColorAdjust;\n" +
            "void main() {\n" +
            "    int i = 0;\n" +
            "    vec4 sum = vec4(0.0);\n" +
            "    if (vTextureCoord.x < vTextureCoord.y - 0.002) {\n" +
            "        for (i = 0; i < KERNEL_SIZE; i++) {\n" +
            "            vec4 texc = texture2D(sTexture, vTextureCoord + uTexOffset[i]);\n" +
            "            sum += texc * uKernel[i];\n" +
            "        }\n" +
            "    sum += uColorAdjust;\n" +
            "    } else if (vTextureCoord.x > vTextureCoord.y + 0.005) {\n" +
            "        sum = texture2D(sTexture, vTextureCoord);\n" +
            "    } else {\n" +
            "        sum.r = 1.0;\n" +
            "    }\n" +
            "    gl_FragColor = sum;\n" +
            "}\n";

    private ProgramType mProgramType;

    // Handles to the GL program and various components of it.
    private int mProgramHandle;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int muKernelLoc;
    private int muTexOffsetLoc;
    private int muColorAdjustLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;

    private int mTextureTarget;

    private float[] mKernel = new float[KERNEL_SIZE];
    private float[] mTexOffset;
    private float mColorAdjust;


    /**
     * Prepares the program in the current EGL context.
     */
    public Texture2dProgram(ProgramType programType) {
        mProgramType = programType;

        switch (programType) {
            case TEXTURE_2D:
                mTextureTarget = GLES20.GL_TEXTURE_2D;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D);
                break;
            case TEXTURE_2D_BILINEAR:
                mTextureTarget = GLES20.GL_TEXTURE_2D;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER_BILINEAR, FRAGMENT_SHADER_2D_BILINEAR);
                break;
            case TEXTURE_2D_BICUBIC:
                mTextureTarget = GLES20.GL_TEXTURE_2D;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER_BICUBIC, FRAGMENT_SHADER_2D_BICUBIC);
                break;
            case TEXTURE_EXT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
                break;
            case TEXTURE_EXT_BW:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW);
                break;
            case TEXTURE_EXT_FILT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT);
                break;
            case TEXTURE_EXT_BILINEAR:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER_BILINEAR, FRAGMENT_SHADER_EXT_BILINEAR);
                break;
            case TEXTURE_EXT_BICUBIC:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER_BICUBIC, FRAGMENT_SHADER_EXT_BICUBIC);
                break;
            case TEXTURE_EXT_LANCZOS:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER_LANCZOS, FRAGMENT_SHADER_EXT_LANCZOS);
                break;
            default:
                throw new RuntimeException("Unhandled type " + programType);
        }
        if (mProgramHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }
        Log.d(TAG, "Created program " + mProgramHandle + " (" + programType + ")");

        // get locations of attributes and uniforms

        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        GlUtil.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        GlUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");
        muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel");
        if (muKernelLoc < 0) {
            // no kernel in this one
            muKernelLoc = -1;
            muTexOffsetLoc = -1;
            muColorAdjustLoc = -1;
        } else {
            // has kernel, must also have tex offset and color adj
            muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset");
            GlUtil.checkLocation(muTexOffsetLoc, "uTexOffset");
            muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust");
            GlUtil.checkLocation(muColorAdjustLoc, "uColorAdjust");

            // initialize default values
            //setKernel(new float[] {0.0625f, 0.125f, 0.0625f,  0.125f, 0.25f, 0.125f,  0.0625f, 0.125f, 0.0625f}, 0f);
            //setKernel(new float[] {0.0f, -0.032f, 0.0f, 0.284f, 0.496f, 0.284f, 0.0f, -0.032f, 0.0f}, 0f);
            setKernel(new float[] {
                    0f, -1f, 0f,
                    -1f, 5f, -1f,
                    0f, -1f, 0f }, 0f);
            setTexSize(1920, 1080);
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
     * Returns the program type.
     */
    public ProgramType getProgramType() {
        return mProgramType;
    }

    /**
     * Creates a texture object suitable for use with this program.
     * <p>
     * On exit, the texture will be bound.
     */
    public int createTextureObject(int filteringMethod) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GlUtil.checkGlError("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
        GlUtil.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filteringMethod);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filteringMethod);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, filteringMethod);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, filteringMethod);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GlUtil.checkGlError("glTexParameter");

        return texId;
    }

    /**
     * Configures the convolution filter values.
     *
     * @param values Normalized filter values; must be KERNEL_SIZE elements.
     */
    public void setKernel(float[] values, float colorAdj) {
        if (values.length != KERNEL_SIZE) {
            throw new IllegalArgumentException("Kernel size is " + values.length +
                    " vs. " + KERNEL_SIZE);
        }
        System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE);
        mColorAdjust = colorAdj;
        //Log.d(TAG, "filt kernel: " + Arrays.toString(mKernel) + ", adj=" + colorAdj);
    }

    /**
     * Sets the size of the texture.  This is used to find adjacent texels when filtering.
     */
    public void setTexSize(int width, int height) {
        float rw = 1.0f / width;
        float rh = 1.0f / height;

        // Don't need to create a new array here, but it's syntactically convenient.
        mTexOffset = new float[] {
            -rw, -rh,   0f, -rh,    rw, -rh,
            -rw, 0f,    0f, 0f,     rw, 0f,
            -rw, rh,    0f, rh,     rw, rh
        };
        //Log.d(TAG, "filt size: " + width + "x" + height + ": " + Arrays.toString(mTexOffset));
    }

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix The 4x4 projection matrix.
     * @param vertexBuffer Buffer with vertex position data.
     * @param firstVertex Index of first vertex to use in vertexBuffer.
     * @param vertexCount Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride Width, in bytes, of the position data for each vertex (often
     *        vertexCount * sizeof(float)).
     * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
     *        for use with SurfaceTexture.)
     * @param texBuffer Buffer with vertex texture data.
     * @param texStride Width, in bytes, of the texture data for each vertex.
     */
    public void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
            int vertexCount, int coordsPerVertex, int vertexStride,
            float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
        GlUtil.checkGlError("draw start");

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        GlUtil.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
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

        // Populate the convolution kernel, if present.
        if (muKernelLoc >= 0) {
            GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0);
            GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0);
            GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
        }

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
        GlUtil.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }
}
