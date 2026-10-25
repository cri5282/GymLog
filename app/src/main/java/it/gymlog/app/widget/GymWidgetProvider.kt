package it.gymlog.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import it.gymlog.app.GymLogApp
import it.gymlog.app.MainActivity
import it.gymlog.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Widget della home: mostra l'allenamento in corso oppure la scheda di oggi.
 * Il contenuto viene letto dal database in background e aggiornato quando l'app torna in primo piano.
 */
class GymWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val container = (context.applicationContext as GymLogApp).container
        val pending = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val unfinished = runCatching { container.workouts.getUnfinished() }.getOrNull()
                val today = runCatching { container.programs.todayDay(LocalDate.now()) }.getOrNull()
                val count = today?.let { runCatching { container.programs.getDayExercises(it.id).size }.getOrDefault(0) } ?: 0
                val title = unfinished?.title ?: today?.name ?: "GymLog"
                val subtitle = when {
                    unfinished != null -> "Allenamento in corso · tocca per riprendere"
                    today != null -> "Oggi: $count esercizi · tocca per iniziare"
                    else -> "Nessun allenamento pianificato per oggi"
                }
                val views = RemoteViews(context.packageName, R.layout.widget_gym).apply {
                    setTextViewText(R.id.widgetTitle, title)
                    setTextViewText(R.id.widgetSubtitle, subtitle)
                    setOnClickPendingIntent(R.id.widgetContainer, pending)
                }
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        /** Aggiorna tutti i widget installati (chiamato quando l'app torna visibile). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GymWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, GymWidgetProvider::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
