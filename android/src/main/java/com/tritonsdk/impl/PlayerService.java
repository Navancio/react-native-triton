package com.tritonsdk.impl;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.tritondigital.player.MediaPlayer;
import com.tritondigital.player.TritonPlayer;
import com.tritonsdk.R;

import java.util.Arrays;
import java.util.List;


public class PlayerService extends Service implements TritonPlayer.OnCuePointReceivedListener, TritonPlayer.OnStateChangedListener, TritonPlayer.OnMetaDataReceivedListener, AudioManager.OnAudioFocusChangeListener {

    // Constants
    public static final String ARG_STREAM = "stream";
    public static final String ARG_ON_DEMAND_STREAM = "on_demand_stream";
    public static final String ARG_TRACK = "track";
    public static final String ARG_STATE = "state";
    public static final String DEFAULT_CHANNEL = "default";
    public static final String ACTION_INIT = "PlayerService.ACTION_INIT";
    public static final String ACTION_PLAY = "PlayerService.ACTION_PLAY";
    public static final String ACTION_STOP = "PlayerService.ACTION_STOP";
    public static final String ACTION_QUIT = "PlayerService.ACTION_QUIT";
    public static final String CUE_TYPE_TRACK = "track";
    public static final String CUE_TYPE_AD = "ad";
    public static final String EVENT_TRACK_CHANGED = "PlayerService.EVENT_TRACK_CHANGED";
    public static final String EVENT_STREAM_CHANGED = "PlayerService.EVENT_STREAM_CHANGED";
    public static final String EVENT_STATE_CHANGED = "PlayerService.EVENT_STATE_CHANGED";
    public static final int NOTIFICATION_SERVICE = 8;
    public static final boolean IS_NOTIF_ACTIVE = false;
    public static String BRAND = "slam";

    // Binder
    private final IBinder iBinder = new LocalBinder();

    // Player
    private TritonPlayer mPlayer;
    private Stream mCurrentStream;
    private OnDemandStream mCurrentOnDemandStream;
    private Track mCurrentTrack;

    // Notification
    private NotificationCompat.Builder mBuilder;
    private RemoteViews mRemoteViews;
    private NotificationManager mNotificationManager;

    private MusicIntentReceiver mReceiver = new MusicIntentReceiver();


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_INIT:
                    IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
                    registerReceiver(mReceiver, filter);
                    // nothing
                    break;
                case ACTION_PLAY:
                    if (intent.hasExtra(ARG_STREAM)) {
                        mCurrentStream = (Stream) intent.getSerializableExtra(ARG_STREAM);
                        notifyStationUpdate();
                        mCurrentTrack = null;
                        mCurrentOnDemandStream = null;
                        notifyTrackUpdate();
                    }
                    else if (intent.hasExtra(ARG_ON_DEMAND_STREAM)) {
                        mCurrentOnDemandStream = (OnDemandStream) intent.getSerializableExtra(ARG_ON_DEMAND_STREAM);
                        //notifyStationUpdate();
                        //TODO maybe fire an event here somehow?
                        mCurrentTrack = null;
                        mCurrentStream = null;
                        //notifyTrackUpdate();
                    }

                    play();
                    break;
                case ACTION_QUIT:
                    //releasePlayer();
                    quit();
                    //stopSelf();
                    break;
                case ACTION_STOP:
                    stop();
                    break;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }

        try {
            unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException ignored) {

        }
    }

    private void playMedia() {
        if (mCurrentStream == null && mCurrentOnDemandStream == null) return;

        String[] tTags = {"PLAYER:NOPREROLL"};

        Bundle settings = new Bundle();
        settings.putString(TritonPlayer.SETTINGS_STATION_BROADCASTER, "Triton Digital");

        if (mCurrentStream != null)
        {
            settings.putString(TritonPlayer.SETTINGS_STATION_NAME, mCurrentStream.getTritonName());
            settings.putString(TritonPlayer.SETTINGS_STATION_MOUNT, mCurrentStream.getTritonMount());
        }
        else if (mCurrentOnDemandStream != null) {
            settings.putString(TritonPlayer.SETTINGS_STREAM_URL, mCurrentOnDemandStream.getURL());
        }

        settings.putString(TritonPlayer.SETTINGS_PLAYER_SERVICES_REGION, "EU");
        settings.putBoolean(TritonPlayer.SETTINGS_TARGETING_LOCATION_TRACKING_ENABLED, true);
        settings.putStringArray(TritonPlayer.SETTINGS_TTAGS, tTags);
        mPlayer = new TritonPlayer(this, settings);
        mPlayer.setOnStateChangedListener(this);
        mPlayer.setOnCuePointReceivedListener(this);
        mPlayer.setOnMetaDataReceivedListener(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                AudioManager audioManager = getAudioManager();
                if (audioManager != null) {
                    int result = audioManager.requestAudioFocus(PlayerService.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        mPlayer.play();
                    }
                }
                Looper.loop();
            }
        }).start();
    }

    private boolean isPlaying() {
        return mPlayer != null && mPlayer.getState() == TritonPlayer.STATE_PLAYING;
    }

    private boolean isConnecting() {
        return mPlayer != null && mPlayer.getState() == TritonPlayer.STATE_CONNECTING;
    }

    public void play() {
        if (mCurrentStream == null && mCurrentOnDemandStream == null) return;
        releasePlayer();
        playMedia();
        showNotification();
    }


    public void stop() {
        if (!isPlaying() || mPlayer == null) return;
        mCurrentTrack = null;
        mPlayer.stop();
    }

    public int getState() {
        if (mPlayer == null) {
            return -1;
        }
        return mPlayer.getState();
    }

    private void releasePlayer() {
        if (mPlayer == null) return;
        int state = mPlayer.getState();
        if (state == TritonPlayer.STATE_CONNECTING || state == TritonPlayer.STATE_PLAYING || state == TritonPlayer.STATE_PAUSED) {
            mPlayer.stop();
        }
        mPlayer.release();
        mPlayer = null;
    }

    public void pause() {
        if (!isPlaying()) return;
        mPlayer.pause();
    }

    public void unPause() {
        if (isPlaying()) return;
        AudioManager audioManager = getAudioManager();
        if (audioManager != null) {
            int result = audioManager.requestAudioFocus(PlayerService.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mPlayer.play();
            }
        }
        showNotification();
    }

    public void quit() {
        stop();
        mBuilder = null;
        stopForeground(true);
    }

    public Stream getCurrentStream() {
        return mCurrentStream;
    }

    public Track getCurrentTrack() {
        return mCurrentTrack;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCuePointReceived(MediaPlayer mediaPlayer, Bundle cuePoint) {
        if (cuePoint == null) return;


        String cueType = cuePoint.getString("cue_type", null);
        if (cueType == null) return;
        switch (cueType) {
            case CUE_TYPE_TRACK:
                if (cuePoint.containsKey("cue_title") && cuePoint.containsKey("track_artist_name")) {
                    String artist = cuePoint.getString("track_artist_name");
                    String song = cuePoint.getString("cue_title");
                    int duration = cuePoint.getInt("cue_time_duration", 0);

                    mCurrentTrack = new Track(song, artist, duration);

                    mRemoteViews.setTextViewText(R.id.song_title, mCurrentTrack.getTitle());
                    mRemoteViews.setTextViewText(R.id.station_artist, mCurrentTrack.getArtist());

                    if (mNotificationManager != null && mBuilder != null) {
                        mNotificationManager.notify(NOTIFICATION_SERVICE, mBuilder.build());
                    }

                    notifyTrackUpdate();
                }
                break;
            case CUE_TYPE_AD:
                mCurrentTrack = new Track(true);
                mRemoteViews.setTextViewText(R.id.song_title, "Reclame");
                mRemoteViews.setTextViewText(R.id.station_artist, "Reclame");

                if (mNotificationManager != null && mBuilder != null) {
                    mNotificationManager.notify(NOTIFICATION_SERVICE, mBuilder.build());
                }

                notifyTrackUpdate();
                break;
        }
    }

    private void notifyTrackUpdate() {
        Intent intent = new Intent(EVENT_TRACK_CHANGED);
        intent.putExtra(ARG_TRACK, mCurrentTrack);
        sendBroadcast(intent);
    }

    private void notifyStationUpdate() {
        Intent intent = new Intent(EVENT_STREAM_CHANGED);
        intent.putExtra(ARG_STREAM, mCurrentStream);
        sendBroadcast(intent);
    }

    private void notifyStateUpdate(int state) {
        Intent intent = new Intent(EVENT_STATE_CHANGED);
        intent.putExtra(ARG_STREAM, mCurrentStream);
        intent.putExtra(ARG_STATE, state);
        sendBroadcast(intent);
    }

    @Override
    public void onStateChanged(MediaPlayer mediaPlayer, int state) {
        final Integer[] states = {TritonPlayer.STATE_COMPLETED, TritonPlayer.STATE_STOPPED, TritonPlayer.STATE_ERROR, TritonPlayer.STATE_PAUSED};
        if (Arrays.asList(states).contains(state)) {
            AudioManager audioManager = getAudioManager();
            if (audioManager != null) {
                audioManager.abandonAudioFocus(this);
            }
        }
        updateNotification();
        notifyStateUpdate(state);
    }

    @Override
    public void onMetaDataReceived(MediaPlayer mediaPlayer, Bundle bundle) {
        if (bundle == null) return;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                unPause();
                break;
        }
    }

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    public void showNotification() {
        if (isShowingNotification()) {
            return;
        }
        if (!IS_NOTIF_ACTIVE){
            return 
        }

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Music player notification";
            String description = "Music player notifications for this app.";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel(DEFAULT_CHANNEL, name, importance);
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.enableVibration(true);
            mChannel.setSound(null, null);
            mChannel.setVibrationPattern(new long[]{0L});
            mChannel.setLightColor(Color.RED);
            mNotificationManager.createNotificationChannel(mChannel);

        }

        int layout;
        if(PlayerService.BRAND.equals("slam")) {
            layout = R.layout.slam_player_small;
        } else {
            layout = R.layout.nl100_player_small;
        }
        mRemoteViews = new RemoteViews(getPackageName(), layout);
        mBuilder = new NotificationCompat.Builder(this, DEFAULT_CHANNEL);

        mBuilder
                .setVibrate(new long[]{0L})
                .setSound(null)
                .setCustomContentView(mRemoteViews)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_player_notification); //small icon
        startForeground(NOTIFICATION_SERVICE, mBuilder.build());

        updateNotification();
    }

    private void updateNotification() {
        if (isShowingNotification()) {
            Intent stopIntent = new Intent(this, PlayerService.class);
            stopIntent.setAction(ACTION_STOP);
            PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent quitIntent = new Intent(this, PlayerService.class);
            quitIntent.setAction(ACTION_QUIT);
            PendingIntent pendingQuitIntent = PendingIntent.getService(this, 0, quitIntent, 0);

            Intent playIntent = new Intent(this, PlayerService.class);
            //playIntent.putExtra(ARG_STATION, mCurrentStation);
            playIntent.setAction(ACTION_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
                if (activityManager != null) {
                    Intent target = null;
                    List<ActivityManager.AppTask> taskList = activityManager.getAppTasks();
                    for (ActivityManager.AppTask appTask : taskList) {
                        ActivityManager.RecentTaskInfo taskInfo = appTask.getTaskInfo();
                        if (taskInfo.baseIntent.getComponent() != null && taskInfo.baseIntent.getComponent().getPackageName().equals(getPackageName())) {
                            target = taskInfo.baseIntent;
                            target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            break;
                        }
                    }

                    if (target != null) {
                        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, target, PendingIntent.FLAG_UPDATE_CURRENT);
                        mRemoteViews.setOnClickPendingIntent(R.id.notification_clickable_content, contentIntent);
                    }
                }
            }

            // use right actions depending on playstate
            if (isConnecting()) {
                mRemoteViews.setViewVisibility(R.id.station_progress_bar, View.VISIBLE);
                mRemoteViews.setViewVisibility(R.id.station_play_pause_button, View.GONE);
            } else if (isPlaying()) {
                mRemoteViews.setViewVisibility(R.id.station_progress_bar, View.GONE);
                mRemoteViews.setViewVisibility(R.id.station_play_pause_button, View.VISIBLE);

                int pauseDrawable;
                if(PlayerService.BRAND.equals("slam")) {
                    pauseDrawable = R.drawable.icon_state_pause_slam;
                } else {
                    pauseDrawable = R.drawable.icon_state_pause_nl100;
                }
                mRemoteViews.setOnClickPendingIntent(R.id.station_play_pause_button, pausePendingIntent);
                mRemoteViews.setImageViewResource(R.id.station_audio_image, pauseDrawable);
            } else {
                mRemoteViews.setViewVisibility(R.id.station_progress_bar, View.GONE);
                mRemoteViews.setViewVisibility(R.id.station_play_pause_button, View.VISIBLE);

                int playDrawable;
                if(PlayerService.BRAND.equals("slam")) {
                    playDrawable = R.drawable.icon_state_play_slam;
                } else {
                    playDrawable = R.drawable.icon_state_play_nl100;
                }
                mRemoteViews.setOnClickPendingIntent(R.id.station_play_pause_button, playPendingIntent);
                mRemoteViews.setImageViewResource(R.id.station_audio_image, playDrawable);
            }

            if (!isPlaying() && mCurrentTrack == null) {
                mRemoteViews.setTextViewText(R.id.song_title, "-");
                mRemoteViews.setTextViewText(R.id.station_artist, "-");
            }

            int clearDrawable;
            if(PlayerService.BRAND.equals("slam")) {
                clearDrawable = R.drawable.ic_close_white_slam;
            } else {
                clearDrawable = R.drawable.ic_close_nl100;
            }

            mRemoteViews.setImageViewResource(R.id.station_exit_image, clearDrawable);
            mRemoteViews.setOnClickPendingIntent(R.id.station_exit, pendingQuitIntent);

            mNotificationManager.notify(NOTIFICATION_SERVICE, mBuilder.build());
        }
    }

    public boolean isShowingNotification() {
        return mBuilder != null;
    }

    private AudioManager getAudioManager() {
        return (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    private class MusicIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        // headset unplugged, pause if playing...
                        pause();
                        break;
                    case 1:
                        // headset is plugged, do absolutely nothing!
                        break;
                    default:
                        // no ides
                }
            }
        }
    }

}

