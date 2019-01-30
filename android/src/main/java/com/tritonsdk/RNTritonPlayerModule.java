
package com.tritonsdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.tritonsdk.impl.PlayerService;

import static android.content.Context.BIND_AUTO_CREATE;

public class RNTritonPlayerModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    private PlayerService mService;
    private boolean mServiceBound;

    public RNTritonPlayerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNTritonPlayer";
    }

    @ReactMethod
    public void initPlayer() {
        if (!mServiceBound) {
            Context context = getReactApplicationContext();
            Intent intent = new Intent(context, PlayerService.class);
            intent.setAction(PlayerService.ACTION_INIT);
            getReactApplicationContext().bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
            getReactApplicationContext().startService(intent);
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mServiceBound = true;
            mService = ((PlayerService.LocalBinder) binder).getService();

            if (mService.getCurrentStream() != null) {
                //onStreamChanged(mService.getCurrentStream());
            }
            if (mService.getCurrentTrack() != null) {
                //onTrackChanged(mService.getCurrentTrack());
            }

            //onStateChanged(mService.getState());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }
    };
}