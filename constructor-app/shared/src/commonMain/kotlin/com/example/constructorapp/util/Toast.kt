package com.example.constructorapp.util

object Toast {
    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1

    var toastHandler: ((String) -> Unit)? = null

    fun makeText(context: Any?, message: CharSequence?, duration: Int = LENGTH_SHORT): Toast {
        return Toast(message?.toString() ?: "")
    }

    fun makeText(message: CharSequence?, duration: Int = LENGTH_SHORT): Toast {
        return Toast(message?.toString() ?: "")
    }

    class Toast(val message: String) {
        fun show() {
            println("[Toast] $message")
            toastHandler?.invoke(message)
        }
    }
}
