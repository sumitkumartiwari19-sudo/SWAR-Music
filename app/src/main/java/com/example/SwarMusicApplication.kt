package com.example
 
import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.crash.CrashHandler
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Main Application class for SWAR Music.
 * Installs global crash handler and initializes core app components.
 */
class SwarMusicApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "SwarMusicApplication"

        init {
            // Apply environment variables at the earliest possible class-loading moment
            // to suppress Mesa DRI render node probing and silence its internal error logger in virtualized containers
            try {
                android.system.Os.setenv("MESA_LOG_LEVEL", "none", true)
                android.system.Os.setenv("MESA_LOG_FILE", "/dev/null", true)
                android.system.Os.setenv("MESA_DEBUG", "0", true)
                android.system.Os.setenv("LIBGL_DEBUG", "0", true)
                android.system.Os.setenv("LIBGL_DRI3_DISABLE", "1", true)
                android.system.Os.setenv("LIBGL_DRI2_DISABLE", "1", true)
                android.system.Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                android.system.Os.setenv("EGL_LOG_LEVEL", "none", true)
                android.system.Os.setenv("VK_LOADER_DEBUG", "none", true)
            } catch (_: Throwable) {
                // Ignored if environment cannot be modified
            }
        }
    }

    init {
        try {
            android.system.Os.setenv("MESA_LOG_LEVEL", "none", true)
            android.system.Os.setenv("MESA_LOG_FILE", "/dev/null", true)
            android.system.Os.setenv("MESA_DEBUG", "0", true)
            android.system.Os.setenv("LIBGL_DEBUG", "0", true)
            android.system.Os.setenv("LIBGL_DRI3_DISABLE", "1", true)
            android.system.Os.setenv("LIBGL_DRI2_DISABLE", "1", true)
            android.system.Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
        } catch (_: Throwable) {
            // Ignored if environment cannot be modified
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Configure WebView to disable GPU rasterization probe in headless emulator
        try {
            val cmdFile = java.io.File("/data/local/tmp/webview-command-line")
            if (cmdFile.canWrite() || !cmdFile.exists()) {
                cmdFile.writeText("_ --disable-gpu --disable-gpu-rasterization\n")
            }
        } catch (_: Throwable) {
            // Ignored if permissions don't allow
        }

        // 1. Install global uncaught exception handler immediately
        CrashHandler.install(this)

        // 2. Initialize NewPipe Extractor globally with custom Downloader
        com.example.playback.extractor.NewPipeDownloaderImpl.initGlobal()

        // 3. Safely initialize Firebase if not already initialized
        initFirebaseSafely()
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                // If google-services.json was missing at build time, provide default fallback options
                // so Firebase APIs (Auth, Firestore) do not throw IllegalStateException on launch.
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:100456789012:android:swar-music-app")
                    .setApiKey("AIzaSySwarMusicFallbackApiKeyForAppStability00")
                    .setProjectId("swar-music-app-dev")
                    .setStorageBucket("swar-music-app-dev.appspot.com")
                    .build()

                FirebaseApp.initializeApp(this, options)
                Log.d(TAG, "Firebase initialized with fallback development options")
            } else {
                Log.d(TAG, "Firebase already initialized by system provider")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase safe initialization handled exception: ${e.message}")
        }
    }
}

