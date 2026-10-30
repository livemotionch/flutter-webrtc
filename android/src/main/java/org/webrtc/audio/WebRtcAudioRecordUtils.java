package org.webrtc.audio;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In this package because WebRtcAudioRecord is package-private.
 */
public abstract class WebRtcAudioRecordUtils {

    private static final String TAG = "WebRtcAudioRecordUtils";

    /**
     * Clear the mark WebRtcAudioRecord.startRecording sets before it starts
     * and leaves behind when the start fails, so the next start from native
     * code retries instead of throwing. Only the webrtc-sdk build has the
     * mark; without it there is nothing to clear.
     */
    public static void allowRestart(JavaAudioDeviceModule audioDeviceModule) {
        if (audioDeviceModule == null) return;
        try {
            Field field = WebRtcAudioRecord.class.getDeclaredField("nativeCalledStartRecording");
            field.setAccessible(true);
            ((AtomicBoolean) field.get(audioDeviceModule.audioInput)).set(false);
        } catch (NoSuchFieldException e) {
            // A WebRTC build that does not keep the mark.
        } catch (ReflectiveOperationException | ClassCastException e) {
            Log.w(TAG, "could not clear the failed recording start", e);
        }
    }
}
