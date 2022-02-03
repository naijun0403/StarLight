package dev.mooner.starlight.plugincore.chat

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.mooner.starlight.plugincore.logger.Logger

data class ChatRoomImpl(
    override val name: String,
    override val isGroupChat: Boolean,
    override val isDebugRoom: Boolean = false,
    val session: Notification.Action,
    private val context: Context
): ChatRoom {
    override fun send(message: String): Boolean {
        return try {
            val sendIntent = Intent()
            val msg = Bundle()
            for (input in session.remoteInputs) msg.putCharSequence(
                input.resultKey,
                message
            )
            RemoteInput.addResultsToIntent(session.remoteInputs, sendIntent, msg)
            session.actionIntent.send(context, 0, sendIntent)
            Logger.v("ChatRoom", "send() success: $message")
            true
        } catch (e: PendingIntent.CanceledException) {
            false
        }
    }

    override fun markAsRead(): Boolean {
        try {
            session.actionIntent.send(context, 1, Intent())
        } catch (e: PendingIntent.CanceledException) {
            return false
        }
        return true
    }
}