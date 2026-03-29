package org.klab.alwaysonfps

import android.content.Context
import android.provider.Settings
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class AlwaysOnFingerprint : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        if (lpparam.packageName == "com.android.settings") {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.android.settings.biometrics.fingerprint.FingerprintSettingsScreenOffUnlockUdfpsPreferenceController",
                    lpparam.classLoader,
                    "getAvailabilityStatus",
                    XC_MethodReplacement.returnConstant(0)
                )

                XposedHelpers.findAndHookMethod(
                    "com.android.settings.biometrics.fingerprint.FingerprintSettings\$FingerprintSettingsFragment",
                    lpparam.classLoader,
                    "isScreenOffUnlcokSupported",
                    XC_MethodReplacement.returnConstant(true)
                )
                XposedBridge.log("Hooked Settings")
            } catch (t: Throwable) {
                XposedBridge.log("Failed to hook Settings: \$t")
            }
        }

        try {
            XposedHelpers.findAndHookMethod(
                "android.hardware.display.AmbientDisplayConfiguration",
                lpparam.classLoader,
                "screenOffUdfpsEnabled",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                        val userId = param.args[0] as Int

                        val enabled = XposedHelpers.callStaticMethod(
                            Settings.Secure::class.java, "getIntForUser",
                            context.contentResolver, "screen_off_udfps_enabled", 0, userId
                        ) as Int

                        if (enabled == 1) {
                            param.result = true
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                "android.hardware.display.AmbientDisplayConfiguration",
                lpparam.classLoader,
                "udfpsLongPressSensorType",
                XC_MethodReplacement.returnConstant("com.google.sensor.long_press")
            )
            XposedBridge.log("Hooked AmbientDisplayConfiguration")
        } catch (t: Throwable) {
            XposedBridge.log("Failed to hook AmbientDisplayConfiguration: \$t")
        }

        if (lpparam.packageName != "com.android.systemui") return

        try {
            XposedHelpers.findAndHookMethod(
                "com.android.keyguard.KeyguardUpdateMonitor",
                lpparam.classLoader,
                "isFingerprintDetectionRunning",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.result as Boolean) return
                        val userId = 0
                        val mFace = XposedHelpers.findMethodExactIfExists(
                            param.thisObject.javaClass,
                            "getUserFaceAuthenticated",
                            Int::class.javaPrimitiveType
                        )
                        if (mFace != null) {
                            val isFaceAuthenticated = mFace.invoke(param.thisObject, userId) as Boolean
                            if (isFaceAuthenticated) return
                        }

                        val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context

                        if (context != null && Settings.Secure.getInt(context.contentResolver, "screen_off_udfps_enabled", 0) == 1) {
                            val isInteractive = XposedHelpers.getBooleanField(param.thisObject, "mDeviceInteractive")

                            if (!isInteractive) {
                                param.result = true
                            }
                        }
                    }
                }
            )
            XposedBridge.log("Hooked KeyguardUpdateMonitor")
        } catch (t: Throwable) {
            XposedBridge.log("Failed to hook KeyguardUpdateMonitor: \$t")
        }

        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.biometrics.UdfpsController",
                lpparam.classLoader,
                "onFingerDown",
                Long::class.javaPrimitiveType, Int::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Long::class.javaPrimitiveType, Long::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                        if (Settings.Secure.getInt(context.contentResolver, "screen_off_udfps_enabled", 0) == 1) {
                            XposedHelpers.setBooleanField(param.thisObject, "mIgnoreRefreshRate", true)
                        }
                    }
                }
            )
            XposedBridge.log("Hooked UdfpsController")
        } catch (t: Throwable) {
            XposedBridge.log("Failed to hook UdfpsController: \$t")
        }
    }
}
