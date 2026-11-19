package com.chitieu.app.core.backup

/**
 * Schema version cho file sao lưu.
 * Tăng khi thay đổi cấu trúc JSON không tương thích.
 */
const val BACKUP_SCHEMA_VERSION = 1

data class BackupMeta(
    val schemaVersion: Int,
    val exportedAt: Long,
    val appVersionName: String,
    val transactionCount: Int,
    val accountCount: Int,
    val categoryCount: Int,
    val minOccurredAt: Long?,
    val maxOccurredAt: Long?
)

data class BackupPreview(
    val meta: BackupMeta,
    val compatible: Boolean,
    val message: String
)
