package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.SyncGreenBg
import com.example.ui.theme.SyncOrange
import com.example.ui.theme.SyncOrangeBg
import com.example.ui.theme.SyncRed
import com.example.ui.theme.SyncRedBg

enum class SyncStatus(val label: String, val badgeColor: Color, val badgeBgColor: Color) {
    SYNCED("Synced", SyncGreen, SyncGreenBg),
    PENDING("Saved locally (waiting for internet)", SyncOrange, SyncOrangeBg),
    ERROR("Sync needs attention", SyncRed, SyncRedBg);

    val shortLabel: String
        get() = when (this) {
            SYNCED -> "Synced"
            PENDING -> "Offline (Pending)"
            ERROR -> "Needs Attention"
        }
}
