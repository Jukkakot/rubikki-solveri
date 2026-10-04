@file:JsModule("./platform.mjs")

package fi.jukkakot.rubikkisolveri.web

// The functions of platform.mjs (the only JavaScript the app has).

external fun storageGet(key: String): String?
external fun storageSet(key: String, value: String)
external fun storageRemove(key: String)
external fun persistStorage()

external fun log(level: String, line: String)
external fun hideLoading()
external fun reload()
external fun queryFlag(name: String): Boolean
external fun setThemeColor(color: String)
external fun reducedMotion(): Boolean
external fun vibrate(ms: Int)
external fun formatDateTime(epochMillis: Double, language: String, timeOnly: Boolean): String
external fun installCrashHooks(report: (String) -> Unit)
external fun shareOrDownload(logName: String, logText: String, pictureNames: String, pictureData: String)
