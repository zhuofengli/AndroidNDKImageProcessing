#include <jni.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <setjmp.h>
#include <math.h>

#include "jpeglib.h"
#include "jerror.h"

#include "png.h"

#define max(x,y) (x>y?x:y)

//#define PNG_BYTES_TO_CHECK 4
//int check_if_png(char *file_name, FILE **fp)
//{
//    char buf[PNG_BYTES_TO_CHECK];
//    
//    /* Open the prospective PNG file. */
//    if ((*fp = fopen(file_name, "rb")) == NULL)
//        return 0;
//    
//    /* Read in some of the signature bytes */
//    if (fread(buf, 1, PNG_BYTES_TO_CHECK, *fp) != PNG_BYTES_TO_CHECK)
//        return 0;
//    
//    /* Compare the first PNG_BYTES_TO_CHECK bytes of the signature.
//     Return nonzero (true) if they match */
//    
//    return(!png_sig_cmp(buf, (png_size_t)0, PNG_BYTES_TO_CHECK));
//}

JNIEXPORT jint JNICALL Java_com_zhuofengli_blackwhitephotos_NativeLib_processAndSaveFile
(JNIEnv * env, jobject obj, jstring srcPath, jint srcType, jint srcW, jint srcH, jint orientation, jint capSize, jstring dstPath, jint quality, jint thumbCapSize, jstring thumbPath){

    int ret = 1;
    int w = srcW;
    int h = srcH;
    int size = w*h;

    char *pathString = (char*)(*env)->GetStringUTFChars(env,srcPath, NULL);

    int *pix = (int*)malloc(sizeof(int)*size);
    if(pix == NULL)
    {
        goto exit;
    }

    //decode
    if(srcType == 0){
        ret = read_JPEG_file(pathString, pix, w, h);
    }
    else if(srcType == 1){
        ret = read_PNG_file(pathString, pix);
    }
    
    if(ret == 0)
    {
        goto exit;
    }

    (*env)->ReleaseStringUTFChars(env, srcPath, pathString);
    pathString = NULL;



    //resize
    int *finalPix;
    int targetW, targetH;
    if(capSize >= max(srcW, srcH))
    {
        finalPix = pix;
        targetW = srcW;
        targetH = srcH;
        pix = NULL;
    }
    else
    {
        if(srcW < srcH)
        {
            targetH = capSize;
            targetW = srcW*capSize/srcH;
        }
        else
        {
            targetW = capSize;
            targetH = srcH*capSize/srcW;
        }

        size = targetW*targetH;
        finalPix = (int*)malloc(sizeof(int)*size);
        if(finalPix == NULL)
        {
            goto exit;
        }

        ret = resize(pix, srcW, srcH, finalPix, targetW, targetH, TRUE);
        
        free(pix);
        pix = NULL;

        if(ret == 0)
        {
            goto exit;
        }
    }
    
    // rotate
    
    ret = rotate(finalPix, &targetW, &targetH, orientation);
    if(ret == 0)
    {
        goto exit;
    }
    
    //apply effect
    
    blackWhiteFilter(finalPix, targetW, targetH);
    
    
    //save to dest
    
    char *destPathString = (char*)(*env)->GetStringUTFChars(env,dstPath, NULL);
    ret = generateJPEG(finalPix, targetW, targetH, quality, destPathString);
    (*env)->ReleaseStringUTFChars(env, dstPath, destPathString);
    destPathString = NULL;
    if(ret == 0)
    {
        goto exit;
    }
    
    
    //create thumbnail
    
    ret = createThumbFile(env, finalPix, targetW, targetH, thumbCapSize, thumbPath);
    
    
    //cleanup and return
    
exit:
    if(pathString != NULL)
    {
        (*env)->ReleaseStringUTFChars(env, srcPath, pathString);
        pathString = NULL;
    }
    
    if(pix != NULL)
    {
        free(pix);
        pix = NULL;
    }
    
    if (finalPix != NULL) {
        free(finalPix);
        finalPix = NULL;
    }
    
    
    return ret;
    
}

int rotate(int pix[], int *targetW, int *targetH, int orientation){
    int ret = 1;
    
    switch(orientation)
    {
        case 1:
        {
            ret = rotate90(pix, targetW, targetH);
            break;
        }
            
        case 2:
        {
            ret = rotate180(pix, targetW, targetH);
            break;
        }
            
        case 3:
        {
            ret = rotate270(pix, targetW, targetH);
            break;
        }
            
        default:
            break;
    }
    
    return ret;
}

int rotate90(int pix[], int *targetW, int *targetH)
{
    int w = *targetW;
    int h = *targetH;
    int size = sizeof(int)*w*h;
    int *newpix = (int*)malloc(size);
    if(newpix == NULL)
    {
        return 0;
    }
    
    memcpy(newpix, pix, size);
    
    int i, j;
    for(i = 0; i < w; i++)
    {
        for(j = 0; j < h; j++)
        {
            pix[i*h+j] = newpix[(h-1-j)*w+i];
        }
    }
    *targetW = h;
    *targetH = w;
    free(newpix);
    
    return 1;
}

int rotate180(int pix[], int *targetW, int *targetH)
{
    int w = *targetW;
    int h = *targetH;
    int size = w*h;
    
    int i = 0;
    int tmp;
    for(i = 0; i < size/2; i++)
    {
        tmp = pix[i];
        pix[i] = pix[size - 1 - i];
        pix[size - 1 - i] = tmp;
    }
    
    return 1;
}

int rotate270(int pix[], int *targetW, int *targetH)
{
    int w = *targetW;
    int h = *targetH;
    int size = sizeof(int)*w*h;
    int *newpix = (int*)malloc(size);
    if(newpix == NULL)
    {
        return 0;
    }
    
    memcpy(newpix, pix, size);
    
    int i, j;
    for(i = 0; i < w; i++)
    {
        for(j = 0; j < h; j++)
        {
            pix[i*h+j] = newpix[j*w+w-1-i];
        }
    }
    
    *targetW = h;
    *targetH = w;
    free(newpix);
    
    return 1;
}

int createThumbFile(JNIEnv * env, int pix[], int srcW, int srcH, int capSize, jstring thumbPath){
    
    int ret = 1;
    
    int *finalPix;
    int targetW, targetH;
    
    if(capSize >= max(srcW, srcH))
    {
        finalPix = pix;
        targetW = srcW;
        targetH = srcH;
    }
    else
    {
        
        if(srcW < srcH)
        {
            targetH = capSize;
            targetW = srcW*capSize/srcH;
        }
        else
        {
            targetW = capSize;
            targetH = srcH*capSize/srcW;
        }
        
        int numPix = targetW*targetH;
        finalPix = (int*)malloc(sizeof(int)*numPix);
        if(finalPix == NULL)
        {
            return 0;
        }
        
        ret = resize(pix, srcW, srcH, finalPix, targetW, targetH, TRUE);

    }
    
    char *destPathString = (char*)(*env)->GetStringUTFChars(env,thumbPath, NULL);
    ret = generateJPEG(finalPix, targetW, targetH, 90, destPathString);
    (*env)->ReleaseStringUTFChars(env, thumbPath, destPathString);
    destPathString = NULL;
    
    if(finalPix != NULL && finalPix != pix){
        free(finalPix);
        finalPix = NULL;
    }
    return ret;

}

struct my_error_mgr {
    struct jpeg_error_mgr pub;	/* "public" fields */
    
    jmp_buf setjmp_buffer;	/* for return to caller */
};

typedef struct my_error_mgr * my_error_ptr;

/*
 * Here's the routine that will replace the standard error_exit method:
 */

METHODDEF(void)
my_error_exit (j_common_ptr cinfo)
{
    /* cinfo->err really points to a my_error_mgr struct, so coerce pointer */
    my_error_ptr myerr = (my_error_ptr) cinfo->err;
    
    /* Always display the message. */
    /* We could postpone this until after returning, if we chose. */
    (*cinfo->err->output_message) (cinfo);
    
    /* Return control to the setjmp point */
    longjmp(myerr->setjmp_buffer, 1);
}

int read_JPEG_file (char * filename, int *fill, int w, int h)
{
    FILE * infile;
    if ((infile = fopen(filename, "rb")) == NULL) {
        fprintf(stderr, "can't open %s\n", filename);
        return 2;
    }
    
    struct jpeg_decompress_struct cinfo;
    
    struct my_error_mgr jerr;
    cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = my_error_exit;
    
    if (setjmp(jerr.setjmp_buffer)) {
        jpeg_destroy_decompress(&cinfo);
        fclose(infile);
        return 0;
    }
    
    jpeg_create_decompress(&cinfo);
    
    jpeg_stdio_src(&cinfo, infile);
    
    (void) jpeg_read_header(&cinfo, TRUE);
    
    (void) jpeg_start_decompress(&cinfo);
    
    int row_stride = cinfo.output_width * cinfo.output_components;
    JSAMPARRAY buffer = (*cinfo.mem->alloc_sarray)
    ((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);
    
    while (cinfo.output_scanline < cinfo.output_height) {
        (void) jpeg_read_scanlines(&cinfo, buffer, 1);
        
        int i = 0;
        int j = (cinfo.output_scanline-1)*w;
        for(i = 0; i < cinfo.output_width; i++)
        {
            fill[j+i] = (int)(0xff000000 | (buffer[0][i*3]<<16) | (buffer[0][i*3+1]<<8) | (buffer[0][i*3+2]) );
        }
        
    }
    
    (void) jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);
    fclose(infile);
    return 1;
}


int generateJPEG(int pix[],int w, int h, int quanlity, const char* outfilename)
{
    int nComponent = 3;
    
    struct jpeg_compress_struct jcs;
    
    struct jpeg_error_mgr jem;
    
    jcs.err = jpeg_std_error(&jem);
    jpeg_create_compress(&jcs);
    
    FILE* f=fopen(outfilename,"wb");
    if (f==NULL)
    {
        return 2;
    }
    
    jpeg_stdio_dest(&jcs, f);
    jcs.image_width = w;
    jcs.image_height = h;
    jcs.input_components = nComponent;
    if (nComponent==1)
        jcs.in_color_space = JCS_GRAYSCALE;
    else
        jcs.in_color_space = JCS_RGB;
    
    jpeg_set_defaults(&jcs);
    jpeg_set_quality (&jcs, quanlity, TRUE);
    
    jpeg_start_compress(&jcs, TRUE);
    
    int row_stride;
    
    row_stride = jcs.image_width*nComponent;
    JSAMPARRAY buffer = (*jcs.mem->alloc_sarray)
    ((j_common_ptr) &jcs, JPOOL_IMAGE, row_stride, 1);
    
    while (jcs.next_scanline < jcs.image_height) {
        int i = 0;
        int j = (jcs.next_scanline)*w;
        for(i = 0; i < w; i++)
        {
            buffer[0][i*3] = (pix[j+i]>>16)&0xff;
            buffer[0][i*3+1] = (pix[j+i]>>8)&0xff;
            buffer[0][i*3+2] = pix[j+i]&0xff;
        }
        
        jpeg_write_scanlines(&jcs, buffer, 1);
    }
    jpeg_finish_compress(&jcs);
    jpeg_destroy_compress(&jcs);
    fclose(f);
    
    return 1;
}


int read_PNG_file(char* file_name, int *pix)
{
    int x,y;
    
    int width, height;
    png_byte color_type;
    png_byte bit_depth;
    
    png_structp png_ptr;
    png_infop info_ptr;
    int number_of_passes;
    png_bytep * row_pointers;
    
    char header[8];    // 8 is the maximum size that can be checked
    
    /* open file and test for it being a png */
    FILE *fp = fopen(file_name, "rb");
    if (!fp)
        return 0;   //File could not be opened for reading
    fread(header, 1, 8, fp);
    if (png_sig_cmp(header, 0, 8))
        return 0;   //File is not recognized as a PNG file
    
    
    /* initialize stuff */
    png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    
    if (!png_ptr)
        return 0;   //png_create_read_struct failed
    
    info_ptr = png_create_info_struct(png_ptr);
    if (!info_ptr)
        return 0;   //png_create_info_struct failed
    
    if (setjmp(png_jmpbuf(png_ptr)))
        return 0;   //Error during init_io
    
    png_init_io(png_ptr, fp);
    png_set_sig_bytes(png_ptr, 8);
    
    png_read_info(png_ptr, info_ptr);
    
    width = png_get_image_width(png_ptr, info_ptr);
    height = png_get_image_height(png_ptr, info_ptr);
    color_type = png_get_color_type(png_ptr, info_ptr);
    bit_depth = png_get_bit_depth(png_ptr, info_ptr);
    
    number_of_passes = png_set_interlace_handling(png_ptr);
    png_read_update_info(png_ptr, info_ptr);
    
    
    /* read file */
    if (setjmp(png_jmpbuf(png_ptr)))
        return 0;   //Error during read_image
    
    row_pointers = (png_bytep*) malloc(sizeof(png_bytep) * height);
    for (y=0; y<height; y++)
        row_pointers[y] = (png_byte*) malloc(png_get_rowbytes(png_ptr,info_ptr));
    
    png_read_image(png_ptr, row_pointers);
    
    fclose(fp);
    
    //read row_pointers into pixel array
    if(color_type == PNG_COLOR_TYPE_RGB){
        for(y=0;y<height;y++){
            png_byte* row = row_pointers[y];
            
            for(x=0;x<width;x++){
                png_byte* ptr = &(row[x*3]);
                pix[width*y + x] = (ptr[0] << 16) | (ptr[1] << 8) | ptr[2];
            }
        }
    }
    else if(color_type == PNG_COLOR_TYPE_RGB_ALPHA){
        for(y=0;y<height;y++){
            png_byte* row = row_pointers[y];
            
            for(x=0;x<width;x++){
                png_byte* ptr = &(row[x*4]);
                pix[width*y + x] = (ptr[3] << 24) | (ptr[0] << 16) | (ptr[1] << 8) | ptr[2];
            }
        }
    }
    
    /* cleanup heap allocation */
    for (y=0; y<height; y++)
        free(row_pointers[y]);
    free(row_pointers);
    
    return 1;

}

void blackWhiteFilter(int pix[], int w, int h){
    int pr,pg,pb,average,i;
    
    for(i=0; i<w*h; i++){
        pr = (pix[i]>>16)&0xff;
        pg = (pix[i]>>8)&0xff;
        pb = pix[i]&0xff;
        
        average = (pr+pg+pb) / 3;
        
        pix[i] = (pix[i] & 0xff000000) | (average << 16) | (average << 8) | average;
    }

}


int mix(int c1, int c2, int c3, int c4)
{
    
    int c_RB = (
                ((c1 & 0x00FF00FF) + (c2 & 0x00FF00FF) + (c3 & 0x00FF00FF) + (c4 & 0x00FF00FF)) >> 2
                ) & 0x00FF00FF;
    
    int c_AG = (
                ((unsigned int)(c1 & 0xFF00FF00) >> 2) + ((unsigned int)(c2 & 0xFF00FF00) >> 2) +
                ((unsigned int)(c3 & 0xFF00FF00) >> 2) + ((unsigned int)(c4 & 0xFF00FF00) >> 2)
                ) & 0xFF00FF00;
    return c_RB | c_AG;
}

// reduce image dimension to half
void halve(int *pix, int w1, int h1, int *out, int w2, int h2)
{
    int *buffer;
    int offset, i, j;
    for(offset = 0, i = 0; i < h2; i++) {
        buffer = pix+i*2*w1;
        
        int o1 = 0, o2 = 1;
        int o3 = w1, o4 = w1 + 1;
        
        for(j = 0; j < w2; j++) {
            out[offset ++] = mix( buffer[o1], buffer[o2], buffer[o3], buffer[o4]);
            o1 += 2;
            o2 += 2;
            o3 += 2;
            o4 += 2;
        }
    }
}

int blend(int c1, int c2, int value256)
{
    
    int v1 = value256 & 0xFF;
    int v2 = 255 - v1;
    
    
    // FAST VERSION
    int c1_RB = c1 & 0x00FF00FF;
    int c2_RB = c2 & 0x00FF00FF;
    int c1_AG = ((unsigned int)c1 >> 8) & 0x00FF00FF;
    int c2_AG = ((unsigned int)c2 >> 8) & 0x00FF00FF;
    
    return
    (((c1_RB * v1 + c2_RB * v2) >> 8) & 0x00FF00FF) |
    ((c1_AG * v1 + c2_AG * v2) & 0xFF00FF00);
}

void resize_rgb_filtered(int src[], int dst[], int w0, int h0, int w1, int h1)
{
    
    int *buffer1;
    int *buffer2;
    
    // UNOPTIMIZED bilinear filtering:
    //
    // The pixel position is defined by y_a and y_b,
    // which are 24.8 fixed point numbers
    //
    // for bilinear interpolation, we use y_a1 <= y_a <= y_b1
    // and x_a1 <= x_a <= x_b1, with y_d and x_d defining how long
    // from x/y_b1 we are.
    //
    // since we are resizing one line at a time, we will at most
    // need two lines from the source image (y_a1 and y_b1).
    // this will save us some memory but will make the algorithm
    // noticeably slower
    int index1, x, y;
    for(index1 = 0, y = 0; y < h1; y++) {
        
        int y_a = ((y * h0) << 8) / h1;
        
        int y_a1 = y_a >> 8;
        int y_d = y_a & 0xFF;
        
        int y_b1 = y_a1 + 1;
        if(y_b1 >= h0) {
            y_b1 = h0-1;
            y_d = 0;
        }
        
        // get the two affected lines:
        //src_i.getPixels(buffer1, 0, w0, 0, y_a1, w0, 1);
        buffer1 = src+y_a1*w0;
        if(y_d != 0)
        {
            //src_i.getPixels(buffer2, 0, w0, 0, y_b1, w0, 1);
            buffer2 = src+y_b1*w0;
        }
        
        
        for(x = 0; x < w1; x++) {
            // get this and the next point
            int x_a = ((x * w0) << 8) / w1;
            int x_a1 = x_a >> 8;
            int x_d = x_a & 0xFF;
            
            
            int x_b1 = x_a1 + 1;
            if(x_b1 >= w0) {
                x_b1 = w0-1;
                x_d = 0;
            }
            
            
            // interpolate in x
            int c12, c34;
            int c1 = buffer1[x_a1];
            int c3 = buffer1[x_b1];
            
            // interpolate in y:
            if(y_d == 0) {
                c12 = c1;
                c34 = c3;
            } else {
                int c2 = buffer2[x_a1];
                int c4 = buffer2[x_b1];
                
                c12 = blend(c2, c1, y_d);
                c34 = blend(c4, c3, y_d);
            }
            
            // final result
            dst[index1++] = blend(c34, c12, x_d);
        }
    }
    
}

// load and resize image:
int resize(int *src_i, int src_w, int src_h, int *dst_i, int size_w, int size_h, boolean mipmap)
{
    int *pixel = src_i;
    int w = src_w;
    int h = src_h;
    
    // scale only after mip-mapping?
    if(mipmap) {
        while(w > size_w *2 && h > size_h * 2) {
            int targetW = w / 2;
            int targetH = h / 2;
            
            int size = sizeof(int)*targetW*targetH;
            int *out = (int *)malloc(size);
            if(out == NULL)
            {
                return 0;
            }
            
            halve(pixel, w, h, out, targetW, targetH);
            memcpy(pixel, out, size);
            free(out);
            out = NULL;
            
            w = targetW;
            h = targetH;
        }
    }
    
    resize_rgb_filtered(pixel, dst_i, w, h, size_w, size_h);
    
    
    return 1;
}