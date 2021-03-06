LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle telephony-common telephony-msim android.urovo.device
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v13 jsr305

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)  \
  src/com/android/cabl/ICABLService.aidl \
  src/com/lava/security/services/IExternalService.aidl

LOCAL_SRC_FILES += \
  src/com/android/display/IPPService.aidl \
  src/com/android/location/XT/IXTSrv.aidl \
  src/com/android/location/XT/IXTSrvCb.aidl

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_AAPT_FLAGS += -c zz_ZZ

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
