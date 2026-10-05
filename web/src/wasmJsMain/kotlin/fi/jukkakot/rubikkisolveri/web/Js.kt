@file:JsModule("./platform.mjs")

package fi.jukkakot.rubikkisolveri.web

import org.khronos.webgl.Int8Array

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
external fun installGraphicsLostHook(report: (Boolean) -> Unit)
external fun shareOrDownload(logName: String, logText: String, pictureNames: String, pictureData: String, report: (String, String) -> Unit)

external fun cameraAcquire(done: (String) -> Unit)
external fun cameraRelease()
external fun cameraVideoWidth(): Int
external fun cameraVideoHeight(): Int
external fun cameraGrab(x: Int, y: Int, w: Int, h: Int, previewLong: Int, size: Int): Boolean
external fun cameraPreviewWidth(): Int
external fun cameraPreviewHeight(): Int
external fun cameraPreviewData(): Int8Array
external fun cameraAnalysisData(): Int8Array
external fun cameraTorchSupported(): Boolean
external fun cameraInfo(): String
external fun cameraSetTorch(on: Boolean)
external fun cameraLockExposure(lock: Boolean): String
external fun cameraShow(x: Double, y: Double, w: Double, h: Double)
external fun cameraHide()
