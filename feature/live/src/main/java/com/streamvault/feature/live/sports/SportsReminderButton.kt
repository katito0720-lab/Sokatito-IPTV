package com.streamvault.feature.live.sports

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.streamvault.core.ui.interaction.TvButton
import com.streamvault.core.ui.platform.NotificationPermissionGate
import com.streamvault.domain.model.Result
import com.streamvault.feature.live.R
import kotlinx.coroutines.launch

@Composable
internal fun SportsReminderButton(
    target: SportsReminderTarget,
    permissionGate: NotificationPermissionGate,
    isReminderScheduled: suspend (SportsReminderTarget) -> Boolean,
    setReminder: suspend (SportsReminderTarget, Boolean) -> Result<Unit>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(R.string.live_source_unavailable_short)
    var scheduled by remember(target) { mutableStateOf<Boolean?>(null) }
    var saving by remember(target) { mutableStateOf(false) }
    LaunchedEffect(target) { scheduled = isReminderScheduled(target) }
    val label = stringResource(when {
        saving || scheduled == null -> R.string.epg_program_reminder_loading
        scheduled == true -> R.string.epg_program_reminder_cancel
        else -> R.string.epg_program_reminder_set
    })
    TvButton(
        enabled = !saving && scheduled != null,
        onClick = {
            val enable = scheduled == false
            val update: () -> Unit = {
                if (!saving) {
                    saving = true
                    scope.launch {
                        try {
                            when (setReminder(target, enable)) {
                                is Result.Success -> scheduled = enable
                                else -> Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            saving = false
                        }
                    }
                }
            }
            if (enable) permissionGate.runReminderAction(update) else update()
        },
    ) {
        Text(label, maxLines = 1)
    }
}
