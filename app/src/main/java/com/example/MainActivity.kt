package com.example

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.crash.CrashLogManager
import com.example.crash.CrashLogScreen
import com.example.data.preferences.AppSettings
import com.example.di.AppModule
import com.example.playback.PlaybackManager
import com.example.playback.PlaybackMode
import com.example.playback.PlaybackState
import com.example.ui.navigation.SwarNavGraph
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.theme.NeumorphicTheme
import com.example.ui.theme.SwarMusicTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val ACTION_PIP_PLAY_PAUSE = "com.skt.swaemusic.PIP_PLAY_PAUSE"
        const val ACTION_PIP_SKIP_NEXT = "com.skt.swaemusic.PIP_SKIP_NEXT"
        const val ACTION_PIP_SKIP_PREVIOUS = "com.skt.swaemusic.PIP_SKIP_PREVIOUS"
    }

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var playbackManager: PlaybackManager
    private var isInPipModeState by mutableStateOf(false)
    private var isReceiverRegistered = false

    private val pipBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "PiP RemoteAction received: ${intent?.action}")
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> {
                    playbackManager.playPause()
                }
                ACTION_PIP_SKIP_NEXT -> {
                    playbackManager.skipToNext()
                }
                ACTION_PIP_SKIP_PREVIOUS -> {
                    playbackManager.skipToPrevious()
                }
            }
            updatePipParams(playbackManager.state.value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppModule.init(applicationContext)
        playbackManager = AppModule.providePlaybackManager(applicationContext)

        // Register PiP action broadcast receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_SKIP_NEXT)
            addAction(ACTION_PIP_SKIP_PREVIOUS)
        }
        try {
            ContextCompat.registerReceiver(
                this,
                pipBroadcastReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register PiP receiver: ${e.message}")
        }

        // Continually observe playback state to keep PiP params & Auto-Enter up to date
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playbackManager.state.collect { state ->
                    updatePipParams(state)
                }
            }
        }

        setContent {
            var activeCrashLog by remember {
                mutableStateOf(CrashLogManager.getCrashLog(this@MainActivity))
            }

            if (activeCrashLog != null) {
                CrashLogScreen(
                    crashLog = activeCrashLog ?: "",
                    onDismissAndContinue = {
                        CrashLogManager.clearCrashLog(this@MainActivity)
                        activeCrashLog = null
                    },
                    onClearLog = {
                        CrashLogManager.clearCrashLog(this@MainActivity)
                        activeCrashLog = null
                    }
                )
            } else {
                val settingsPreferences = remember { AppModule.provideSettingsPreferences(applicationContext) }
                val appSettings by settingsPreferences.settings.collectAsStateWithLifecycle(initialValue = AppSettings())

                SwarMusicTheme(
                    themeMode = appSettings.themeMode,
                    highContrast = appSettings.highContrastMode,
                    fontSizeScale = appSettings.fontSizeScale
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = NeumorphicTheme.colors.background
                    ) {
                        val navController = rememberNavController()
                        SwarNavGraph(
                            navController = navController,
                            currentThemeMode = appSettings.themeMode,
                            onThemeModeChange = { newMode ->
                                homeViewModel.setThemeMode(newMode)
                            },
                            isInPipMode = isInPipModeState,
                            onEnterPip = { enterPipMode() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInPipModeState = isInPictureInPictureMode
        }
        updatePipParams(playbackManager.state.value)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = playbackManager.state.value
        // Fallback for API < 31 (where setAutoEnterEnabled is not available) or system gesture fallback
        if (state.playbackMode == PlaybackMode.VIDEO && state.isPlaying) {
            Log.d(TAG, "onUserLeaveHint: Entering PiP for video playback")
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Log.d(TAG, "onPictureInPictureModeChanged: isInPictureInPictureMode=$isInPictureInPictureMode")
        isInPipModeState = isInPictureInPictureMode
    }

    fun enterPipMode(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val state = playbackManager.state.value
                val actions = createPipActions(state.isPlaying)
                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setActions(actions)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val isVideoPlaying = state.playbackMode == PlaybackMode.VIDEO && state.isPlaying
                    builder.setAutoEnterEnabled(isVideoPlaying)
                }

                return enterPictureInPictureMode(builder.build())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enter Picture-in-Picture mode", e)
            }
        }
        return false
    }

    private fun updatePipParams(state: PlaybackState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val actions = createPipActions(state.isPlaying)
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(actions)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Auto-enter PiP only when actively playing in Video mode
                val shouldAutoEnter = state.playbackMode == PlaybackMode.VIDEO && state.isPlaying
                builder.setAutoEnterEnabled(shouldAutoEnter)
            }

            setPictureInPictureParams(builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update PiP parameters: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPipActions(isPlaying: Boolean): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()

        // 1. Previous Track Action
        try {
            val prevIntent = PendingIntent.getBroadcast(
                this,
                1001,
                Intent(ACTION_PIP_SKIP_PREVIOUS).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val prevIcon = Icon.createWithResource(this, android.R.drawable.ic_media_previous)
            val prevAction = RemoteAction(
                prevIcon,
                "Previous",
                "Previous track",
                prevIntent
            )
            actions.add(prevAction)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create PiP previous action: ${e.message}")
        }

        // 2. Play/Pause Action
        try {
            val playPauseIntent = PendingIntent.getBroadcast(
                this,
                1002,
                Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val playPauseIcon = Icon.createWithResource(
                this,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
            val playPauseAction = RemoteAction(
                playPauseIcon,
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) "Pause video" else "Play video",
                playPauseIntent
            )
            actions.add(playPauseAction)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create PiP play/pause action: ${e.message}")
        }

        // 3. Next Track Action
        try {
            val nextIntent = PendingIntent.getBroadcast(
                this,
                1003,
                Intent(ACTION_PIP_SKIP_NEXT).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val nextIcon = Icon.createWithResource(this, android.R.drawable.ic_media_next)
            val nextAction = RemoteAction(
                nextIcon,
                "Next",
                "Next track",
                nextIntent
            )
            actions.add(nextAction)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create PiP next action: ${e.message}")
        }

        return actions
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(pipBroadcastReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister PiP receiver: ${e.message}")
            }
        }
    }
}
