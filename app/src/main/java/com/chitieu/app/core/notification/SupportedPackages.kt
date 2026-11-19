package com.chitieu.app.core.notification

/**
 * Danh sách package ngân hàng / ví được hỗ trợ.
 * Chỉ parse thông báo từ các package này.
 */
object SupportedPackages {
    val packages: Set<String> = setOf(
        // Ví điện tử
        "com.mservice.momotransfer",      // MoMo
        "vn.com.vng.zalopay",              // ZaloPay
        "vn.zalopay.wallet",
        "com.vnpay.sv",                    // VNPay / một số bank app
        "com.vietinbank.ipay",
        "com.VCB",                         // Vietcombank (tùy bản)
        "com.vietcombank.dse",
        "vn.com.techcombank.bb.app",       // Techcombank
        "com.mbmobile",                    // MB Bank
        "com.mbmobile.enterprise",
        "com.tpb.mb",                      // TPBank
        "com.bidv.ibusiness",
        "com.vnpay.bidv",
        "com.viettel.bankplus",
        "com.shb.retail",
        "com.acb.banking",
        // Thêm package test / debug
        "com.chitieu.app"                  // cho phép test nội bộ nếu cần
    )

    fun isSupported(packageName: String?): Boolean =
        packageName != null && packageName in packages

    fun displayName(packageName: String): String = when {
        packageName.contains("momo", ignoreCase = true) -> "MoMo"
        packageName.contains("zalopay", ignoreCase = true) -> "ZaloPay"
        packageName.contains("vietcombank", ignoreCase = true) || packageName.contains("VCB") -> "Vietcombank"
        packageName.contains("techcombank", ignoreCase = true) -> "Techcombank"
        packageName.contains("mbmobile", ignoreCase = true) -> "MB Bank"
        packageName.contains("tpb", ignoreCase = true) -> "TPBank"
        packageName.contains("bidv", ignoreCase = true) -> "BIDV"
        else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
}
