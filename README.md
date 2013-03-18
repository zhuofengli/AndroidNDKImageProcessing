This Android project shows how to import an image from gallery and do all the image processing (decode, resize, rotate, filter, encode) in NDK.

Some things you need to know:

<h2>Image Libraries</h2>

In order to decode JPEG and PNG images, libjpeg and libpng have been compiled and imported into the native code. You can find the list of image libraries and their origins here:

http://www.leptonica.org/vs2008doc/building-image-libraries.html


But if you're not building things up from ground, I believe you can just use the jni folder in this project as it already has the required library files in place.


That also says currently this project only decodes JPEG and PNG files. I may add further support for GIF in future.

<h2>ndk_blackwhitephotos.c</h2>

All the major image processing code are in this file (jni/ndk_blackwhitephotos.c), you can tweak around it to serve your own purpose.
