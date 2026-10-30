package com.cloudwebrtc.webrtc.audio;

import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordStartErrorCode;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordStateCallback;
import org.webrtc.audio.WebRtcAudioRecordUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The audio device module's record error and state callbacks, fanned out to
 * any number of listeners an app registers through
 * {@link com.cloudwebrtc.webrtc.FlutterWebRTCPlugin#audioRecordCallbacks}.
 *
 * It also keeps a refused microphone from aborting the process. Android 9 and
 * older let one app capture at a time, so AudioRecord.startRecording fails
 * while another app records. WebRtcAudioRecord marks itself started before it
 * tries and keeps the mark when the start fails, so the next start from native
 * code throws "startRecording called twice without stopRecording" and the JNI
 * check behind it aborts. Clearing the mark on a failed start turns that next
 * start into a retry.
 */
public class AudioRecordCallbackAdapter
        implements AudioRecordErrorCallback, AudioRecordStateCallback {

    private final List<AudioRecordErrorCallback> errorCallbacks = new ArrayList<>();
    private final List<AudioRecordStateCallback> stateCallbacks = new ArrayList<>();
    private volatile JavaAudioDeviceModule audioDeviceModule;

    /** The module these callbacks are installed on. */
    public void setAudioDeviceModule(JavaAudioDeviceModule audioDeviceModule) {
        this.audioDeviceModule = audioDeviceModule;
    }

    public void addErrorCallback(AudioRecordErrorCallback callback) {
        synchronized (errorCallbacks) {
            errorCallbacks.add(callback);
        }
    }

    public void removeErrorCallback(AudioRecordErrorCallback callback) {
        synchronized (errorCallbacks) {
            errorCallbacks.remove(callback);
        }
    }

    public void addStateCallback(AudioRecordStateCallback callback) {
        synchronized (stateCallbacks) {
            stateCallbacks.add(callback);
        }
    }

    public void removeStateCallback(AudioRecordStateCallback callback) {
        synchronized (stateCallbacks) {
            stateCallbacks.remove(callback);
        }
    }

    @Override
    public void onWebRtcAudioRecordInitError(String errorMessage) {
        for (AudioRecordErrorCallback callback : errorCallbacks()) {
            callback.onWebRtcAudioRecordInitError(errorMessage);
        }
    }

    @Override
    public void onWebRtcAudioRecordStartError(
            AudioRecordStartErrorCode errorCode, String errorMessage) {
        // Called from within the failed start; it returns false next, which
        // native code takes as "not recording" and retries later.
        WebRtcAudioRecordUtils.allowRestart(audioDeviceModule);
        for (AudioRecordErrorCallback callback : errorCallbacks()) {
            callback.onWebRtcAudioRecordStartError(errorCode, errorMessage);
        }
    }

    @Override
    public void onWebRtcAudioRecordError(String errorMessage) {
        for (AudioRecordErrorCallback callback : errorCallbacks()) {
            callback.onWebRtcAudioRecordError(errorMessage);
        }
    }

    @Override
    public void onWebRtcAudioRecordStart() {
        for (AudioRecordStateCallback callback : stateCallbacks()) {
            callback.onWebRtcAudioRecordStart();
        }
    }

    @Override
    public void onWebRtcAudioRecordStop() {
        for (AudioRecordStateCallback callback : stateCallbacks()) {
            callback.onWebRtcAudioRecordStop();
        }
    }

    private List<AudioRecordErrorCallback> errorCallbacks() {
        synchronized (errorCallbacks) {
            return new ArrayList<>(errorCallbacks);
        }
    }

    private List<AudioRecordStateCallback> stateCallbacks() {
        synchronized (stateCallbacks) {
            return new ArrayList<>(stateCallbacks);
        }
    }
}
