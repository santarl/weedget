package io.rfsj.tmr;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Slide-to-confirm: only drags if you grab the thumb, and must reach the end. */
final class SlideView extends View {
    Runnable cb;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d = getResources().getDisplayMetrics().density;
    private float x;
    private boolean drag;

    SlideView(Context c) { super(c); }

    @Override protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (56 * d));
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), r = h / 2, max = Math.max(1, w - h);
        p.setColor(0xFF3A3A42);
        c.drawRoundRect(0, 0, w, h, r, r, p);
        p.setColor(0xFFE53935);
        c.drawRoundRect(0, 0, x + h, h, r, r, p);
        p.setColor(0xFFFFFFFF);
        p.setAlpha((int) (255 * (1 - Math.min(1, x / max))));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(15 * d);
        c.drawText("Slide to reset", w / 2 + r / 2, h / 2 + 5 * d, p);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(x + r, r, r - 4 * d, p);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float h = getHeight(), max = getWidth() - h;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                drag = Math.abs(e.getX() - (x + h / 2)) < h / 2 + 8 * d;
                if (drag) getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (drag) { x = Math.max(0, Math.min(max, e.getX() - h / 2)); invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean ok = drag && e.getActionMasked() == MotionEvent.ACTION_UP && x >= max * .97f;
                drag = false;
                if (ok) {
                    if (cb != null) cb.run();
                } else {
                    ValueAnimator a = ValueAnimator.ofFloat(x, 0);
                    a.addUpdateListener(v -> { x = (float) v.getAnimatedValue(); invalidate(); });
                    a.start();
                }
                return true;
        }
        return true;
    }
}
