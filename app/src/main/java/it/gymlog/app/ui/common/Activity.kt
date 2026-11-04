package it.gymlog.app.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** L'Activity che ospita la schermata: serve a Credential Manager per aprire il selettore account. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
