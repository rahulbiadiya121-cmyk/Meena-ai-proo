package com.example.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.domain.CapturedNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeenaNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        _isListenerConnected.value = false
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshActiveNotifications()
    }

    private fun refreshActiveNotifications() {
        try {
            val list = activeNotifications?.mapNotNull { sbn ->
                val extras = sbn.notification?.extras ?: return@mapNotNull null
                val title = extras.getCharSequence("android.title")?.toString().orEmpty()
                val text = extras.getCharSequence("android.text")?.toString().orEmpty()
                if (title.isBlank() && text.isBlank()) return@mapNotNull null
                CapturedNotification(
                    id = sbn.key,
                    appName = sbn.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    title = title,
                    text = text,
                    timestamp = sbn.postTime
                )
            }.orEmpty().sortedByDescending { it.timestamp }.take(20)
            _notifications.value = list
        } catch (_: Exception) {
            // Listener permission might be revoked asynchronously
        }
    }

    companion object {
        private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
        val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()
    }
}
