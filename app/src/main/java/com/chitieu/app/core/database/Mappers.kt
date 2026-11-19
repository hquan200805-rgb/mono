package com.chitieu.app.core.database

import com.chitieu.app.core.model.Account
import com.chitieu.app.core.model.Category
import com.chitieu.app.core.model.Transaction

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    type = type,
    initialBalance = initialBalance,
    isArchived = isArchived
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type,
    initialBalance = initialBalance,
    isArchived = isArchived
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    icon = icon,
    colorHex = colorHex,
    type = type,
    isDefault = isDefault,
    sortOrder = sortOrder
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    colorHex = colorHex,
    type = type,
    isDefault = isDefault,
    sortOrder = sortOrder
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amount = amount,
    type = type,
    note = note,
    merchant = merchant,
    occurredAt = occurredAt,
    source = source,
    recurringId = recurringId,
    transferId = transferId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amount = amount,
    type = type,
    note = note,
    merchant = merchant,
    occurredAt = occurredAt,
    source = source,
    recurringId = recurringId,
    transferId = transferId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BudgetEntity.toDomain() = com.chitieu.app.core.model.Budget(
    id = id,
    categoryId = categoryId,
    month = month,
    limitAmount = limitAmount
)

fun com.chitieu.app.core.model.Budget.toEntity() = BudgetEntity(
    id = id,
    categoryId = categoryId,
    month = month,
    limitAmount = limitAmount
)


fun PendingTransactionEntity.toDomain() = com.chitieu.app.core.model.PendingTransaction(
    id = id,
    amount = amount,
    type = type,
    descriptionShort = descriptionShort,
    suggestedCategoryId = suggestedCategoryId,
    detectedAt = detectedAt,
    sourcePackage = sourcePackage,
    status = com.chitieu.app.core.model.PendingStatus.valueOf(status)
)

fun com.chitieu.app.core.model.PendingTransaction.toEntity() = PendingTransactionEntity(
    id = id,
    amount = amount,
    type = type,
    descriptionShort = descriptionShort,
    suggestedCategoryId = suggestedCategoryId,
    detectedAt = detectedAt,
    sourcePackage = sourcePackage,
    status = status.name
)


fun RecurringRuleEntity.toDomain() = com.chitieu.app.core.model.RecurringRule(
    id = id,
    merchantKey = merchantKey,
    categoryId = categoryId,
    expectedAmount = expectedAmount,
    periodDays = periodDays,
    nextDueDate = nextDueDate,
    isConfirmed = isConfirmed
)

fun com.chitieu.app.core.model.RecurringRule.toEntity() = RecurringRuleEntity(
    id = id,
    merchantKey = merchantKey,
    categoryId = categoryId,
    expectedAmount = expectedAmount,
    periodDays = periodDays,
    nextDueDate = nextDueDate,
    isConfirmed = isConfirmed
)
