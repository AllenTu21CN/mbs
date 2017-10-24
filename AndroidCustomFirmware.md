Android Custom Firmware

1.开机logo

	在kernel根目录下，存放有logo.bmp和logo_kernel.bmp两张图片，在编译内核后，他们会被打包到resource.img中。
	开机启动时，uboot会从中读取logo.bmp并显示出来。默认uboot只支持8b模式的图像读取，且必须为BMP格式。
	找一张分辨率不是太大的图片，转换成BMP格式，在WINDOWS的图片编辑软件下转换成8b模式后，替换kernel根目录下的logo.bmp即可。
	注意，如果分辨率过大，可能会无法显示。

2.android开机动画

	在相应device的设备mk文件路径（如rk3288/rk3288_box/）下创建sanbu目录，拷贝事先准备好的bootanimation.zip。并编辑相应的mk文件增加一行 如：
	PRODUCT_COPY_FILES += \
	device/rockchip/rk3399/x3399/sanbu/bootanimation.zip:/system/media/bootanimation.zip

3.去除System Bar

	修改frameworks/base/core/res/res/values/dimens.xml
	为：
		<dimen name="status_bar_height">0dip</dimen>
		<dimen name="status_bar_height_mul">0dip</dimen>
		<!-- Height of the bottom navigation / system bar. -->
		<dimen name="navigation_bar_height">0dp</dimen>
		<!-- Height of the bottom navigation bar in portrait; often the same as @dimen/navigation_bar_height -->
		<dimen name="navigation_bar_height_landscape">0dp</dimen>
		<!-- Width of the navigation bar when it is placed vertically on the screen -->
		<dimen name="navigation_bar_width">0dp</dimen>

4.增加字体

	复制custom Xxx.ttf into frameworks/base/data/fonts
	Modify framworks/base/data/fonts/Android.mk ，Add your custom font into list of ‘font_src_files’
	修改同目录下的Android.mk文件，将Xxx.ttf文件添加到‘font_src_files ’，具体如下：
	font_src_files := \ 
	Roboto-Regular.ttf \ 
	…. 
	AndroidClock_Solid.ttf \ 
	Xxx.ttf \Modify frameworks/base/data/fonts/fonts.mk ，Add your custom font into list of PRODUCT_PACKAGES
	修改同目录下的fonts.mk文件，在PRODUCT_PACKAGES末尾添加Xxx.ttf文件，如下：
	PRODUCT_PACKAGES := \ 
	DroidSansFallback.ttf \ 
	… 
	AndroidClock_Solid.ttf \ 
	Xxx.ttf \


5.预装apk

	第一情况、如何将带源码的APK预置进系统？
	1) 在 packages/apps 下面以需要预置的 APK的 名字创建一个新文件夹，以预置一个名为Test的APK 为例
	2) 将 Test APK的Source code 拷贝到 Test 文件夹下，删除 /bin 和 /gen 目录
	3) 在 Test 目录下创建一个名为 Android.mk的文件，内容如下：
	LOCAL_PATH:= $(call my-dir)
	include $(CLEAR_VARS)

	LOCAL_MODULE_TAGS := optional
	LOCAL_SRC_FILES := $(call all-subdir-java-files)

	LOCAL_PACKAGE_NAME := Test
	include $(BUILD_PACKAGE)
	4) 打开文件 device\mediatek\common\device.mk
	将 Test 添加到 PRODUCT_PACKAGES 里面。
	PRODUCT_PACKAGES += Test
	5) 重新 build 整个工程

	第二情况、如何将无源码的 APK 预置进系统？ 
	1) 在 packages/apps 下面以需要预置的 APK 名字创建文件夹，以预置一个名为Test的APK为例
	2) 将 Test.apk 放到 packages/apps/Test 下面
	3) 在 packages/apps/Test 下面创建文件 Android.mk，文件内容如下：
	LOCAL_PATH := $(call my-dir)
	include $(CLEAR_VARS)
	# Module name should match apk name to be installed
	LOCAL_MODULE := Test
	LOCAL_MODULE_TAGS := optional

	LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
	LOCAL_MODULE_CLASS := APPS
	LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

	LOCAL_PREBUILT_JNI_LIBS:= \
	@lib/armeabi/libtest.so \
	@lib/armeabi/libtest2.so 

	LOCAL_CERTIFICATE := PRESIGNED
	include $(BUILD_PREBUILT)

	若无so，删除LOCAL_PREBUILT_JNI_LIBS
	若有so，使用LOCAL_PREBUILT_JNI_LIBS列出所有so的路径，不要忘记使用@。@标识符会将apk中的so抽离出来build进apk同级目录下的lib文件夹中

	若apk支持不同cpu类型的so，针对so的部分的处理:
	Ifeq ($(TARGET_ARCH),arm)
	LOCAL_PREBUILT_JNI_LIBS := \
	@lib/armeabi-v7a/xxx.so\
	@ lib/armeabi-v7a/xxxx.so
	else ifeq ($(TARGET_ARCH),x86)
	LOCAL_PREBUILT_JNI_LIBS := \
	@lib/x86/xxx.so
	else ifeq ($(TARGET_ARCH),arm64)
	LOCAL_PREBUILT_JNI_LIBS := \
	@lib/armeabi-v8a/xxx.so
	…
	即将和TARGET_ARCH对应的so抽离出来

	4) 打开文件 device\mediatek\common\device.mk
	将 Test 添加到 PRODUCT_PACKAGES 里面。
	PRODUCT_PACKAGES += Test
	5) 重新 build 整个工程

	注：如果App使用System Level的permission，需要预置到/system/priv-app底下 (原在/system/app)。
	修改Android.mk，增加LOCAL_PRIVILEGED_MODULE := true，以声明app需要放在/system/priv-app下。

	第三情况、如何预置APK使得用户可以卸载，恢复出厂设置时不能恢复？
	1) 在 packages/apps 下面以需要预置的 APK 名字创建文件夹，以预置一个名为Test的APK为例
	2) 将 Test.apk 放到 packages/apps/Test 下面
	3) 在 packages/apps/Test 下面创建文件 Android.mk，文件内容如下：

	LOCAL_PATH := $(call my-dir)
	include $(CLEAR_VARS)

	# Module name should match apk name to be installed
	LOCAL_MODULE := Test
	LOCAL_MODULE_TAGS := optional

	LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
	LOCAL_MODULE_CLASS := APPS
	LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
	# LOCAL_PRIVILEGED_MODULE := true
	LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)

	LOCAL_CERTIFICATE := PRESIGNED
	include $(BUILD_PREBUILT)

	4) 打开文件 device\mediatek\common\device.mk
	将 Test 添加到 PRODUCT_PACKAGES 里面。
	PRODUCT_PACKAGES += Test
	5) 重新 build 整个工程
	注意：这个比不能卸载的多了一句
	LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)

	第四情况、如何预置APK使得用户可以卸载，并且恢复出厂设置时能够恢复？
	1在 vendor\mediatek\proprietary\binary\3rd-party\free下面以需要预置的 APK 名字创建文件夹，以预置一个名为Test的APK为例
	2 将Test.apk 放入vendor\mediatek\proprietary\binary\3rd-party\free\Test下面
	3 在vendor\mediatek\proprietary\binary\3rd-party\free\Test 下面创建文件 Android.mk，文件内容如下

	LOCAL_PATH := $(call my-dir)
	include $(CLEAR_VARS)

	# Module name should match apk name to be installed
	LOCAL_MODULE := Test
	LOCAL_MODULE_TAGS := optional
	LOCAL_SRC_FILES := $(LOCAL_MODULE).apk

	LOCAL_MODULE_CLASS := APPS
	LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
	LOCAL_CERTIFICATE := PRESIGNED

	LOCAL_MODULE_PATH := $(TARGET_OUT)/vendor/operator/app
	include $(BUILD_PREBUILT)

	2 打开文件device\mediatek\common\device.mk
	将 Test 添加到 PRODUCT_PACKAGES 里面。
	PRODUCT_PACKAGES += Test
	3 然后重新build整个工程


	若需要apk作为32bit的apk运行，则需要在Android.mk中定义
	LOCAL_MULTILIB :=32



6.设置默认输入法（SogouInput）

	在预装apk的情况（下面以sogouTv输入法为例）
	1.  frameworks\base\packages\SettingsProvider\res\values\defaults.xml 文件中修改默认输入法为搜狗输入法
	        <stringname="def_enabled_input_methods" translatable="false">com.sohu.inputmethod.sogou.tv/.SogouIME
	        </string>
	 
	2.  frameworks\base\packages\SettingsProvider\src\com\Android\providers\settings\DatabaseHelper.Java  
	        在loadSecureSettings()中增加一条语句，制定默认使能的输入法  loadStringSetting(stmt, Settings.Secure.DEFAULT_INPUT_METHOD,R.string.def_enabled_input_methods);

7.去除默认桌面Launcher3

	在去除默认桌面Launcher3情况下，且只有一个Launcher应用，安卓系统就会使用我们的桌面。
	注释Launcher3源码中AndroidManifest.xml文件，含有android.intent.category.HOME android.intent.category.LAUNCHER的xml配置即可

