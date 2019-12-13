package com.iflytek.ivw;


import android.util.Log;

public class IVWEngine {
    private static final String TAG = "IVWEngine";

    static {
        System.loadLibrary("hlw-jni");
        System.loadLibrary("hlw");
    }

    private static IVWEngine instance;

    public interface IVWListener {
        void onWakeup(short angle, short channel, float power, short CMScore, short beam,
                      String param1, String param2);
    }

    private IVWListener mIVWListener;

    private IVWEngine(String ivwResPath, IVWListener listener) {
        mIVWListener = listener;

        int ret = create_ivw(ivwResPath);
        if (ret != 0) {
            Log.e(TAG, "create_ivw, ret=" + ret);
        }
    }

    public static IVWEngine createInstance(String ivwResPath, IVWListener listener) {
        if (instance == null) {
            instance = new IVWEngine(ivwResPath, listener);
        }
        return instance;
    }

    private void ivwCb(short angle, short channel, float power, short CMScore, short beam,
                       String param1, String param2) {
        if (mIVWListener != null) {
            mIVWListener.onWakeup(angle, channel, power, CMScore, beam, param1, param2);
        }
    }

    public int auth(String sn) {
        return ivw_auth(sn);
    }

    public int writeAudio(byte[] audio, int len) {
        return write_audio(audio, len);
    }

    public void setLogLevel(int level) {
        set_log_level(level);
    }

    public String getVersion() {
        return get_version();
    }

    public void destroy() {
        destroy_ivw();
        instance = null;
    }

    private native int create_ivw(String ivwResPath);

    private native int ivw_auth(String sn);

    private native int write_audio(byte[] audio, int len);

    private native String get_version();

    private native void set_log_level(int level);

    private native int destroy_ivw();
}
