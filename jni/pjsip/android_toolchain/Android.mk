LOCAL_PATH := $(call my-dir)

MY_PJSIP_FLAGS := $(BASE_PJSIP_FLAGS) -DPJ_HAS_FLOATING_POINT=0

# Build all sub dirs
include $(call all-subdir-makefiles)
include $(CLEAR_VARS)

