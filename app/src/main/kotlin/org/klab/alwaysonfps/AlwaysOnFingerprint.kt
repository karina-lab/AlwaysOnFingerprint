package org.klab.alwaysonfps

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

class AlwaysOnFingerprint : XposedModule() {

    private companion object {
        const val TAG = "AlwaysOnFPS"
    }

    @SuppressLint("PrivateApi", "BlockedPrivateApi")
    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        val packageName = param.packageName
        val classLoader = param.classLoader

        if (packageName == "com.android.settings") {
            try {
                val clazz1 = classLoader.loadClass("com.android.settings.biometrics.fingerprint.FingerprintSettingsScreenOffUnlockUdfpsPreferenceController")
                hook(clazz1.getDeclaredMethod("getAvailabilityStatus")).intercept(ConstantHooker(0))

                val clazz2 = classLoader.loadClass("com.android.settings.biometrics.fingerprint.FingerprintSettings\$FingerprintSettingsFragment")
                hook(clazz2.getDeclaredMethod("isScreenOffUnlcokSupported")).intercept(ConstantHooker(true))

                log(Log.INFO, TAG, "Hooked Settings")
            } catch (t: Throwable) {
                log(Log.ERROR, TAG, "Failed to hook Settings", t)
            }
        }

        try {
            val clazz = classLoader.loadClass("android.hardware.display.AmbientDisplayConfiguration")
            hook(clazz.getDeclaredMethod("screenOffUdfpsEnabled", Int::class.javaPrimitiveType)).intercept(AmbientDisplayHooker())
            hook(clazz.getDeclaredMethod("udfpsLongPressSensorType")).intercept(ConstantHooker("com.google.sensor.long_press"))
            log(Log.INFO, TAG, "Hooked AmbientDisplayConfiguration")
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "Failed to hook AmbientDisplayConfiguration", t)
        }

        if (packageName == "com.android.systemui") {
            try {
                val clazz = classLoader.loadClass("com.android.keyguard.KeyguardUpdateMonitor")
                hook(clazz.getDeclaredMethod("isFingerprintDetectionRunning")).intercept(KeyguardUpdateMonitorHooker())
                log(Log.INFO, TAG, "Hooked KeyguardUpdateMonitor")
            } catch (t: Throwable) {
                log(Log.ERROR, TAG, "Failed to hook KeyguardUpdateMonitor", t)
            }

            try {
                val clazz = classLoader.loadClass("com.android.systemui.biometrics.UdfpsController")
                val method = clazz.declaredMethods.find { m ->
                    m.name == "onFingerDown" && m.parameterCount >= 10
                }
                if (method != null) {
                    hook(method).intercept(UdfpsControllerHooker())
                    log(Log.INFO, TAG, "Hooked UdfpsController")
                }
            } catch (t: Throwable) {
                log(Log.ERROR, TAG, "Failed to hook UdfpsController", t)
            }
        }
    }

    inner class ConstantHooker(private val value: Any) : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any {
            return value
        }
    }

    inner class AmbientDisplayHooker : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            val instance = chain.thisObject ?: return chain.proceed()
            val userId = chain.getArg(0) as Int
            
            try {
                val mContextField = instance.javaClass.getDeclaredField("mContext")
                mContextField.isAccessible = true
                val context = mContextField.get(instance) as Context

                val enabled = Settings.Secure.getInt(
                    context.contentResolver, "screen_off_udfps_enabled", 0
                )

                if (enabled == 1) {
                    return true
                }
            } catch (e: Exception) {
            }
            return chain.proceed()
        }
    }

    inner class KeyguardUpdateMonitorHooker : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            val result = chain.proceed()
            if (result as? Boolean == true) return true
            
            val instance = chain.thisObject ?: return result
            val userId = 0
            
            try {
                val mFaceMethod = instance.javaClass.declaredMethods.find { it.name == "getUserFaceAuthenticated" }
                if (mFaceMethod != null) {
                    mFaceMethod.isAccessible = true
                    val isFaceAuthenticated = mFaceMethod.invoke(instance, userId) as Boolean
                    if (isFaceAuthenticated) return result
                }

                val mContextField = instance.javaClass.getDeclaredField("mContext")
                mContextField.isAccessible = true
                val context = mContextField.get(instance) as? Context

                if (context != null && Settings.Secure.getInt(context.contentResolver, "screen_off_udfps_enabled", 0) == 1) {
                    val mDeviceInteractiveField = instance.javaClass.getDeclaredField("mDeviceInteractive")
                    mDeviceInteractiveField.isAccessible = true
                    val isInteractive = mDeviceInteractiveField.getBoolean(instance)

                    if (!isInteractive) {
                        return true
                    }
                }
            } catch (e: Exception) {
            }
            return result
        }
    }

    inner class UdfpsControllerHooker : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            val instance = chain.thisObject
            if (instance != null) {
                try {
                    val mContextField = instance.javaClass.getDeclaredField("mContext")
                    mContextField.isAccessible = true
                    val context = mContextField.get(instance) as Context
                    
                    if (Settings.Secure.getInt(context.contentResolver, "screen_off_udfps_enabled", 0) == 1) {
                        val mIgnoreRefreshRateField = instance.javaClass.getDeclaredField("mIgnoreRefreshRate")
                        mIgnoreRefreshRateField.isAccessible = true
                        mIgnoreRefreshRateField.setBoolean(instance, true)
                    }
                } catch (e: Exception) {
                }
            }
            return chain.proceed()
        }
    }
}
