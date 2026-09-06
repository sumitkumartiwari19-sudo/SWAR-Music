package com.example.crash

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CrashHandlerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        CrashLogManager.clearCrashLog(context)
    }

    @Test
    fun testCrashLogSaveAndRead() {
        val sampleCrashLog = "Sample crash report stack trace"
        CrashLogManager.saveCrashLog(context, sampleCrashLog)

        val retrievedLog = CrashLogManager.getCrashLog(context)
        assertNotNull(retrievedLog)
        assertTrue(retrievedLog!!.contains("Sample crash report stack trace"))
    }

    @Test
    fun testCrashLogClear() {
        CrashLogManager.saveCrashLog(context, "Temporary error")
        assertTrue(CrashLogManager.getCrashLog(context) != null)

        val cleared = CrashLogManager.clearCrashLog(context)
        assertTrue(cleared)
        assertNull(CrashLogManager.getCrashLog(context))
    }

    @Test
    fun testCrashHandlerFormatsReport() {
        val handler = CrashHandler(context, defaultHandler = null, exitOnCrash = false)
        val dummyException = RuntimeException("Test failure in Swar Music Engine", IllegalStateException("Root cause issue"))

        handler.uncaughtException(Thread.currentThread(), dummyException)

        val log = CrashLogManager.getCrashLog(context)
        assertNotNull(log)
        assertTrue(log!!.contains("SWAR MUSIC CRASH REPORT"))
        assertTrue(log.contains("Test failure in Swar Music Engine"))
        assertTrue(log.contains("ROOT CAUSE:"))
        assertTrue(log.contains("IllegalStateException"))
    }
}
