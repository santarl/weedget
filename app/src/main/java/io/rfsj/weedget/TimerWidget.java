package io.rfsj.weedget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public class TimerWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int w : ids) m.updateAppWidget(w, Widgets.render(c, w));
        Engine.schedule(c);
    }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) {
        m.updateAppWidget(id, Widgets.render(c, id)); // font size follows the tile size
    }

    @Override public void onDeleted(Context c, int[] ids) {
        for (int w : ids) Store.unbind(c, w);
    }
}
