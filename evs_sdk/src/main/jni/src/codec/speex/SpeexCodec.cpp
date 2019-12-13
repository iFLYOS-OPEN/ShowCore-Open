//
// Created by huang on 2019/9/23.
//

#include "../../../com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec.h"
#include "src/arch.h"

#include <speex/speex.h>
#include <android/log.h>
#include <stdlib.h>

#undef TAG
#define TAG "SpeexCodec_Jni"

#define METHOD_SET_HANDLES "setHandles"


// nb模式下，各quality下每帧（640bytes，320个short）压缩后字节数
int enc_nb_frame_nbytes_of_quality[11] = {6, 10, 15, 20, 20, 28, 28, 38, 38, 46, 62};

// wb模式下，各quality下每帧（640bytes，320个short）压缩后字节数
int enc_wb_frame_nbytes_of_quality[11] = {10, 15, 20, 25, 32, 42, 52, 60, 70, 86, 106};

void* getHandle(JNIEnv *pEnv, jbyteArray handle)
{
    char* handleChar = (char*) pEnv->GetByteArrayElements(handle, JNI_FALSE);
    void* st = NULL;
    if (sizeof(st) == 8) {
        st = (SpeexBits*) (*((unsigned long long*) handleChar));
    } else {
        st = (SpeexBits*) (*((unsigned long*) handleChar));
    }

    pEnv->ReleaseByteArrayElements(handle, (jbyte*) handleChar, JNI_FALSE);

    return st;
}

/*
 * Class:     com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec
 * Method:    init_native
 * Signature: (IIII)I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec_init_1native
        (JNIEnv *pEnv, jobject thiz, jint codec, jint sampleRate, jint mode, jint quality)
{
    jmethodID setHandlesMethod = pEnv->GetMethodID(pEnv->GetObjectClass(thiz), METHOD_SET_HANDLES, "([B[B)V");
    if (NULL == setHandlesMethod) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "cannot find method %s with signature %s in Java class",
                            METHOD_SET_HANDLES, "(JJ)V");
        return -1;
    }

    void* state = NULL;
    SpeexBits* bits = new SpeexBits;
    int tmp = quality;

    if (com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec_CODEC_ENCODE == codec) {
        if (com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec_MODE_NB == mode) {
            state = speex_encoder_init(&speex_nb_mode);
        } else {
            state = speex_encoder_init(&speex_wb_mode);
        }

        if (NULL == state) {
            if (NULL != bits) {
                delete bits;
            }
            return -1;
        }

        speex_encoder_ctl(state, SPEEX_SET_QUALITY, &tmp);
    } else {
        if (com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec_MODE_NB == mode) {
            state = speex_decoder_init(&speex_nb_mode);
        } else {
            state = speex_decoder_init(&speex_wb_mode);
        }

        if (NULL == state) {
            if (NULL != bits) {
                delete bits;
            }
            return -1;
        }

        tmp = 1;
        speex_decoder_ctl(state, SPEEX_SET_ENH, &tmp);
    }

    speex_bits_init(bits);

    int len = sizeof(state);
    jbyteArray stateArray = pEnv->NewByteArray(len);
    jbyteArray bitsArray = pEnv->NewByteArray(len);

    pEnv->SetByteArrayRegion(stateArray, 0, len, (jbyte*) &state);
    pEnv->SetByteArrayRegion(bitsArray, 0, len, (jbyte*) &bits);

    pEnv->CallVoidMethod(thiz, setHandlesMethod, stateArray, bitsArray);

    return 0;
}

/*
 * Class:     com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec
 * Method:    encode_native
 * Signature: ([BI[BI)I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec_encode_1native
        (JNIEnv *pEnv, jobject thiz, jbyteArray state, jbyteArray speex_bits, jbyteArray data, jint dataLen,
                jbyteArray buffer, jint bufferLen)
{
    void* ptr_state = (void*) getHandle(pEnv, state);
    SpeexBits* ptr_bits = (SpeexBits*) getHandle(pEnv, speex_bits);

    int frameSize = 0;
    speex_encoder_ctl(ptr_state, SPEEX_GET_FRAME_SIZE, &frameSize);

    int shortDataLen = dataLen / 2;
    if (shortDataLen % frameSize != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "dataLen must be multiple of %d", frameSize);
        return -1;
    }

    char* dataChars = (char*) pEnv->GetByteArrayElements(data, JNI_FALSE);
    spx_int16_t* dataInt16 = (spx_int16_t*) dataChars;
    char* tmpBuffer = new char[bufferLen];

    int writeLen = 0;
    for (int i = 0; i < shortDataLen; i += frameSize) {
        speex_bits_reset(ptr_bits);
        speex_encode_int(ptr_state, &dataInt16[i], ptr_bits);

        writeLen += speex_bits_write(ptr_bits, &tmpBuffer[writeLen], bufferLen);
    }

    delete[] tmpBuffer;

    pEnv->ReleaseByteArrayElements(data, (jbyte*) dataChars, JNI_FALSE);
    pEnv->SetByteArrayRegion(buffer, 0, writeLen, (jbyte*) tmpBuffer);

    return writeLen;
}

/*
 * Class:     com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec
 * Method:    decode_native
 * Signature: (II[BI[BIII)I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec_decode_1native
        (JNIEnv *pEnv, jobject thiz, jbyteArray state, jbyteArray speex_bits, jbyteArray data, jint dataLen,
         jbyteArray buffer, jint bufferLen, jint mode, jint quality)
{
    void* ptr_state = (void*) getHandle(pEnv, state);
    SpeexBits* ptr_bits = (SpeexBits*) getHandle(pEnv, speex_bits);

    int encFrameBytes = 0;
    if (com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec_MODE_NB == mode) {
        encFrameBytes = enc_nb_frame_nbytes_of_quality[quality];
    } else {
        encFrameBytes = enc_wb_frame_nbytes_of_quality[quality];
    }

    if (dataLen % encFrameBytes != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "dataLen must be multiple of %d", encFrameBytes);
        return -1;
    }

    char* dataChars = (char*) pEnv->GetByteArrayElements(data, JNI_FALSE);
    int readBytes = 0;
    int frameSize = 0;
    int writeOffset = 0;

    speex_decoder_ctl(ptr_state, SPEEX_GET_FRAME_SIZE, &frameSize);
    short* frame = new short[frameSize];

    while (readBytes != dataLen) {
        speex_bits_read_from(ptr_bits, &dataChars[readBytes], encFrameBytes);
        readBytes += encFrameBytes;

        int ret = speex_decode_int(ptr_state, ptr_bits, frame);
        if (0 == ret) {
            if (writeOffset < bufferLen) {
                pEnv->SetByteArrayRegion(buffer, writeOffset, frameSize * 2, (jbyte *) frame);
                writeOffset += frameSize * 2;
            } else {
                __android_log_print(ANDROID_LOG_ERROR, TAG, "buffer length is not enough");
                break;
            }
        } else {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "decode, ret=%d", ret);

            pEnv->ReleaseByteArrayElements(data, (jbyte*) dataChars, JNI_FALSE);
            return ret;
        }
    }

    delete[] frame;

    pEnv->ReleaseByteArrayElements(data, (jbyte*) dataChars, JNI_FALSE);

    return writeOffset;
}

/*
 * Class:     com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec
 * Method:    destroy_native
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec_destroy_1native
        (JNIEnv *pEnv, jobject thiz, jint codec, jbyteArray state, jbyteArray bits)
{
    void* st = getHandle(pEnv, state);
    void* pBits = getHandle(pEnv, bits);

    if (com_iflytek_cyber_evs_sdk_codec_audio_speex_SpeexCodec_CODEC_ENCODE == codec) {
        speex_encoder_destroy((void*) st);
    } else {
        speex_decoder_destroy((void*) st);
    }

    speex_bits_destroy((SpeexBits*) pBits);
    delete (SpeexBits*) pBits;
}
