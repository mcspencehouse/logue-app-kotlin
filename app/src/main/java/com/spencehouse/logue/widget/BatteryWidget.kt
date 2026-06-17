package com.spencehouse.logue.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.spencehouse.logue.service.SessionManager

import androidx.glance.GlanceTheme
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.spencehouse.logue.R

class BatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val sessionManager = SessionManager(context)
            val battery = sessionManager.cachedBatteryPercentage
            val range = sessionManager.cachedRange
            val chargeStatus = sessionManager.cachedChargeStatus
            val isPluggedIn = sessionManager.cachedIsPluggedIn
            val targetLimit = sessionManager.targetChargeLevel
            val voltage = sessionManager.cachedVoltage
            val batteryText = if (battery >= 0) "$battery%" else "--%"
            val rangeText = if (range >= 0) "$range miles" else "-- miles"
            val statusText = when {
                (isPluggedIn) && (voltage > 0) -> "$chargeStatus ${voltage}V"
                isPluggedIn -> "Plugged In"
                else -> "Unplugged"
            }

            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EV BATTERY",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Image(
                            provider = ImageProvider(android.R.drawable.stat_notify_sync),
                            contentDescription = "Refresh",
                            modifier = GlanceModifier.size(24.dp).clickable(
                                onClick = actionRunCallback<UpdateActionCallback>()
                            ),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                        )
                    }
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = GlanceModifier.size(100.dp).padding(top = 8.dp)
                        ) {
                            Image(
                                provider = ImageProvider(createProgressBitmap(battery, targetLimit, context)),
                                contentDescription = "Battery Progress",
                                modifier = GlanceModifier.fillMaxSize()
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isPluggedIn) {
                                    Image(
                                        provider = ImageProvider(R.drawable.ic_bolt),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                                        modifier = GlanceModifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = batteryText,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = GlanceModifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            InfoColumn("Range", rangeText)
                            InfoColumn("Status", statusText, isPluggedIn)
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun RowScope.InfoColumn(title: String, subtitle: String, isPluggedIn: Boolean = false) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            val statusColor = if (isPluggedIn) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface
            Text(
                text = subtitle,
                style = TextStyle(
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryWidget()
}

fun createProgressBitmap(progress: Int, target: Int, context: Context): android.graphics.Bitmap {
    val size = 300
    val maxStroke = 34f
    val strokeWidth = 26f
    val bitmap = createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val isDarkTheme = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    
    // Default Material 3 colors
    var primaryColor = if (isDarkTheme) "#D0BCFF".toColorInt() else "#6750A4".toColorInt()
    var surfaceVariantColor = if (isDarkTheme) "#49454F".toColorInt() else "#E7E0EC".toColorInt()
    var onSurfaceColor = if (isDarkTheme) "#E6E1E5".toColorInt() else "#1C1B1F".toColorInt()

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val primaryRes = if (isDarkTheme) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
        val surfaceVarRes = if (isDarkTheme) android.R.color.system_neutral2_700 else android.R.color.system_neutral2_200
        val onSurfaceRes = if (isDarkTheme) android.R.color.system_neutral1_100 else android.R.color.system_neutral1_900
        primaryColor = context.resources.getColor(primaryRes, context.theme)
        surfaceVariantColor = context.resources.getColor(surfaceVarRes, context.theme)
        onSurfaceColor = context.resources.getColor(onSurfaceRes, context.theme)
    }

    val backgroundPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        color = surfaceVariantColor
    }

    val progressPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = android.graphics.Paint.Cap.ROUND
        color = primaryColor
    }

    val targetPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = maxStroke // Slightly thicker to stand out
        strokeCap = android.graphics.Paint.Cap.ROUND
        color = onSurfaceColor
    }

    val padding = maxStroke / 2f + 2f
    val rect = android.graphics.RectF(padding, padding, size - padding, size - padding)

    // Draw background ring
    canvas.drawArc(rect, 0f, 360f, false, backgroundPaint)
    
    // Draw progress arc
    val safeProgress = progress.coerceIn(0, 100)
    val sweepAngle = 360f * (safeProgress / 100f)
    if (safeProgress > 0) {
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
    }

    // Draw target marker
    if (target in 1..100) {
        val targetAngle = -90f + (360f * (target / 100f))
        canvas.drawArc(rect, targetAngle - 1f, 2f, false, targetPaint)
    }

    return bitmap
}
