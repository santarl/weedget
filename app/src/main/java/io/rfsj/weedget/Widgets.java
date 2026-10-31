package io.rfsj.weedget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

/** Builds RemoteViews for the icon widgets and refreshes every widget. */
final class Widgets {
    static final int IDLE_COL = 0xFF607D8B, DONE_COL = 0xFF43A047;

    static float prog(Item it, long now) {
        if (it.start == 0) return 0;
        long el = now - it.start;
        return it.mode == Item.DOWN ? Math.min(1f, el / (float) it.dur) : (el % it.dur) / (float) it.dur;
    }

    static int col(Item it, int st) {
        return st == Item.IDLE ? IDLE_COL : st == Item.DONE ? DONE_COL : it.color;
    }

    /** Live number while running (a Chronometer), static text otherwise. */
    static void time(RemoteViews v, Item it, long now) {
        int st = it.state(now);
        long el = now - it.start;
        if (st == Item.RUN) {
            long base = SystemClock.elapsedRealtime() + (it.mode == Item.DOWN ? it.dur - el : -el);
            v.setViewVisibility(R.id.chr, View.VISIBLE);
            v.setViewVisibility(R.id.txt, View.GONE);
            v.setChronometer(R.id.chr, base, null, true);
            v.setChronometerCountDown(R.id.chr, it.mode == Item.DOWN);
        } else {
            v.setViewVisibility(R.id.chr, View.GONE);
            v.setViewVisibility(R.id.txt, View.VISIBLE);
            v.setTextViewText(R.id.txt, st == Item.DONE ? "DONE" : it.mode == Item.DOWN ? Item.fmt(it.dur) : "0:00");
        }
    }

    static RemoteViews render(Context c, int wid) {
        return render(c, wid, Store.cfg(c, wid), Store.get(c, Store.tid(c, wid)), false);
    }

    /** preview = for the look editor: no click handlers. */
    static RemoteViews render(Context c, int wid, Cfg k, Item it, boolean preview) {
        int layout = k.style == Cfg.VBAR ? R.layout.w_side : k.style == Cfg.HBAR ? R.layout.w_top : R.layout.w_center;
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        int tc = k.argb(Cfg.TXT, 0);
        for (int id : new int[]{R.id.chr, R.id.txt}) {
            v.setTextColor(id, tc);
            if (k.tsize > 0) v.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, Math.max(6, k.tsize));
        }
        if (it == null) {
            v.setImageViewBitmap(R.id.img, Painter.draw(k, 0, IDLE_COL));
            v.setViewVisibility(R.id.chr, View.GONE);
            v.setViewVisibility(R.id.txt, View.VISIBLE);
            v.setTextViewText(R.id.txt, "?");
            if (!preview) v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, wid,
                    new Intent(c, MainActivity.class)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), Engine.FI));
            return v;
        }
        long now = System.currentTimeMillis();
        int st = it.state(now);
        v.setImageViewBitmap(R.id.img, Painter.draw(k, st == Item.DONE ? 1 : prog(it, now), col(it, st)));
        time(v, it, now);
        if (!preview) v.setOnClickPendingIntent(R.id.root,
                st == Item.IDLE ? Engine.bc(c, "START", it.id) : Engine.tap(c, it.id, wid));
        return v;
    }

    static void update(Context c, int wid) {
        AppWidgetManager.getInstance(c).updateAppWidget(wid, render(c, wid));
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int w : m.getAppWidgetIds(new ComponentName(c, TimerWidget.class)))
            m.updateAppWidget(w, render(c, w));
        m.notifyAppWidgetViewDataChanged(m.getAppWidgetIds(new ComponentName(c, ListWidget.class)), R.id.list);
    }
}
