#include <jni.h>
#include <string>

#include "hlw.h"
#include "com_iflytek_ivw_IVWEngine.h"


JavaVM* g_JVM = NULL;

jobject g_thiz = NULL;
jmethodID g_ivwCb_method = NULL;

CAE_HANDLE g_handle = NULL;


void ivw_cb_func(short angle, short channel, float power, short CMScore, short beam,
        char *param1, void *param2, void *userData)
{
    if (g_JVM == NULL) {
        return;
    }

    JNIEnv* pEnv = NULL;
    int ret = g_JVM->AttachCurrentThread(&pEnv, NULL);
    if (ret != JNI_OK) {
        return;
    }

    if (g_thiz != NULL) {
        jstring param1Str = pEnv->NewStringUTF(param1);

        pEnv->CallVoidMethod(g_thiz, g_ivwCb_method,
                angle, channel, power, CMScore, beam, param1Str, NULL);
    }

    g_JVM->DetachCurrentThread();
}

void audio_cb_func(const void *audioData, unsigned int audioLen,
        int param1, const void *param2, void *userData)
{

}

void ivw_audio_cb_func(const void *audioData, unsigned int audioLen,
        int param1, const void *param2, void *userData)
{

}


/*
 * Class:     com_iflytek_ivw_IVWEngine
 * Method:    create_ivw
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_ivw_IVWEngine_create_1ivw
        (JNIEnv *pEnv, jobject thiz, jstring ivwResPath)
{
    int ret = pEnv->GetJavaVM(&g_JVM);
    if (ret != JNI_OK) {
        return -1;
    }

    g_thiz = pEnv->NewGlobalRef(thiz);
    g_ivwCb_method = pEnv->GetMethodID(pEnv->GetObjectClass(thiz), "ivwCb",
                      "(SSFSSLjava/lang/String;Ljava/lang/String;)V");
    const char* pIvwResPathChar = pEnv->GetStringUTFChars(ivwResPath, JNI_FALSE);

    ret = CAENew(&g_handle, pIvwResPathChar, ivw_cb_func, ivw_audio_cb_func, audio_cb_func,
            NULL, NULL);

    pEnv->ReleaseStringUTFChars(ivwResPath, pIvwResPathChar);

    return ret;
}

/*
 * Class:     com_iflytek_ivw_IVWEngine
 * Method:    ivw_auth
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_ivw_IVWEngine_ivw_1auth
        (JNIEnv *pEnv, jobject thiz, jstring sn)
{
    int ret = -1;
    if (g_handle != NULL) {
        const char *pSnChar = pEnv->GetStringUTFChars(sn, JNI_FALSE);
        ret = CAEAuth((char*) pSnChar);
        pEnv->ReleaseStringUTFChars(sn, pSnChar);
    }

    return ret;
}

/*
 * Class:     com_iflytek_ivw_IVWEngine
 * Method:    write_audio
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_ivw_IVWEngine_write_1audio
        (JNIEnv *pEnv, jobject thiz, jbyteArray audio, jint len)
{
    int ret = -1;

    if (g_handle != NULL) {
        char* pAudioChar = (char*) pEnv->GetByteArrayElements(audio, JNI_FALSE);

        ret = CAEAudioWrite(g_handle, pAudioChar, len);

        pEnv->ReleaseByteArrayElements(audio, (jbyte*) pAudioChar, 0);
    }

    return ret;
}

/*
 * Class:     com_iflytek_ivw_IVWEngine
 * Method:    get_version
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_iflytek_ivw_IVWEngine_get_1version
        (JNIEnv *pEnv, jobject thiz)
{
    char* pVer = CAEGetVersion();
    return pEnv->NewStringUTF(pVer);
}

/*
 * Class:     com_iflytek_ivw_IVWEngine
 * Method:    set_log_level
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_iflytek_ivw_IVWEngine_set_1log_1level
        (JNIEnv *pEnv, jobject thiz, jint level)
{
    CAESetShowLog(level);
}

/*
 * Class:     com_iflytek_ivw_IVWEngine
 * Method:    destroy_ivw
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_iflytek_ivw_IVWEngine_destroy_1ivw
        (JNIEnv *pEnv, jobject thiz)
{
    int ret = -1;
    if (g_handle != NULL) {
        ret = CAEDestroy(g_handle);
        g_handle = NULL;
    }

    if (g_thiz != NULL) {
        pEnv->DeleteGlobalRef(g_thiz);
        g_thiz = NULL;
    }

    g_ivwCb_method = NULL;

    return ret;
}
