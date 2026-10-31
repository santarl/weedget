package io.rfsj.tmr;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class ListWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int w : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.w_list);
            v.setRemoteAdapter(R.id.list, new Intent(c, ListService.class));
            v.setPendingIntentTemplate(R.id.list, PendingIntent.getActivity(c, 0,
                    new Intent(c, TapActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
            v.setOnClickPendingIntent(R.id.hdr, PendingIntent.getActivity(c, 1,
                    new Intent(c, MainActivity.class), Engine.FI));
            m.updateAppWidget(w, v);
        }
    }
}
