#include <jni.h>
#include <stdint.h>
#include "lame.h"
JNIEXPORT jlong JNICALL Java_com_pocketaudio_app_Mp3_init(JNIEnv *e,jclass c,jint r,jint ch,jint b) {
 lame_t g=lame_init(); if(!g)return 0;
 lame_set_in_samplerate(g,r); lame_set_num_channels(g,ch); lame_set_brate(g,b);
 lame_set_out_samplerate(g,44100); lame_set_quality(g,3); lame_set_bWriteVbrTag(g,0);
 if(lame_init_params(g)<0){lame_close(g);return 0;} return (jlong)(intptr_t)g;
}
JNIEXPORT jint JNICALL Java_com_pocketaudio_app_Mp3_encodeNative(JNIEnv *e,jclass c,jlong h,jshortArray pcm,jint frames,jbyteArray out) {
 lame_t g=(lame_t)(intptr_t)h;
 if(!g || frames<0 || (*e)->GetArrayLength(e,pcm)<frames*lame_get_num_channels(g))return -1;
 jshort *p=(*e)->GetShortArrayElements(e,pcm,0); if(!p)return -1;
 jbyte *b=(*e)->GetByteArrayElements(e,out,0); if(!b){(*e)->ReleaseShortArrayElements(e,pcm,p,JNI_ABORT);return -1;}
 int n=lame_get_num_channels(g)==1 ? lame_encode_buffer(g,p,p,frames,(unsigned char*)b,(*e)->GetArrayLength(e,out)) : lame_encode_buffer_interleaved(g,p,frames,(unsigned char*)b,(*e)->GetArrayLength(e,out));
 (*e)->ReleaseShortArrayElements(e,pcm,p,JNI_ABORT); (*e)->ReleaseByteArrayElements(e,out,b,0); return n;
}
JNIEXPORT jint JNICALL Java_com_pocketaudio_app_Mp3_flush(JNIEnv *e,jclass c,jlong h,jbyteArray out) {
 jbyte *b=(*e)->GetByteArrayElements(e,out,0);if(!b)return -1;
 int n=lame_encode_flush((lame_t)(intptr_t)h,(unsigned char*)b,(*e)->GetArrayLength(e,out));
 (*e)->ReleaseByteArrayElements(e,out,b,0);return n;
}
JNIEXPORT void JNICALL Java_com_pocketaudio_app_Mp3_destroy(JNIEnv *e,jclass c,jlong h){lame_close((lame_t)(intptr_t)h);}
