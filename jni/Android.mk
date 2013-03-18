# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)

## prebuilt the static libs
include $(CLEAR_VARS)

LOCAL_MODULE 	:= jpeg
LOCAL_SRC_FILES := libjpeg.a

include $(PREBUILT_STATIC_LIBRARY)

## add libpng
include $(CLEAR_VARS)
LOCAL_MODULE	:= png
LOCAL_SRC_FILES := png/png.c png/pngset.c png/pngget.c png/pngrutil.c png/pngtrans.c png/pngwutil.c \
png/pngread.c png/pngrio.c png/pngwio.c png/pngwrite.c png/pngrtran.c \
png/pngwtran.c png/pngmem.c png/pngerror.c png/pngpread.c

include $(BUILD_STATIC_LIBRARY)


## to call the static libs
include $(CLEAR_VARS)

LOCAL_MODULE    := ndk_blackwhitephotos
LOCAL_SRC_FILES := ndk_blackwhitephotos.c

LOCAL_STATIC_LIBRARIES += jpeg
LOCAL_STATIC_LIBRARIES += png
LOCAL_LDLIBS += -lz

include $(BUILD_SHARED_LIBRARY)