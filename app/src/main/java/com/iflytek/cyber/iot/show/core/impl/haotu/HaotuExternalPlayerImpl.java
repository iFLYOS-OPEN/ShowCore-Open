package com.iflytek.cyber.iot.show.core.impl.haotu;

import android.content.Context;
import android.content.Intent;

import com.iflytek.cyber.evs.sdk.agent.ExternalPlayer;
import com.iflytek.cyber.iot.show.core.HaotuVideoActivity;
import com.iflytek.cyber.iot.show.core.model.DemoConstant;

import java.lang.ref.SoftReference;


public class HaotuExternalPlayerImpl extends ExternalPlayer {
    private static HaotuExternalPlayerImpl mInstance;
    private SoftReference<Context> mContextRef;

    public static HaotuExternalPlayerImpl getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        } else {
            HaotuExternalPlayerImpl newInstance = new HaotuExternalPlayerImpl(context);
            mInstance = newInstance;
            return newInstance;
        }
    }

    public HaotuCallback haotuCallback;

    private HaotuExternalPlayerImpl(Context context) {
        setPlayerType(ExternalPlayer.TYPE_VIDEO);
        setSourceId("haotuplayer-001");

        mContextRef = new SoftReference<>(context);
    }

    @Override
    public void play(String list) {
        super.play(list);

        if (mContextRef.get() == null) return;

        if (!isActive()) {
            Intent intent = new Intent();
            intent.setClass(mContextRef.get(), HaotuVideoActivity.class);
            intent.putExtra(DemoConstant.KEY_MEDIA_LIST, list);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContextRef.get().startActivity(intent);
        }

        if (haotuCallback != null) {
            haotuCallback.requestPlay(list);
        }
    }

    @Override
    public void pause() {
        super.pause();

        if (mContextRef.get() == null) return;
        mContextRef.get().sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_PAUSE));
    }

    @Override
    public void resume() {
        super.resume();

        if (mContextRef.get() == null) return;
        mContextRef.get().sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_RESUME));
    }

    @Override
    public void previous() {
        super.previous();

        if (mContextRef.get() == null) return;
        mContextRef.get().sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_PREVIOUS));
    }

    @Override
    public void next() {
        super.next();

        if (mContextRef.get() == null) return;
        mContextRef.get().sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_NEXT));
    }

    @Override
    public void volumeUp() {
        if (mContextRef.get() == null) return;
        mContextRef.get().sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_VOL_UP));
    }

    @Override
    public void volumeDown() {
        if (mContextRef.get() == null) return;
        mContextRef.get().sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_VOL_DOWN));
    }

    @Override
    public void fastForward(int offset) {
        if (mContextRef.get() == null) return;
        Intent intent = new Intent(DemoConstant.ACTION_HAOTU_FAST_FORWARD);
        intent.putExtra("offset", offset);

        mContextRef.get().sendBroadcast(intent);
    }

    @Override
    public void fastBackward(int offset) {
        if (mContextRef.get() == null) return;
        Intent intent = new Intent(DemoConstant.ACTION_HAOTU_FAST_BACKWARD);
        intent.putExtra("offset", offset);

        mContextRef.get().sendBroadcast(intent);
    }

    @Override
    public void seekTo(int offset) {
        if (mContextRef.get() == null) return;
        Intent intent = new Intent(DemoConstant.ACTION_HAOTU_SET_OFFSET);
        intent.putExtra("offset", offset);

        mContextRef.get().sendBroadcast(intent);
    }

    @Override
    public void exit() {
        super.exit();

        if (mContextRef.get() == null) return;
        Intent intent = new Intent(DemoConstant.ACTION_HAOTU_EXIT);

        mContextRef.get().sendBroadcast(intent);
    }

    @Override
    public boolean moveToBackground() {
        if (mContextRef.get() == null) return false;
        mContextRef.get().sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_MOVE_TO_BACKGROUND));
        return false;
    }

    @Override
    public boolean moveToForegroundIfAvailable() {
        if (mContextRef.get() == null) return false;
        mContextRef.get().sendBroadcast(new Intent(DemoConstant.ACTION_HAOTU_MOVE_TO_FOREGROUND));
        return false;
    }

    public interface HaotuCallback {
        void requestPlay(String list);
    }
}
