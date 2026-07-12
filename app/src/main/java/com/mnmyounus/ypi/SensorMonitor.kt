package com.mnmyounus.ypi

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper

/**
 * SensorMonitor
 *
 * Detects camera, microphone, and audio hardware activity using
 * Android's event-driven system APIs — zero polling required.
 *
 * Detection mechanisms:
 * ┌─────────────┬──────────────────────────────────────────────────────┐
 * │ Camera      │ CameraManager.AvailabilityCallback                  │
 * │             │ Fires immediately on register for in-use cameras.   │
 * ├─────────────┼──────────────────────────────────────────────────────┤
 * │ Microphone  │ AudioManager.AudioRecordingCallback  (API 24+)      │
 * │             │ Fires for any active AudioRecord / MediaRecorder.   │
 * ├─────────────┼──────────────────────────────────────────────────────┤
 * │ Audio       │ AudioManager.AudioPlaybackCallback   (API 26+)      │
 * │             │ Fires for any active AudioTrack / MediaPlayer.      │
 * └─────────────┴──────────────────────────────────────────────────────┘
 *
 * All callbacks are delivered on the main thread (mainHandler passed to
 * each registration call). No synchronization primitives needed.
 */
class SensorMonitor(
    context: Context,
    private val onStateChanged: (camera: Boolean, mic: Boolean, audio: Boolean) -> Unit
) {
    private val appContext    = context.applicationContext
    private val mainHandler   = Handler(Looper.getMainLooper())
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager  = appContext.getSystemService(Context.AUDIO_SERVICE)  as AudioManager

    // Track unavailable (in-use) camera IDs — a set handles multi-camera
    // scenarios (e.g. front + back simultaneously) correctly.
    private val activeCameraIds = mutableSetOf<String>()

    private var cameraActive = false
    private var micActive    = false
    private var audioActive  = false

    // ── Camera ────────────────────────────────────────────────────

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        // Fires immediately on registration for any currently in-use camera
        override fun onCameraUnavailable(cameraId: String) {
            activeCameraIds.add(cameraId)
            cameraActive = true
            dispatch()
        }
        override fun onCameraAvailable(cameraId: String) {
            activeCameraIds.remove(cameraId)
            cameraActive = activeCameraIds.isNotEmpty()
            dispatch()
        }
    }

    // ── Microphone ────────────────────────────────────────────────

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        // Fires whenever any app starts or stops microphone recording
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            micActive = configs.isNotEmpty()
            dispatch()
        }
    }

    // ── Audio Playback ────────────────────────────────────────────

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        // Fires whenever any app starts or stops audio playback
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            audioActive = configs.isNotEmpty()
            dispatch()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    fun start() {
        // Camera callback fires immediately for any currently in-use cameras
        cameraManager.registerAvailabilityCallback(cameraCallback, mainHandler)

        // Seed mic and audio states synchronously before callbacks fire
        micActive = try {
            audioManager.activeRecordingConfigurations.isNotEmpty()
        } catch (_: Exception) { false }

        audioActive = try {
            audioManager.activePlaybackConfigurations.isNotEmpty()
        } catch (_: Exception) { false }

        audioManager.registerAudioRecordingCallback(recordingCallback, mainHandler)
        audioManager.registerAudioPlaybackCallback(playbackCallback,   mainHandler)

        dispatch()  // publish the initial combined state right away
    }

    fun stop() {
        cameraManager.unregisterAvailabilityCallback(cameraCallback)
        audioManager.unregisterAudioRecordingCallback(recordingCallback)
        audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        activeCameraIds.clear()
    }

    private fun dispatch() {
        onStateChanged(cameraActive, micActive, audioActive)
    }
}
