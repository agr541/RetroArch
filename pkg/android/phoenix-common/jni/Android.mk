LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

RARCH_DIR := ../../../..

HAVE_NEON   := 1
HAVE_LOGGER := 0
HAVE_VULKAN := 1
HAVE_CHEEVOS := 1
HAVE_FILE_LOGGER := 1
HAVE_GFX_WIDGETS := 1
HAVE_SAF := 1
HAVE_BUILTINSMBCLIENT := 1

INCFLAGS :=
DEFINES  :=

LIBRETRO_COMM_DIR := $(RARCH_DIR)/libretro-common
DEPS_DIR := $(RARCH_DIR)/deps

GIT_VERSION := $(shell git rev-parse --short HEAD 2>/dev/null)
ifneq ($(GIT_VERSION),)
   DEFINES += -DHAVE_GIT_VERSION -DGIT_VERSION=$(GIT_VERSION)
endif

# -------------------------------------------------------
# ARCH DETECTION (FIXED + CLEANED)
# -------------------------------------------------------

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
   DEFINES += -DANDROID_ARM -DANDROID_ARM_V7
   LOCAL_ARM_MODE := arm

   ifeq ($(HAVE_NEON),1)
      DEFINES += -D__ARM_NEON__ -DHAVE_NEON
   endif
endif

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
   DEFINES += -DANDROID_AARCH64
endif

ifeq ($(TARGET_ARCH_ABI),x86)
   DEFINES += -DANDROID_X86 -DHAVE_SSSE3
endif

ifeq ($(TARGET_ARCH_ABI),x86_64)
   DEFINES += -DANDROID_X64
endif

# REMOVE mips entirely (obsolete + breaks modern builds)
# -------------------------------------------------------

LOCAL_MODULE := retroarch-activity

LOCAL_SRC_FILES += \
   $(RARCH_DIR)/griffin/griffin.c \
   $(RARCH_DIR)/griffin/griffin_cpp.cpp

# -------------------------------------------------------
# SMB CLIENT
# -------------------------------------------------------

ifeq ($(HAVE_BUILTINSMBCLIENT),1)
   DEFINES += -DHAVE_BUILTINSMBCLIENT
   DEFINES += "-D_U_=__attribute__((unused))"
   DEFINES += -DHAVE_SMBCLIENT
endif

# -------------------------------------------------------
# LOGGER
# -------------------------------------------------------

ifeq ($(HAVE_LOGGER),1)
   DEFINES += -DHAVE_LOGGER
endif

LOGGER_LDLIBS := -llog

# -------------------------------------------------------
# GLES
# -------------------------------------------------------

ifeq ($(GLES),3)
   GLES_LIB := -lGLESv3
   DEFINES += -DHAVE_OPENGLES3
else
   GLES_LIB := -lGLESv2
   DEFINES += -DHAVE_OPENGLES2
endif

# -------------------------------------------------------
# CORE DEFINES (UNCHANGED BUT CLEANED)
# -------------------------------------------------------

DEFINES += \
   -DRARCH_MOBILE \
   -DHAVE_GRIFFIN \
   -DANDROID \
   -DHAVE_DYNAMIC \
   -DHAVE_OPENGL \
   -DHAVE_OPENGLES \
   -DHAVE_EGL \
   -DHAVE_GLSL \
   -DHAVE_MENU \
   -DHAVE_THREADS \
   -D__LIBRETRO__

# -------------------------------------------------------
# OPTIONAL FEATURES
# -------------------------------------------------------

ifeq ($(HAVE_VULKAN),1)
   DEFINES += -DHAVE_VULKAN -DHAVE_SPIRV_CROSS -DWANT_GLSLANG
endif

ifeq ($(HAVE_CHEEVOS),1)
   DEFINES += -DHAVE_CHEEVOS
endif

ifeq ($(HAVE_SAF),1)
   DEFINES += -DHAVE_SAF
endif

# -------------------------------------------------------
# COMPILER FLAGS
# -------------------------------------------------------

LOCAL_CFLAGS += -Wall -std=gnu99 -pthread -fno-stack-protector $(DEFINES)
LOCAL_CPPFLAGS += -fexceptions -fpermissive -std=gnu++11 -fno-rtti $(DEFINES)

LOCAL_CFLAGS := $(subst -O3,-O2,$(LOCAL_CFLAGS))

# -------------------------------------------------------
# LINKING
# -------------------------------------------------------

LOCAL_LDLIBS := -landroid -lEGL $(GLES_LIB) $(LOGGER_LDLIBS) -ldl -lOpenSLES -lz

# -------------------------------------------------------
# INCLUDES
# -------------------------------------------------------

LOCAL_C_INCLUDES := \
   $(LOCAL_PATH)/$(RARCH_DIR)/libretro-common/include \
   $(LOCAL_PATH)/$(RARCH_DIR)/deps

# -------------------------------------------------------
# VULKAN INCLUDES
# -------------------------------------------------------

ifeq ($(HAVE_VULKAN),1)
   LOCAL_CPPFLAGS += \
      -I$(LOCAL_PATH)/$(DEPS_DIR)/glslang \
      -I$(LOCAL_PATH)/$(DEPS_DIR)/SPIRV-Cross
endif

# -------------------------------------------------------
# SANITIZER
# -------------------------------------------------------

ifneq ($(SANITIZER),)
   LOCAL_CFLAGS   += -g -fsanitize=$(SANITIZER)
   LOCAL_CPPFLAGS += -g -fsanitize=$(SANITIZER)
   LOCAL_LDFLAGS  += -fsanitize=$(SANITIZER)
endif

include $(BUILD_SHARED_LIBRARY)
