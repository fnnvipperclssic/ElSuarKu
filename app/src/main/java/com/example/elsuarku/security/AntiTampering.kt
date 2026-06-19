package com.example.elsuarku.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Anti-tampering checks for e-voting security:
 * - Root detection
 * - Emulator detection
 * - Debugger detection
 * - Hook framework detection (Frida, Xposed)
 *
 * These checks help ensure vote integrity by preventing compromised devices
 * from participating in elections.
 */
class AntiTampering(private val context: Context) {

    data class IntegrityResult(
        val isSafe: Boolean,
        val threats: List<String> = emptyList()
    )

    /**
     * Run all integrity checks. Returns true only if ALL pass.
     */
    fun performFullCheck(): IntegrityResult {
        val threats = mutableListOf<String>()

        if (isDeviceRooted()) threats.add("ROOT_DETECTED")
        if (isEmulator()) threats.add("EMULATOR_DETECTED")
        if (isDebuggerAttached()) threats.add("DEBUGGER_ATTACHED")
        if (isHookFrameworkDetected()) threats.add("HOOK_FRAMEWORK_DETECTED")

        return IntegrityResult(
            isSafe = threats.isEmpty(),
            threats = threats
        )
    }

    /**
     * Check if the device is rooted.
     * Checks for common root binaries, Superuser APK, and su existence.
     */
    fun isDeviceRooted(): Boolean {
        return checkForRootBinaries() ||
                checkForSuperuserApk() ||
                checkForSuBinary() ||
                checkForDangerousProperties() ||
                checkForWritePermissionOnSystem()
    }

    /**
     * Check if running on an emulator.
     */
    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.PRODUCT.contains("sdk_google") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("vbox86p") ||
                Build.PRODUCT.contains("emulator")
    }

    /**
     * Check if a debugger is attached to the process.
     */
    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * Check for hook frameworks like Frida and Xposed.
     */
    fun isHookFrameworkDetected(): Boolean {
        return checkForXposed() || checkForFrida() || checkForSubstrate()
    }

    // ---- Private Check Methods ----

    private fun checkForRootBinaries(): Boolean {
        val rootBinaries = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return rootBinaries.any { File(it).exists() }
    }

    private fun checkForSuperuserApk(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.noshufou.android.su", PackageManager.GET_ACTIVITIES)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun checkForSuBinary(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            reader.close()
            process.destroy()
            line != null
        } catch (_: Exception) {
            false
        }
    }

    private fun checkForDangerousProperties(): Boolean {
        val dangerousProps = arrayOf("ro.debuggable", "ro.secure")
        return try {
            val process = Runtime.getRuntime().exec("getprop")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.destroy()
            // ro.debuggable = 1 or ro.secure = 0 indicates root
            output.contains("[ro.debuggable]: [1]") || output.contains("[ro.secure]: [0]")
        } catch (_: Exception) {
            false
        }
    }

    private fun checkForWritePermissionOnSystem(): Boolean {
        val systemDirs = arrayOf("/system", "/system/bin", "/system/xbin", "/vendor")
        return systemDirs.any { File(it).canWrite() }
    }

    private fun checkForXposed(): Boolean {
        return try {
            // Check for Xposed class loader
            ClassLoader.getSystemClassLoader()
                .loadClass("de.robv.android.xposed.XposedBridge")
            true
        } catch (_: ClassNotFoundException) {
            // Also check for Xposed files
            File("/data/data/de.robv.android.xposed.installer").exists() ||
                    File("/data/local/xposed").exists()
        }
    }

    private fun checkForFrida(): Boolean {
        // Frida injects its library into the process
        return try {
            System.getProperty("os.arch")?.contains("frida") == true
        } catch (_: Exception) {
            // Check for Frida server on device
            File("/data/local/tmp/frida-server").exists() ||
                    File("/data/local/tmp/re.frida.server").exists()
        }
    }

    private fun checkForSubstrate(): Boolean {
        return File("/data/local/tmp/libsubstrate.so").exists() ||
                File("/data/local/tmp/libsubstrate-dvm.so").exists() ||
                File("/system/lib/libsubstrate.so").exists()
    }
}
