package org.klab.alwaysonfps;

import android.content.Context;
import android.provider.Settings;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AlwaysOnFingerprint implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals("com.android.settings")) {
            try {
                XposedHelpers.findAndHookMethod(
                        "com.android.settings.biometrics.fingerprint.FingerprintSettingsScreenOffUnlockUdfpsPreferenceController",
                        lpparam.classLoader,
                        "getAvailabilityStatus",
                        XC_MethodReplacement.returnConstant(0)
                );

                XposedHelpers.findAndHookMethod(
                        "com.android.settings.biometrics.fingerprint.FingerprintSettings$FingerprintSettingsFragment",
                        lpparam.classLoader,
                        "isScreenOffUnlcokSupported",
                        XC_MethodReplacement.returnConstant(true)
                );
            } catch (Throwable t) {
                XposedBridge.log("Failed to hook Settings: " + t);
            }
        }

        try {
            XposedHelpers.findAndHookMethod("android.hardware.display.AmbientDisplayConfiguration",
                    lpparam.classLoader,
                    "screenOffUdfpsEnabled",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            int userId = (int) param.args[0];

                            int enabled = (int) XposedHelpers.callStaticMethod(Settings.Secure.class, "getIntForUser",
                                    context.getContentResolver(), "screen_off_udfps_enabled", 0, userId);

                            if (enabled == 1) {
                                param.setResult(true);
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    "android.hardware.display.AmbientDisplayConfiguration",
                    lpparam.classLoader,
                    "udfpsLongPressSensorType",
                    XC_MethodReplacement.returnConstant("com.google.sensor.long_press")
            );
        } catch (Throwable t) {
            XposedBridge.log("Failed to hook AmbientDisplayConfiguration: " + t);
        }

        if (!lpparam.packageName.equals("com.android.systemui")) return;

        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.keyguard.KeyguardUpdateMonitor",
                    lpparam.classLoader,
                    "isFingerprintDetectionRunning",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                            if (Settings.Secure.getInt(context.getContentResolver(), "screen_off_udfps_enabled", 0) == 1) {
                                param.setResult(true);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("Failed to hook KeyguardUpdateMonitor: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.systemui.doze.DozeSensors$TriggerSensor",
                    lpparam.classLoader,
                    "updateListening",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object dozeSensors = XposedHelpers.getSurroundingThis(param.thisObject);
                            Context context = (Context) XposedHelpers.getObjectField(dozeSensors, "mContext");

                            if (Settings.Secure.getInt(context.getContentResolver(), "screen_off_udfps_enabled", 0) == 1) {
                                int pulseReason = XposedHelpers.getIntField(param.thisObject, "mPulseReason");
                                if (pulseReason == 10) { // 10 = UDFPS Long Press
                                    XposedHelpers.setBooleanField(param.thisObject, "mConfigured", true);
                                    XposedHelpers.setBooleanField(param.thisObject, "mRequested", true);
                                }
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("Failed to hook DozeSensors: " + t);
        }

        try {
            XposedHelpers.findAndHookConstructor(
                    "com.android.systemui.biometrics.UdfpsController",
                    lpparam.classLoader,
                    XposedHelpers.findClass("android.content.Context", lpparam.classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Context context = (Context) param.args[0];
                            if (Settings.Secure.getInt(context.getContentResolver(), "screen_off_udfps_enabled", 0) == 1) {
                                XposedHelpers.setBooleanField(param.thisObject, "mIgnoreRefreshRate", true);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("Failed to hook UdfpsController constructor: " + t);
        }
    }
}