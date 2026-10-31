package io.rfsj.weedget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Per-widget look. Colours are RGB, opacity is 0..100 per part. */
final class Cfg {
    static final String[] STYLES = {"Ring", "Pie", "Vertical bar", "Horizontal bar", "Fill", "Gauge", "Segments", "Text only"};
    static final String[] PARTS = {"Background", "Track", "Progress", "Text"};
    static final int RING = 0, PIE = 1, VBAR = 2, HBAR = 3, FILL = 4, GAUGE = 5, SEGS = 6, TEXT = 7;
    static final int BG = 0, TRACK = 1, PROG = 2, TXT = 3;
    /** Colour used by "auto". Progress is auto = the timer's state colour. */
    static final int[] DEF = {0x1C1C20, 0xFFFFFF, 0, 0xFFFFFF};

    int style, tsize, line = 9, radius = 22; // tsize in sp, 0 = auto; line/radius in % of the tile
    final boolean[] auto = {true, true, true, true};
    final int[] col = {0x1C1C20, 0xFFFFFF, 0xFB8C00, 0xFFFFFF};
    final int[] alpha = {90, 20, 100, 100};

    int argb(int part, int stateCol) {
        int rgb = auto[part] ? (part == PROG ? stateCol : DEF[part]) : col[part];
        return (alpha[part] * 255 / 100) << 24 | (rgb & 0xFFFFFF);
    }

    String json() {
        try {
            JSONObject o = new JSONObject().put("s", style).put("z", tsize).put("l", line).put("r", radius);
            JSONArray a = new JSONArray(), c = new JSONArray(), t = new JSONArray();
            for (int i = 0; i < 4; i++) { a.put(auto[i]); c.put(col[i]); t.put(alpha[i]); }
            return o.put("a", a).put("c", c).put("t", t).toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    static Cfg from(String s, int styleFallback) {
        Cfg k = new Cfg();
        k.style = styleFallback;
        if (s != null) {
            try {
                JSONObject o = new JSONObject(s);
                k.style = o.optInt("s", k.style);
                k.tsize = o.optInt("z");
                k.line = o.optInt("l", k.line);
                k.radius = o.optInt("r", k.radius);
                JSONArray a = o.optJSONArray("a"), c = o.optJSONArray("c"), t = o.optJSONArray("t");
                for (int i = 0; i < 4; i++) {
                    if (a != null) k.auto[i] = a.optBoolean(i, true);
                    if (c != null) k.col[i] = c.optInt(i, k.col[i]);
                    if (t != null) k.alpha[i] = t.optInt(i, k.alpha[i]);
                }
            } catch (JSONException e) { }
        }
        k.style = Math.max(0, Math.min(STYLES.length - 1, k.style));
        return k;
    }
}
