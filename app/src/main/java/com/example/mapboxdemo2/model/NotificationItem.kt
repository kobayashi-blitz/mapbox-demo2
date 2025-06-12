package com.example.mapboxdemo2.model

enum class NotificationType {
    NEWS,    // 運営からのお知らせ
    MARKER   // マーカー関連
}

data class NotificationItem(
    val title: String,
    val message: String,
    val date: String,
    val type: NotificationType
)