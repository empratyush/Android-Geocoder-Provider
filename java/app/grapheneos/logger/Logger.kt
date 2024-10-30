package app.grapheneos.logger

import android.os.Build
import android.util.Log

class Logger(private val tag: String) {

    fun d(msg: String) {
        if (Log.isLoggable(tag, Log.DEBUG) || Build.IS_DEBUGGABLE) {
            Log.d(tag, msg)
        }
    }

}
