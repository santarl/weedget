package io.rfsj.weedget;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

/** Timer list + editor. With an appWidgetId extra it acts as the widget picker. */
public class MainActivity extends Activity {
    int wid = AppWidgetManager.INVALID_APPWIDGET_ID;
    Spinner styleSp;
    LinearLayout list;
    float d;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;
        wid = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
        if (wid != AppWidgetManager.INVALID_APPWIDGET_ID)
            setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid));
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);

        int p = (int) (16 * d);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        sv.addView(root);
        setContentView(sv);
        // targetSdk 35 draws edge to edge: keep the content clear of the system bars
        sv.setOnApplyWindowInsetsListener((v, i) -> {
            v.setPadding(i.getSystemWindowInsetLeft(), i.getSystemWindowInsetTop(),
                    i.getSystemWindowInsetRight(), i.getSystemWindowInsetBottom());
            return i;
        });

        TextView h = new TextView(this);
        h.setText(wid != AppWidgetManager.INVALID_APPWIDGET_ID ? "Pick a timer for this widget" : "Timers");
        h.setTextSize(22);
        root.addView(h);
        if (wid == AppWidgetManager.INVALID_APPWIDGET_ID) {
            TextView hint = new TextView(this);
            hint.setText("Tap = edit, long-press = start / reset");
            hint.setPadding(0, 0, 0, p);
            root.addView(hint);
        } else {
            styleSp = new Spinner(this);
            styleSp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"Ring", "Pie", "Vertical bar", "Fill"}));
            styleSp.setSelection(Store.style(this, wid));
            root.addView(styleSp);
        }
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        Button add = new Button(this);
        add.setText("+ New timer");
        add.setOnClickListener(v -> edit(null));
        root.addView(add);
    }

    @Override protected void onResume() { super.onResume(); build(); }

    void build() {
        list.removeAllViews();
        long now = System.currentTimeMillis();
        int p = (int) (14 * d);
        for (final Item it : Store.all(this)) {
            int st = it.state(now);
            TextView r = new TextView(this);
            r.setText(it.name + "\n" + (it.mode == Item.DOWN ? "Count down " + Item.fmt(it.dur)
                    : "Count up (alert at " + Item.fmt(it.dur) + ")")
                    + (st == Item.RUN ? " - running" : st == Item.DONE ? " - done" : ""));
            r.setTextSize(16);
            r.setPadding(p, p, p, p);
            r.setBackgroundColor(0x22FFFFFF);
            r.setOnClickListener(v -> {
                if (wid != AppWidgetManager.INVALID_APPWIDGET_ID) pick(it); else edit(it);
            });
            r.setOnLongClickListener(v -> {
                startActivity(new Intent(this, TapActivity.class).putExtra("id", it.id));
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = (int) (8 * d);
            list.addView(r, lp);
        }
    }

    void pick(Item t) {
        Store.bind(this, wid, t.id, styleSp.getSelectedItemPosition());
        AppWidgetManager.getInstance(this).updateAppWidget(wid, Widgets.render(this, wid));
        Engine.schedule(this);
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid));
        finish();
    }

    void edit(Item old) {
        final boolean isNew = old == null;
        final Item t = isNew ? new Item() : old;
        if (isNew) t.id = Store.nextId(this);

        int p = (int) (20 * d);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(p, p, p, 0);

        final EditText nm = new EditText(this);
        nm.setHint("Name"); nm.setText(t.name); nm.setSingleLine();
        final EditText du = new EditText(this);
        du.setHint("Duration: 5h, 1h30m, 45s, 90 (minutes)"); du.setText(Item.fmtIn(t.dur)); du.setSingleLine();
        final Spinner md = new Spinner(this);
        md.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Count down", "Count up forever (alert at duration)"}));
        md.setSelection(t.mode);
        final CheckBox nt = new CheckBox(this);
        nt.setText("Notify"); nt.setChecked(t.notify);

        final int[] cs = {0xFFFB8C00, 0xFFE53935, 0xFFAB47BC, 0xFF1E88E5, 0xFF00ACC1, 0xFFFDD835};
        final int[] sel = {t.color};
        final LinearLayout cr = new LinearLayout(this);
        for (int c : cs) {
            View s = new View(this);
            s.setBackgroundColor(c);
            s.setAlpha(c == sel[0] ? 1f : .35f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (36 * d), (int) (36 * d));
            lp.setMargins(0, (int) (8 * d), (int) (8 * d), (int) (8 * d));
            s.setOnClickListener(v -> {
                sel[0] = c;
                for (int i = 0; i < cr.getChildCount(); i++) cr.getChildAt(i).setAlpha(cs[i] == c ? 1f : .35f);
            });
            cr.addView(s, lp);
        }

        Button as = new Button(this);
        as.setText("Alert sound & vibration...");
        as.setOnClickListener(v -> {
            String n = nm.getText().toString().trim();
            t.name = n.isEmpty() ? "Timer" : n;
            Engine.channel(this, t);
            startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, "t" + t.id));
        });

        l.addView(nm); l.addView(du); l.addView(md); l.addView(nt); l.addView(cr); l.addView(as);

        AlertDialog.Builder ab = new AlertDialog.Builder(this)
                .setTitle(isNew ? "New timer" : "Edit timer").setView(l)
                .setPositiveButton("Save", (dl, which) -> {
                    t.name = nm.getText().toString().trim();
                    if (t.name.isEmpty()) t.name = "Timer";
                    long ms = Item.parse(du.getText().toString());
                    t.dur = Math.max(1000, ms == 0 ? t.dur : ms);
                    t.mode = md.getSelectedItemPosition();
                    t.notify = nt.isChecked();
                    t.color = sel[0];
                    Engine.save(this, t);
                    if (wid != AppWidgetManager.INVALID_APPWIDGET_ID) pick(t); else build();
                })
                .setNegativeButton("Cancel", null);
        if (!isNew) ab.setNeutralButton("Delete", (dl, which) -> { Engine.delete(this, t.id); build(); });
        ab.show();
    }
}
