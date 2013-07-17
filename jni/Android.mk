
LOCAL_PATH := $(call my-dir)/
JNI_PATH := $(LOCAL_PATH)

# Build pjsip for android
include $(JNI_PATH)/pjsip/android_toolchain/Android.mk
