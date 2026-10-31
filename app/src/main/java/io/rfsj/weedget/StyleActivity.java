package io.rfsj.weedget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Look editor for one widget, with a live preview. Changes apply to that widget straight away. */
public class StyleActivity extends Activity {
    int wid, prev = 40;
    Cfg cfg;
    FrameLayout box;
    float d;

    int dp(float v) { return (int) (v * d + .5f); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;
        wid = getIntent().getIntExtra("wid", AppWidgetManager.INVALID_APPWIDGET_ID);
        if (wid == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        cfg = Store.cfg(this, wid);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setOnApplyWindowInsetsListener((v, i) -> {
            v.setPadding(i.getSystemWindowInsetLeft(), i.getSystemWindowInsetTop(),
                    i.getSystemWindowInsetRight(), i.getSystemWindowInsetBottom());
            return i;
        });
        setContentView(root);

        box = new FrameLayout(this);
        box.setBackgroundColor(0x33808080); // neutral backdrop so a translucent tile is visible
        root.addView(box, new LinearLayout.LayoutParams(-1, dp(150)));
        preview();

        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(8), dp(16), dp(16));
        sv.addView(col);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        IntFunction<String> pct = n -> n + "%";
        slider(col, "Preview progress", 0, 100, prev, pct, n -> prev = n);

        TextView st = new TextView(this);
        st.setText("Style");
        st.setTextSize(18);
        st.setPadding(0, dp(12), 0, 0);
        col.addView(st);
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Cfg.STYLES));
        sp.setSelection(cfg.style);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos != cfg.style) { cfg.style = pos; preview(); save(); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        col.addView(sp);

        slider(col, "Font size", 0, 40, cfg.tsize, n -> n == 0 ? "auto" : n + " sp", n -> cfg.tsize = n);
        slider(col, "Line width", 2, 30, cfg.line, pct, n -> cfg.line = n);
        slider(col, "Corner radius", 0, 50, cfg.radius, pct, n -> cfg.radius = n);

        for (int part = 0; part < 4; part++) {
            final int e = part;
            TextView h = new TextView(this);
            h.setText(Cfg.PARTS[e]);
            h.setTextSize(18);
            h.setPadding(0, dp(20), 0, 0);
            col.addView(h);

            CheckBox au = new CheckBox(this);
            au.setText("Auto colour");
            au.setChecked(cfg.auto[e]);
            col.addView(au);

            LinearLayout hsvBox = new LinearLayout(this);
            hsvBox.setOrientation(LinearLayout.VERTICAL);
            hsvBox.setVisibility(cfg.auto[e] ? View.GONE : View.VISIBLE);
            final float[] hsv = new float[3];
            Color.colorToHSV(0xFF000000 | cfg.col[e], hsv);
            slider(hsvBox, "Hue", 0, 360, Math.round(hsv[0]), n -> n + "\u00b0",
                    n -> { hsv[0] = n; cfg.col[e] = Color.HSVToColor(hsv) & 0xFFFFFF; });
            slider(hsvBox, "Saturation", 0, 100, Math.round(hsv[1] * 100), pct,
                    n -> { hsv[1] = n / 100f; cfg.col[e] = Color.HSVToColor(hsv) & 0xFFFFFF; });
            slider(hsvBox, "Brightness", 0, 100, Math.round(hsv[2] * 100), pct,
                    n -> { hsv[2] = n / 100f; cfg.col[e] = Color.HSVToColor(hsv) & 0xFFFFFF; });
            col.addView(hsvBox);
            au.setOnCheckedChangeListener((btn, on) -> {
                cfg.auto[e] = on;
                hsvBox.setVisibility(on ? View.GONE : View.VISIBLE);
                preview();
                save();
            });

            slider(col, "Opacity", 0, 100, cfg.alpha[e], pct, n -> cfg.alpha[e] = n);
        }

        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(16), dp(8), dp(16), dp(8));
        Button rs = new Button(this);
        rs.setText("Reset look");
        rs.setOnClickListener(v -> {
            Store.putCfg(this, wid, new Cfg());
            Widgets.update(this, wid);
            recreate();
        });
        Button dn = new Button(this);
        dn.setText("Done");
        dn.setOnClickListener(v -> finish());
        bar.addView(rs, new LinearLayout.LayoutParams(0, -2, 1f));
        bar.addView(dn, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(bar);
    }

    /** The preview is the real RemoteViews, fed a sample timer at the chosen progress. */
    void preview() {
        box.removeAllViews();
        Item t = Store.get(this, Store.tid(this, wid)), s = new Item();
        if (t != null) { s.name = t.name; s.color = t.color; s.mode = t.mode; s.dur = t.dur; }
        s.id = -1;
        s.start = System.currentTimeMillis() - (long) (prev / 100f * s.dur);
        View v = Widgets.render(this, wid, cfg, s, true).apply(this, box);
        box.addView(v, new FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER));
    }

    void save() {
        Store.putCfg(this, wid, cfg);
        Widgets.update(this, wid);
    }

    /** Preview follows the finger; the widget itself is updated when the finger lifts. */
    void slider(LinearLayout parent, String label, int min, int max, int val,
                IntFunction<String> fmt, IntConsumer on) {
        TextView t = new TextView(this);
        t.setText(label + ": " + fmt.apply(val));
        t.setPadding(0, dp(8), 0, 0);
        parent.addView(t);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(val - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int n = p + min;
                t.setText(label + ": " + fmt.apply(n));
                if (fromUser) { on.accept(n); preview(); }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { save(); }
        });
        parent.addView(sb);
    }
}
