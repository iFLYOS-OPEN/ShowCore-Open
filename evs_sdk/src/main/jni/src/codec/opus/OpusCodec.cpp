//
// Created by huang on 2019/9/24.
//

#include "../../../com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec.h"

#include <opus.h>
#include <stdlib.h>
#include <android/log.h>

#undef TAG
#define TAG "OpusCodec_Jni"

#define METHOD_SET_HANDLES "setHandles"
#define CHANNEL_NUM 1

void* getHandle(JNIEnv *pEnv, jbyteArray handle)
{
    char* handleChar = (char*) pEnv->GetByteArrayElements(handle, JNI_FALSE);
    void* st = NULL;
    if (sizeof(st) == 8) {
        st = (OpusEncoder*) (*((unsigned long long*) handleChar));
    } else {
        st = (OpusEncoder*) (*((unsigned long*) handleChar));
    }

    pEnv->ReleaseByteArrayElements(handle, (jbyte*) handleChar, JNI_FALSE);

    return st;
}

/*
 * Class:     com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec
 * Method:    init_native
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec_init_1native
        (JNIEnv *pEnv, jobject thiz, jint codec, jint sampleRate, jint bitRate)
{
    jmethodID setHandlesMethod = pEnv->GetMethodID(pEnv->GetObjectClass(thiz), METHOD_SET_HANDLES, "([B)V");
    if (NULL == setHandlesMethod) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "cannot find method %s with signature %s in Java class",
                            METHOD_SET_HANDLES, "(J)V");
        return -1;
    }

    void* st = NULL;
    int error = 0;

    if (com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec_CODEC_ENCODE == codec) {
        st = opus_encoder_create(sampleRate, CHANNEL_NUM, OPUS_APPLICATION_VOIP, &error);
        if (OPUS_OK != error) {
            return error;
        }

        opus_encoder_ctl((OpusEncoder*) st, OPUS_SET_VBR(0));
        opus_encoder_ctl((OpusEncoder*) st, OPUS_SET_BITRATE(bitRate));
    } else {
        st = opus_decoder_create(sampleRate, CHANNEL_NUM, &error);

        if (OPUS_OK != error) {
            return error;
        }
    }

    int len = sizeof(st);
    jbyteArray array = pEnv->NewByteArray(len);
    pEnv->SetByteArrayRegion(array, 0, len, (jbyte*) &st);
    pEnv->CallVoidMethod(thiz, setHandlesMethod, array);

    return 0;
}

/*
 * Class:     com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec
 * Method:    encode_native
 * Signature: (J[BI[BII)I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec_encode_1native
        (JNIEnv *pEnv, jobject thiz, jbyteArray handle, jbyteArray data, jint dataLen,
                jbyteArray buffer, jint bufferLen, jint packetSize)
{
    OpusEncoder* st = (OpusEncoder*) getHandle(pEnv, handle);

    if (dataLen % packetSize != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "dataLen must be multiple of %d",
                            packetSize);
        return -1;
    }

    char* dataChars = (char*) pEnv->GetByteArrayElements(data, NULL);
    unsigned char* tmpBuffer = new unsigned char[packetSize];

    int readLen = 0;
    int writeOffset = 0;

    while (readLen < dataLen) {
        int encLen = opus_encode(st, (opus_int16*) &dataChars[readLen], packetSize / 2, tmpBuffer, 4000);
        if (encLen < 0) {
            pEnv->ReleaseByteArrayElements(data, (jbyte*) dataChars, JNI_FALSE);
            return encLen;
        }

        if (writeOffset + encLen > bufferLen) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "buffer is not enough");
            pEnv->ReleaseByteArrayElements(data, (jbyte*) dataChars, JNI_FALSE);
            return -1;
        }

        pEnv->SetByteArrayRegion(buffer, writeOffset, encLen, (jbyte*) tmpBuffer);
        readLen += packetSize;
        writeOffset += encLen;
    }

    delete[] tmpBuffer;

    pEnv->ReleaseByteArrayElements(data, (jbyte*) dataChars, JNI_FALSE);
    return writeOffset;
}

/*
 * Class:     com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec
 * Method:    decode_native
 * Signature: (J[BI[BII)I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec_decode_1native
        (JNIEnv *pEnv, jobject thiz, jbyteArray handle, jbyteArray data, jint dataLen,
                jbyteArray buffer, jint bufferLen, jint packetSize, jint packetDecSize)
{
    OpusDecoder* st = (OpusDecoder*) getHandle(pEnv, handle);

    if (dataLen % packetSize != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "dataLen must be multiple of %d",
                            packetSize);
        return -1;
    }

    char* dataChars = (char*) pEnv->GetByteArrayElements(data, NULL);
    opus_int16* tmpBuffer = new opus_int16[packetDecSize / 2];

    int readLen = 0;
    int writeOffset = 0;

    while (readLen < dataLen) {
        int decLen = opus_decode(st, (const unsigned char*) &dataChars[readLen], packetSize,
                tmpBuffer, packetDecSize / 2, 0);
        if (decLen < 0) {
            pEnv->ReleaseByteArrayElements(data, (jbyte*) dataChars, JNI_FALSE);
            return decLen;
        }

        if (writeOffset + decLen > bufferLen) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "buffer is not enough");
            pEnv->ReleaseByteArrayElements(data, (jbyte*) dataChars, JNI_FALSE);
            return -1;
        }

        pEnv->SetByteArrayRegion(buffer, writeOffset, decLen * 2, (jbyte*) tmpBuffer);
        readLen += packetSize;
        writeOffset += decLen * 2;
    }

    delete[] tmpBuffer;

    pEnv->ReleaseByteArrayElements(data, (jbyte*) dataChars, JNI_FALSE);
    return writeOffset;
}

/*
 * Class:     com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec
 * Method:    destroy_native
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL Java_com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec_destroy_1native
        (JNIEnv *pEnv, jobject thiz, jint codec, jbyteArray handle)
{
    void* st = getHandle(pEnv, handle);

    if (com_iflytek_cyber_evs_sdk_codec_audio_opus_OpusCodec_CODEC_ENCODE == codec) {
        opus_encoder_destroy((OpusEncoder*) st);
    } else {
        opus_decoder_destroy((OpusDecoder*) st);
    }
}