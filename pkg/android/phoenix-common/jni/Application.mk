ifeq ($(GLES),3)
   ifndef NDK_GL_HEADER_VER
      APP_PLATFORM := android-18
   else
      APP_PLATFORM := $(NDK_GL_HEADER_VER)
   endif
else
   ifndef NDK_NO_GL_HEADER_VER
      APP_PLATFORM := android-9
   else
      APP_PLATFORM := $(NDK_NO_GL_HEADER_VER)
   endif
endif


ifndef TARGET_ABIS
   APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
else
   APP_ABI := $(TARGET_ABIS)
endif


# AddressSanitizer block
ifneq ($(SANITIZER),)
   # AddressSanitizer doesn't run on mips
   ifndef TARGET_ABIS
      APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
   else
      ifneq ($(findstring mips,$(TARGET_ABIS)),)
         $(error "AddressSanitizer does not support mips.")
      endif
   endif
   USE_CLANG := 1
endif


ifeq ($(USE_CLANG),1)
   NDK_TOOLCHAIN_VERSION := clang
   APP_CFLAGS   := -Wno-invalid-source-encoding
   APP_CPPFLAGS := -Wno-invalid-source-encoding
endif


APP_STL := c++_static
