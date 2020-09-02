package com.iflytek.cyber.iot.show.core.impl.haotu;

import android.content.Context;

import com.iflytek.cyber.evs.sdk.agent.VideoPlayer;

import org.jetbrains.annotations.NotNull;

public class HaotuVideoPlayerAgentImpl extends VideoPlayer {
    private static final String TAG = "HaotuVideo";

    private Context mContext;

    public HaotuVideoPlayerAgentImpl(Context context) {
        mContext = context;
    }

    @Override
    public long getOffset() {
        return 0;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public boolean play(@NotNull String resourceId, @NotNull String url) {
//        if (url.startsWith("haotu://playUrl?result=")) {
//            String base64 = url.substring("haotu://playUrl?result=".length());
//            String resultJsonStr = new String(DataUtil.INSTANCE.decodeBase64(base64));
//
//            Intent intent = new Intent();
//            intent.setClass(mContext, HaotuVideoActivity.class);
//            intent.putExtra(DemoConstant.KEY_MEDIA_INFO, resultJsonStr);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            mContext.startActivity(intent);
//        }

        return false;
    }

    @Override
    public boolean pause() {
        return false;
    }

    @Override
    public boolean resume() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean exit() {
        return false;
    }

    @Override
    public boolean moveToBackground() {
//        mContext.sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_MOVE_TO_BACKGROUND));
        return false;
    }

    @Override
    public boolean moveToForegroundIfAvailable() {
//        mContext.sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_MOVE_TO_FOREGROUND));
        return false;
    }
}
