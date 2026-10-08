@file:JsModule("./worker.mjs")

package fi.jukkakot.rubikkisolveri.worker

import org.khronos.webgl.Int8Array

// The functions of worker.mjs.

external fun workerListen(find: (Int, Int, Int8Array) -> String, scanned: () -> String, command: (String) -> String)
external fun now(): Double
