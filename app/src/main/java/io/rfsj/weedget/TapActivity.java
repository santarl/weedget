package io.rfsj.weedget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Idle timer: starts it and closes. Running/done: slide-to-restart card with name, rename and look. */
public class TapActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        final int id = getIntent().getIntExtra("id", 0);
        final Item it = Store.get(this, id);
        if (it == null) { finish(); return; }
        long now = System.currentTimeMillis();
        int st = it.state(now);
        if (st == Item.IDLE) { Engine.start(this, id); finish(); return; }

        float d = getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0x99000000);
        root.setOnClickListener(v -> finish());

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * d);
        card.setPadding(pad, pad, pad, pad);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF26262B);
        g.setCornerRadius(28 * d);
        card.setBackground(g);
        card.setClickable(true);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        final TextView nameView = new TextView(this);
        nameView.setText(it.name);
        nameView.setTextSize(22);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(0xFFFFFFFF);
        nameView.setSingleLine();
        TextView rename = new TextView(this);
        rename.setText("Rename");
        rename.setTextColor(0xFF90CAF9);
        rename.setPadding((int) (12 * d), (int) (8 * d), 0, (int) (8 * d));
        rename.setOnClickListener(v -> rename(id, nameView));
        head.addView(nameView, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(rename);
        TextView s = new TextView(this);
        s.setText(st == Item.DONE ? "Finished" : "Running " + Item.fmt(now - it.start));
        s.setTextColor(0xFFAAAAAA);
        SlideView sv = new SlideView(this);
        sv.cb = () -> { Engine.restart(this, id); finish(); };

        card.addView(head);
        card.addView(s);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = (int) (16 * d);
        card.addView(sv, lp);

        final int wid = getIntent().getIntExtra("wid", AppWidgetManager.INVALID_APPWIDGET_ID);
        if (wid != AppWidgetManager.INVALID_APPWIDGET_ID) {
            TextView look = new TextView(this);
            look.setText("Customize look");
            look.setTextColor(0xFFFFFFFF);
            look.setGravity(Gravity.CENTER);
            look.setPadding(0, (int) (14 * d), 0, (int) (14 * d));
            GradientDrawable lg = new GradientDrawable();
            lg.setColor(0x22FFFFFF);
            lg.setCornerRadius(28 * d);
            look.setBackground(lg);
            look.setOnClickListener(v -> {
                startActivity(new Intent(this, StyleActivity.class).putExtra("wid", wid)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            });
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-1, -2);
            ll.topMargin = (int) (12 * d);
            card.addView(look, ll);
        }

        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        int m = (int) (24 * d);
        fp.setMargins(m, 0, m, 0);
        root.addView(card, fp);
        setContentView(root);
    }

    private void rename(int id, TextView nameView) {
        final Item cur = Store.get(this, id);
        if (cur == null) return;
        final EditText et = new EditText(this);
        et.setText(cur.name);
        et.setSingleLine();
        et.setSelectAllOnFocus(true);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout box = new FrameLayout(this);
        box.setPadding(p, p / 2, p, 0);
        box.addView(et);
        new AlertDialog.Builder(this).setTitle("Rename timer").setView(box)
                .setPositiveButton("Save", (dl, which) -> {
                    String n = et.getText().toString().trim();
                    if (n.isEmpty()) return;
                    cur.name = n;
                    Engine.save(this, cur); // also renames its notification channel
                    nameView.setText(n);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
