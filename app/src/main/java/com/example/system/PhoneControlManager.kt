package com.example.system

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.domain.InstalledAppInfo

class PhoneControlManager(private val context: Context) {

    fun hasCallPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun isNotificationAccessEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val meenaComponent = ComponentName(context, MeenaNotificationListenerService::class.java).flattenToString()
        return flat.split(":").any { it.equals(meenaComponent, ignoreCase = true) }
    }

    fun getInstalledLauncherApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = resolveInfo.loadLabel(pm)?.toString().orEmpty()
                if (label.isBlank()) return@mapNotNull null
                InstalledAppInfo(label = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String): Result<String> {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return Result.failure(IllegalStateException("App cannot be launched"))
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Result.success("Opened app")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun launchAppByName(query: String): Result<String> {
        val normalized = query.trim().lowercase()
        val match = getInstalledLauncherApps().firstOrNull {
            it.label.lowercase().contains(normalized)
        } ?: return Result.failure(IllegalStateException("No installed app matching '$query'"))
        return launchApp(match.packageName).map { "Opened ${match.label}" }
    }

    fun placeCall(phoneNumber: String): Result<String> {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Enter a phone number"))
        return try {
            val action = if (hasCallPermission()) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, Uri.parse("tel:${Uri.encode(clean)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(if (hasCallPermission()) "Calling $clean" else "Opened dialer for $clean")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendSms(phoneNumber: String, message: String): Result<String> {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Enter a recipient number"))
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(clean)}")).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened SMS composer for $clean")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "Meena Reminder" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Alarm scheduled for %02d:%02d".format(hour, minute))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openCamera(): Result<String> {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened camera")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun searchWeb(query: String): Result<String> {
        return try {
            val encoded = Uri.encode(query.trim())
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=$encoded")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Searching web for $query")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openSystemSetting(action: String): Result<String> {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened system settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openNotificationListenerSettings(): Result<String> =
        openSystemSetting(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun setFlashlight(enabled: Boolean): Result<Boolean> {
        return try {
            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = camManager.cameraIdList.firstOrNull { id ->
                val chars = camManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.failure(IllegalStateException("No camera flash available on this device"))
            camManager.setTorchMode(cameraId, enabled)
            Result.success(enabled)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMediaVolumeFraction(): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return current.toFloat() / max.toFloat()
    }

    fun setMediaVolumeFraction(fraction: Float) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (fraction.coerceIn(0f, 1f) * max).toInt()
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }
}
