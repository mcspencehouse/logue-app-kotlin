package com.spencehouse.logue.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.spencehouse.logue.service.SessionManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.glance.GlanceTheme
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import com.spencehouse.logue.R

class CommandsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "EV REMOTE COMMANDS",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(16.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CommandButton("Lock", R.drawable.ic_lock, LockAction::class.java, GlanceModifier.defaultWeight())
                        Spacer(modifier = GlanceModifier.width(12.dp))
                        CommandButton("Unlock", R.drawable.ic_lock_open, UnlockAction::class.java, GlanceModifier.defaultWeight())
                    }
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CommandButton("Lights", R.drawable.ic_lights, LightsAction::class.java, GlanceModifier.defaultWeight())
                        Spacer(modifier = GlanceModifier.width(12.dp))
                        CommandButton("Horn", R.drawable.ic_horn, HornAction::class.java, GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CommandButton(text: String, iconRes: Int, actionClass: Class<out ActionCallback>, modifier: GlanceModifier = GlanceModifier) {
        Box(
            modifier = modifier
                .background(GlanceTheme.colors.surface)
                .cornerRadius(12.dp)
                .padding(vertical = 8.dp, horizontal = 8.dp)
                .clickable(actionRunCallback(actionClass)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = text,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                    modifier = GlanceModifier.size(24.dp)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = text,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

class CommandsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CommandsWidget()
}
