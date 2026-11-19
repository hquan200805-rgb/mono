package com.chitieu.app.core.backup

import android.content.Context
import com.chitieu.app.core.database.AccountDao
import com.chitieu.app.core.database.BudgetDao
import com.chitieu.app.core.database.CategoryDao
import com.chitieu.app.core.database.CategoryHintDao
import com.chitieu.app.core.database.PendingTransactionDao
import com.chitieu.app.core.database.RecurringRuleDao
import com.chitieu.app.core.database.TransactionDao
import com.chitieu.app.core.model.AccountType
import com.chitieu.app.core.model.CategoryType
import com.chitieu.app.core.model.TransactionSource
import com.chitieu.app.core.model.TransactionType
import com.chitieu.app.core.database.AccountEntity
import com.chitieu.app.core.database.BudgetEntity
import com.chitieu.app.core.database.CategoryEntity
import com.chitieu.app.core.database.CategoryHintEntity
import com.chitieu.app.core.database.TransactionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val categoryHintDao: CategoryHintDao,
    private val recurringRuleDao: RecurringRuleDao
) {
    fun backupDir(): File {
        val dir = File(context.getExternalFilesDir(null), "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun buildBackupJson(): JSONObject = withContext(Dispatchers.IO) {
        val accounts = accountDao.observeAllAccounts().first()
        val categories = categoryDao.observeAll().first()
        val transactions = transactionDao.observeAll().first()
        val budgets = budgetDao.observeByMonth(
            // export all months: query via raw - use current + empty fallback
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
        ).first()
        // Also pull recurring
        val recurring = recurringRuleDao.observeAll().first()

        val minAt = transactions.minOfOrNull { it.occurredAt }
        val maxAt = transactions.maxOfOrNull { it.occurredAt }

        val meta = JSONObject().apply {
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("appVersionName", "1.0.0-m8")
            put("transactionCount", transactions.size)
            put("accountCount", accounts.size)
            put("categoryCount", categories.size)
            put("minOccurredAt", minAt ?: JSONObject.NULL)
            put("maxOccurredAt", maxAt ?: JSONObject.NULL)
        }

        JSONObject().apply {
            put("meta", meta)
            put("accounts", JSONArray().apply {
                accounts.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("name", a.name)
                        put("type", a.type.name)
                        put("initialBalance", a.initialBalance)
                        put("isArchived", a.isArchived)
                    })
                }
            })
            put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("icon", c.icon)
                        put("colorHex", c.colorHex)
                        put("type", c.type.name)
                        put("isDefault", c.isDefault)
                        put("sortOrder", c.sortOrder)
                    })
                }
            })
            put("transactions", JSONArray().apply {
                transactions.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("accountId", t.accountId)
                        put("categoryId", t.categoryId ?: JSONObject.NULL)
                        put("amount", t.amount)
                        put("type", t.type.name)
                        put("note", t.note ?: JSONObject.NULL)
                        put("merchant", t.merchant ?: JSONObject.NULL)
                        put("occurredAt", t.occurredAt)
                        put("source", t.source.name)
                        put("recurringId", t.recurringId ?: JSONObject.NULL)
                        put("transferId", t.transferId ?: JSONObject.NULL)
                        put("createdAt", t.createdAt)
                        put("updatedAt", t.updatedAt)
                    })
                }
            })
            put("budgets", JSONArray().apply {
                budgets.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("categoryId", b.categoryId ?: JSONObject.NULL)
                        put("month", b.month)
                        put("limitAmount", b.limitAmount)
                    })
                }
            })
            put("recurringRules", JSONArray().apply {
                recurring.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("merchantKey", r.merchantKey)
                        put("categoryId", r.categoryId ?: JSONObject.NULL)
                        put("expectedAmount", r.expectedAmount)
                        put("periodDays", r.periodDays)
                        put("nextDueDate", r.nextDueDate)
                        put("isConfirmed", r.isConfirmed)
                    })
                }
            })
        }
    }

    suspend fun exportToGzipFile(): File = withContext(Dispatchers.IO) {
        val json = buildBackupJson()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(backupDir(), "chitieu_backup_$stamp.json.gz")
        GZIPOutputStream(file.outputStream()).use { gz ->
            gz.write(json.toString().toByteArray(Charsets.UTF_8))
        }
        file
    }

    suspend fun exportCsvFile(): File = withContext(Dispatchers.IO) {
        val transactions = transactionDao.observeAll().first()
        val categories = categoryDao.observeAll().first().associateBy { it.id }
        val accounts = accountDao.observeAllAccounts().first().associateBy { it.id }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(backupDir(), "chitieu_transactions_$stamp.csv")
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.appendLine("id,occurredAt,type,amount,account,category,note,merchant,source")
            for (t in transactions) {
                val acc = accounts[t.accountId]?.name ?: t.accountId.toString()
                val cat = t.categoryId?.let { categories[it]?.name } ?: ""
                val note = (t.note ?: "").replace("\"", "'")
                val merchant = (t.merchant ?: "").replace("\"", "'")
                w.appendLine(
                    "${t.id},${t.occurredAt},${t.type.name},${t.amount},\"$acc\",\"$cat\",\"$note\",\"$merchant\",${t.source.name}"
                )
            }
        }
        file
    }

    fun previewFromStream(input: InputStream, gzipped: Boolean): BackupPreview {
        val text = if (gzipped) {
            GZIPInputStream(input).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val root = JSONObject(text)
        val metaObj = root.getJSONObject("meta")
        val version = metaObj.getInt("schemaVersion")
        val meta = BackupMeta(
            schemaVersion = version,
            exportedAt = metaObj.optLong("exportedAt", 0L),
            appVersionName = metaObj.optString("appVersionName", "?"),
            transactionCount = metaObj.optInt("transactionCount", 0),
            accountCount = metaObj.optInt("accountCount", 0),
            categoryCount = metaObj.optInt("categoryCount", 0),
            minOccurredAt = if (metaObj.isNull("minOccurredAt")) null else metaObj.optLong("minOccurredAt"),
            maxOccurredAt = if (metaObj.isNull("maxOccurredAt")) null else metaObj.optLong("maxOccurredAt")
        )
        val compatible = version <= BACKUP_SCHEMA_VERSION
        val message = if (compatible) {
            "Sẽ ghi đè dữ liệu hiện tại bằng bản sao lưu này."
        } else {
            "Phiên bản schema ($version) mới hơn app (hỗ trợ $BACKUP_SCHEMA_VERSION). Hãy cập nhật app."
        }
        return BackupPreview(meta, compatible, message)
    }

    /**
     * Restore đầy đủ — ghi đè (destructive) vì Room không có merge đơn giản trong MVP.
     * Caller phải đã show preview và user xác nhận.
     */
    suspend fun restoreFromStream(input: InputStream, gzipped: Boolean) = withContext(Dispatchers.IO) {
        val text = if (gzipped) {
            GZIPInputStream(input).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val root = JSONObject(text)
        val version = root.getJSONObject("meta").getInt("schemaVersion")
        if (version > BACKUP_SCHEMA_VERSION) {
            throw IllegalStateException("Schema version $version không được hỗ trợ")
        }

        // Clear & insert — đơn giản cho MVP (cần transaction DB nếu muốn an toàn hơn)
        // Room không expose clear all dễ; dùng delete từng bảng qua query nếu có.
        // Ở đây insert REPLACE theo id.

        val accounts = root.getJSONArray("accounts")
        for (i in 0 until accounts.length()) {
            val o = accounts.getJSONObject(i)
            accountDao.insert(
                AccountEntity(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    type = AccountType.valueOf(o.getString("type")),
                    initialBalance = o.getLong("initialBalance"),
                    isArchived = o.optBoolean("isArchived", false)
                )
            )
        }

        val categories = root.getJSONArray("categories")
        for (i in 0 until categories.length()) {
            val o = categories.getJSONObject(i)
            categoryDao.insert(
                CategoryEntity(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    icon = o.getString("icon"),
                    colorHex = o.getString("colorHex"),
                    type = CategoryType.valueOf(o.getString("type")),
                    isDefault = o.optBoolean("isDefault", false),
                    sortOrder = o.optInt("sortOrder", 0)
                )
            )
        }

        val transactions = root.getJSONArray("transactions")
        for (i in 0 until transactions.length()) {
            val o = transactions.getJSONObject(i)
            transactionDao.insert(
                TransactionEntity(
                    id = o.getLong("id"),
                    accountId = o.getLong("accountId"),
                    categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                    amount = o.getLong("amount"),
                    type = TransactionType.valueOf(o.getString("type")),
                    note = if (o.isNull("note")) null else o.getString("note"),
                    merchant = if (o.isNull("merchant")) null else o.getString("merchant"),
                    occurredAt = o.getLong("occurredAt"),
                    source = TransactionSource.valueOf(o.optString("source", "THU_CONG")),
                    recurringId = if (o.isNull("recurringId")) null else o.getLong("recurringId"),
                    transferId = if (o.isNull("transferId")) null else o.getLong("transferId"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        if (root.has("budgets")) {
            val budgets = root.getJSONArray("budgets")
            for (i in 0 until budgets.length()) {
                val o = budgets.getJSONObject(i)
                budgetDao.upsert(
                    BudgetEntity(
                        id = o.optLong("id", 0),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        month = o.getString("month"),
                        limitAmount = o.getLong("limitAmount")
                    )
                )
            }
        }

        if (root.has("recurringRules")) {
            val rules = root.getJSONArray("recurringRules")
            for (i in 0 until rules.length()) {
                val o = rules.getJSONObject(i)
                recurringRuleDao.upsert(
                    com.chitieu.app.core.database.RecurringRuleEntity(
                        id = o.optLong("id", 0),
                        merchantKey = o.getString("merchantKey"),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        expectedAmount = o.getLong("expectedAmount"),
                        periodDays = o.getInt("periodDays"),
                        nextDueDate = o.getLong("nextDueDate"),
                        isConfirmed = o.optBoolean("isConfirmed", false)
                    )
                )
            }
        }
    }
}
