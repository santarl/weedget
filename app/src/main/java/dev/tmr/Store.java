package dev.tmr;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

final class Store {
    static SharedPreferences p(Context c) { return c.getApplicationContext().getSharedPreferences("s", 0); }

    static synchronized List<Item> all(Context c) {
        List<Item> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p(c).getString("t", "[]"));
            for (int i = 0; i < a.length(); i++) l.add(Item.from(a.getJSONObject(i)));
        } catch (JSONException e) { }
        return l;
    }

    static Item get(Context c, int id) {
        for (Item i : all(c)) if (i.id == id) return i;
        return null;
    }

    static synchronized void put(Context c, Item it) {
        List<Item> l = all(c);
        boolean f = false;
        for (int i = 0; i < l.size(); i++) if (l.get(i).id == it.id) { l.set(i, it); f = true; }
        if (!f) l.add(it);
        save(c, l);
    }

    static synchronized void del(Context c, int id) {
        List<Item> l = all(c);
        l.removeIf(i -> i.id == id);
        save(c, l);
    }

    private static void save(Context c, List<Item> l) {
        JSONArray a = new JSONArray();
        try { for (Item i : l) a.put(i.json()); } catch (JSONException e) { }
        p(c).edit().putString("t", a.toString()).apply();
    }

    static synchronized int nextId(Context c) {
        int n = p(c).getInt("n", 1);
        p(c).edit().putInt("n", n + 1).apply();
        return n;
    }

    static void bind(Context c, int w, int t, int s) { p(c).edit().putInt("t" + w, t).putInt("s" + w, s).apply(); }
    static void unbind(Context c, int w) { p(c).edit().remove("t" + w).remove("s" + w).apply(); }
    static int tid(Context c, int w) { return p(c).getInt("t" + w, 0); }
    static int style(Context c, int w) { return p(c).getInt("s" + w, 0); }
}
