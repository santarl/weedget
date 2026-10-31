package io.rfsj.tmr;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Item {
    static final int DOWN = 0, UP = 1, IDLE = 0, RUN = 1, DONE = 2;
    int id, mode = DOWN, color = 0xFFFB8C00;
    String name = "Timer";
    long dur = 5 * 3600000L, start;
    boolean notify = true, alerted;

    int state(long now) {
        if (start == 0) return IDLE;
        return mode == DOWN && now - start >= dur ? DONE : RUN;
    }

    static String fmt(long ms) {
        long s = Math.max(0, ms) / 1000, h = s / 3600, m = s % 3600 / 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s % 60)
                : String.format(Locale.ROOT, "%d:%02d", m, s % 60);
    }

    static String fmtIn(long ms) {
        long s = ms / 1000;
        StringBuilder b = new StringBuilder();
        if (s / 3600 > 0) b.append(s / 3600).append('h');
        if (s % 3600 / 60 > 0) b.append(s % 3600 / 60).append('m');
        if (s % 60 > 0) b.append(s % 60).append('s');
        return b.toString();
    }

    /** "5h", "1h30m", "45s", "90" (= minutes) */
    static long parse(String t) {
        Matcher m = Pattern.compile("(\\d+)\\s*([hms]?)").matcher(t.toLowerCase(Locale.ROOT));
        long ms = 0;
        while (m.find()) {
            try {
                long n = Long.parseLong(m.group(1));
                String u = m.group(2);
                ms += n * (u.equals("h") ? 3600000L : u.equals("s") ? 1000L : 60000L);
            } catch (NumberFormatException e) { }
        }
        return ms;
    }

    JSONObject json() throws JSONException {
        return new JSONObject().put("id", id).put("n", name).put("d", dur).put("m", mode)
                .put("c", color).put("y", notify).put("s", start).put("a", alerted);
    }

    static Item from(JSONObject o) {
        Item i = new Item();
        i.id = o.optInt("id"); i.name = o.optString("n", "Timer"); i.dur = o.optLong("d", i.dur);
        i.mode = o.optInt("m"); i.color = o.optInt("c", i.color); i.notify = o.optBoolean("y", true);
        i.start = o.optLong("s"); i.alerted = o.optBoolean("a");
        return i;
    }
}
