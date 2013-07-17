#############
# PJSIP-JNI #
#############

LOCAL_PATH := $(call my-dir)/../../sources/pjsip-apps/

include $(CLEAR_VARS)
LOCAL_MODULE    := pjsip-jni

LOCAL_C_INCLUDES := $(LOCAL_PATH)../pjsip/include \
		$(LOCAL_PATH)../pjlib/include/ $(LOCAL_PATH)../pjlib-util/include/ \
		$(LOCAL_PATH)../pjnath/include $(LOCAL_PATH)../pjmedia/include \
		$(LOCAL_PATH)../../android_sources/pjmedia/include

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := src/pjsua

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/pjsua_app.c $(PJLIB_SRC_DIR)/ptt_extension.c\
	$(PJLIB_SRC_DIR)/main.c

#fuck,because the libs sequence:pjsip->pjmedia->(third party)->pjlib-util->pjlib
LOCAL_STATIC_LIBRARIES := pjsip pjnath pjmedia srtp resample g7221 ilbc gsm speex pjlib-util pjlib

#for csipsimple-wrapper
#LOCAL_STATIC_LIBRARIES+=swig-glue pjsipwrapper
#LOCAL_LDLIBS := -ldl

LOCAL_LDLIBS := -llog

#include $(BUILD_EXECUTABLE)
include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_STATIC_LIBRARY)

