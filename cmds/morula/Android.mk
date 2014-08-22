LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	morula.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
	libbinder \
	libandroid_runtime

LOCAL_MODULE:= morula

include $(BUILD_EXECUTABLE)


# Build a variant of app_process binary linked with ASan runtime.
# ARM-only at the moment.
ifeq ($(TARGET_ARCH),arm)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	morula.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
	libbinder \
	libandroid_runtime

LOCAL_MODULE := morula__asan
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE_PATH := $(TARGET_OUT_EXECUTABLES)/asan
LOCAL_MODULE_STEM := morula
LOCAL_ADDRESS_SANITIZER := true

include $(BUILD_EXECUTABLE)

endif # ifeq($(TARGET_ARCH),arm)
