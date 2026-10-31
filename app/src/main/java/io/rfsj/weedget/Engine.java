package io.rfsj.weedget;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

/** Timer actions, alarms and notifications. Drawing lives in Widgets. */
final class Engine {
    static final int FI = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

    static PendingIntent bc(Context c, String a, int id) {
        return PendingIntent.getBroadcast(c, id,
                new Intent(c, Receiver.class).setAction(a).putExtra("id", id), FI);
    }

    static PendingIntent tap(Context c, int id, int wid) {
        return PendingIntent.getActivity(c, wid, new Intent(c, TapActivity.class)
                .putExtra("id", id).putExtra("wid", wid).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FI);
    }

    // ---- actions ----
    static void start(Context c, int id) {
        Item it = Store.get(c, id);
        if (it == null || it.start != 0) return;
        it.start = System.currentTimeMillis();
        it.alerted = false;
        Store.put(c, it);
        schedule(c); Widgets.refresh(c);
    }

    static void reset(Context c, int id) {
        Item it = Store.get(c, id);
        if (it == null) return;
        it.start = 0; it.alerted = false;
        Store.put(c, it);
        c.getSystemService(NotificationManager.class).cancel(id);
        schedule(c); Widgets.refresh(c);
    }

    /** Reset and start again right away, so the widget needs no extra tap. */
    static void restart(Context c, int id) {
        Item it = Store.get(c, id);
        if (it == null) return;
        it.start = System.currentTimeMillis();
        it.alerted = false;
        Store.put(c, it);
        c.getSystemService(NotificationManager.class).cancel(id);
        schedule(c); Widgets.refresh(c);
    }

    static void done(Context c, int id) {
        Item it = Store.get(c, id);
        long now = System.currentTimeMillis();
        if (it != null && it.start != 0 && !it.alerted && now >= it.start + it.dur - 5000) {
            it.alerted = true;
            Store.put(c, it);
            if (it.notify) notifyDone(c, it);
        }
        schedule(c); Widgets.refresh(c);
    }

    static void save(Context c, Item it) {
        long now = System.currentTimeMillis();
        if (it.start != 0 && now < it.start + it.dur) it.alerted = false;
        Store.put(c, it);
        channel(c, it);
        schedule(c); Widgets.refresh(c);
    }

    static void delete(Context c, int id) {
        Store.del(c, id);
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(bc(c, "DONE", id));
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.cancel(id);
        nm.deleteNotificationChannel("t" + id);
        schedule(c); Widgets.refresh(c);
    }

    // ---- alarms ----
    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        long now = System.currentTimeMillis(), iv = Long.MAX_VALUE;
        for (Item it : Store.all(c)) {
            PendingIntent pi = bc(c, "DONE", it.id);
            boolean want = it.start != 0 && !it.alerted && (it.mode == Item.DOWN || it.notify);
            if (want) {
                long at = it.start + it.dur;
                if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms())
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                else
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else am.cancel(pi);
            if (it.state(now) == Item.RUN) iv = Math.min(iv, Math.max(30000, it.dur / 100));
        }
        // non-waking redraw tick for the progress shapes (the numbers tick by themselves)
        PendingIntent t = bc(c, "TICK", 0);
        if (iv == Long.MAX_VALUE) am.cancel(t);
        else am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + iv, t);
    }

    // ---- notifications ----
    static void channel(Context c, Item it) {
        c.getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel("t" + it.id, it.name, NotificationManager.IMPORTANCE_HIGH));
    }

    static void notifyDone(Context c, Item it) {
        channel(c, it);
        Notification n = new Notification.Builder(c, "t" + it.id)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(it.name)
                .setContentText(it.mode == Item.DOWN ? "Time's up" : "Reached " + Item.fmt(it.dur))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), FI))
                .build();
        c.getSystemService(NotificationManager.class).notify(it.id, n);
    }
}
