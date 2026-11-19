package com.chitieu.app.core.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.util.Log
import com.chitieu.app.core.model.PendingTransaction
import com.chitieu.app.core.smart.CategoryClassifier
import com.chitieu.app.core.database.CategoryRepository
import com.chitieu.app.core.model.CategoryType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Chỉ đọc thông báo từ package trong whitelist.
 * Không lưu nội dung thông báo gốc — chỉ amount, type, mô tả ngắn.
 */
@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var pendingRepository: PendingTransactionRepository
    @Inject lateinit var categoryClassifier: CategoryClassifier
    @Inject lateinit var categoryRepository: CategoryRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName
        if (!SupportedPackages.isSupported(pkg)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        val parsed = NotificationParser.parse(title, text, bigText) ?: return

        scope.launch {
            try {
                val categories = categoryRepository.observeAll().first()
                    .filter {
                        if (parsed.type.name == "CHI") it.type == CategoryType.CHI
                        else it.type == CategoryType.THU
                    }
                val suggestion = categoryClassifier.suggest(
                    note = parsed.descriptionShort,
                    merchant = SupportedPackages.displayName(pkg),
                    categories = categories
                )

                val pending = PendingTransaction(
                    amount = parsed.amount,
                    type = parsed.type,
                    descriptionShort = parsed.descriptionShort,
                    suggestedCategoryId = suggestion.categoryId,
                    detectedAt = System.currentTimeMillis(),
                    sourcePackage = pkg
                )
                val id = pendingRepository.addIfNotDuplicate(pending)
                if (id != null) {
                    Log.d(TAG, "Pending created id=$id amount=${parsed.amount}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // không cần xử lý
    }

    companion object {
        private const val TAG = "BankNotifListener"
    }
}
