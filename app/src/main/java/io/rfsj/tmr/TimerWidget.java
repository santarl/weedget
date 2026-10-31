package io.rfsj.tmr;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class TimerWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int w : ids) m.updateAppWidget(w, Widgets.render(c, w));
        Engine.schedule(c);
    }

    @Override public void onDeleted(Context c, int[] ids) {
        for (int w : ids) Store.unbind(c, w);
    }
}
