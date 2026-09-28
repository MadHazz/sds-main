package com.DevCiplak.advdisplay.security

import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.DevCiplak.advdisplay.BuildConfig
import com.DevCiplak.advdisplay.R
import com.DevCiplak.advdisplay.data.SessionStore

object AdminGate {

    fun isPinValid(enteredPin: String): Boolean {
        return enteredPin == BuildConfig.ADMIN_PIN
    }

    fun clearSession(sessionStore: SessionStore) {
        sessionStore.clearSession()
    }

    fun confirm(activity: AppCompatActivity, onCancel: () -> Unit = {}, onConfirmed: () -> Unit) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_password, null)
        val input = view.findViewById<EditText>(R.id.passwordEditText)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.reconfigure_title)
            .setMessage(R.string.reconfigure_message)
            .setView(view)
            .setPositiveButton(R.string.continue_action, null)
            .setNegativeButton(android.R.string.cancel) { _, _ -> onCancel() }
            .setOnCancelListener { onCancel() }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (isPinValid(input.text.toString())) {
                    dialog.dismiss()
                    onConfirmed()
                } else {
                    input.error = activity.getString(R.string.incorrect_pin)
                    input.text.clear()
                }
            }
        }
        dialog.show()
    }
}
