package io.rfsj.weedget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
        return render(c, wid, Store.cfg(c, wid), Store.get(c, Store.tid(c, wid)), false, tileDp(c, wid));
    }

    /** Side of the (square) tile in dp, from what the launcher reports for this widget. */
    static float tileDp(Context c, int wid) {
        Bundle o = AppWidgetManager.getInstance(c).getAppWidgetOptions(wid);
        float w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        float h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        return Math.min(w > 0 ? w : 70, h > 0 ? h : 70);
    }

    /** Longest text the number can show until the next redraw, to size the font for it. */
    static String sample(Item it, long now) {
        int st = it.state(now);
        long el = now - it.start;
        if (st == Item.DONE) return "DONE";
        if (st == Item.IDLE) return it.mode == Item.DOWN ? Item.fmt(it.dur) : "0:00";
        return Item.fmt(it.mode == Item.DOWN ? it.dur - el : el + Math.max(30000, it.dur / 100));
    }

    /** Font size in dp that fits `text` into the free area of the tile for this style. */
    static float fitDp(Cfg k, float tileDp, String text) {
        float line = k.line / 100f, free;
        switch (k.style) {
            case Cfg.RING:
            case Cfg.GAUGE: free = 0.8f - 2 * line; break;
            case Cfg.SEGS: free = 2 * (0.42f - Math.max(0.08f, 1.5f * line)); break;
            case Cfg.VBAR: free = 0.58f; break;
            case Cfg.PIE: free = 0.7f; break;
            default: free = 0.8f; break;
        }
        float em = 0;
        for (char ch : text.toCharArray()) em += Character.isDigit(ch) ? 0.6f : ch == ':' ? 0.3f : 0.75f;
        return Math.max(7, Math.min(30, tileDp * Math.max(0.2f, free) * 0.9f / Math.max(em, 1)));
    }

    /** preview = for the look editor: no click handlers. tileDp = size the tile is shown at. */
    static RemoteViews render(Context c, int wid, Cfg k, Item it, boolean preview, float tileDp) {
        int layout = k.style == Cfg.VBAR ? R.layout.w_side : k.style == Cfg.HBAR ? R.layout.w_top : R.layout.w_center;
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        int tc = k.argb(Cfg.TXT, 0);
        String text = it == null ? "?" : sample(it, System.currentTimeMillis());
        for (int id : new int[]{R.id.chr, R.id.txt}) {
            v.setTextColor(id, tc);
            if (k.tsize > 0) v.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, Math.max(6, k.tsize));
            else v.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_DIP, fitDp(k, tileDp, text));
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
