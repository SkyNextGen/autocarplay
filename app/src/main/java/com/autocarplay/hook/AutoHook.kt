package com.autocarplay.hook

import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class AutoHook : IXposedHookLoadPackage {

    private val TARGET = "android.car.usb.handler"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET) return

        XposedBridge.log("AutoCarPlayClicker: loaded $TARGET")

        XposedHelpers.findAndHookMethod(
            AlertDialog::class.java,
            "show",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val dlg = param.thisObject as? AlertDialog ?: return

                    val title = getText(dlg, android.R.id.title)
                    val msg = getText(dlg, android.R.id.message)
                    val text = (title + "\n" + msg).lowercase()

                    if (!text.contains("carplay")) return

                    Handler(Looper.getMainLooper()).postDelayed({
                        runCatching {
                            val positive = dlg.getButton(AlertDialog.BUTTON_POSITIVE)
                            if (positive != null && positive.isEnabled && positive.isShown) {
                                positive.performClick()
                                XposedBridge.log("AutoCarPlayClicker: clicked POSITIVE")
                            }
                        }.onFailure {
                            XposedBridge.log("AutoCarPlayClicker: click failed $it")
                        }
                    }, 180)
                }
            }
        )
    }

    private fun getText(dlg: AlertDialog, id: Int): String =
        runCatching { dlg.findViewById<TextView>(id)?.text?.toString().orEmpty() }.getOrDefault("")
}
