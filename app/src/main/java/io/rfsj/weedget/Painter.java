package io.rfsj.weedget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

/** Draws the shape behind the number. Everything is driven by Cfg. */
final class Painter {
    static final int S = 192;

    static Bitmap draw(Cfg c, float p, int stateCol) {
        Bitmap b = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        int tr = c.argb(Cfg.TRACK, stateCol), pr = c.argb(Cfg.PROG, stateCol);
        float rad = S * c.radius / 100f, lw = S * c.line / 100f;
        q.setColor(c.argb(Cfg.BG, stateCol));
        cv.drawRoundRect(new RectF(0, 0, S, S), rad, rad, q);
        p = Math.max(0, Math.min(1, p));
        switch (c.style) {
            case Cfg.RING:
            case Cfg.GAUGE: {
                float m = S * 0.1f + lw / 2;
                RectF r = new RectF(m, m, S - m, S - m);
                float start = c.style == Cfg.RING ? -90 : 135, sweep = c.style == Cfg.RING ? 360 : 270;
                q.setStyle(Paint.Style.STROKE);
                q.setStrokeWidth(lw);
                q.setStrokeCap(Paint.Cap.ROUND);
                q.setColor(tr);
                cv.drawArc(r, start, sweep, false, q);
                if (p > 0) { q.setColor(pr); cv.drawArc(r, start, sweep * p, false, q); }
                break;
            }
            case Cfg.PIE: {
                RectF r = new RectF(S * 0.1f, S * 0.1f, S * 0.9f, S * 0.9f);
                q.setColor(tr);
                cv.drawOval(r, q);
                q.setColor(pr);
                cv.drawArc(r, -90, 360 * p, true, q);
                break;
            }
            case Cfg.SEGS: {
                int n = 24, lit = Math.round(p * n);
                float mid = S / 2f, outer = S * 0.42f, inner = outer - Math.max(S * 0.08f, lw * 1.5f);
                q.setStyle(Paint.Style.STROKE);
                q.setStrokeWidth(Math.max(3, lw * 0.55f));
                q.setStrokeCap(Paint.Cap.ROUND);
                for (int i = 0; i < n; i++) {
                    double a = Math.toRadians(-90 + i * 360.0 / n);
                    float cs = (float) Math.cos(a), sn = (float) Math.sin(a);
                    q.setColor(i < lit ? pr : tr);
                    cv.drawLine(mid + cs * inner, mid + sn * inner, mid + cs * outer, mid + sn * outer, q);
                }
                break;
            }
            case Cfg.VBAR: {
                float w = Math.max(S * 0.07f, Math.min(S * 0.22f, lw * 1.6f)), m = S * 0.1f;
                bar(cv, q, new RectF(m, m, m + w, S - m), w / 2, p, true, tr, pr);
                break;
            }
            case Cfg.HBAR: {
                float h = Math.max(S * 0.07f, Math.min(S * 0.22f, lw * 1.6f)), m = S * 0.1f;
                bar(cv, q, new RectF(m, S - m - h, S - m, S - m), h / 2, p, false, tr, pr);
                break;
            }
            case Cfg.FILL: {
                int a = (int) ((pr >>> 24) * 0.75f);
                bar(cv, q, new RectF(0, 0, S, S), rad, p, true, tr, (a << 24) | (pr & 0xFFFFFF));
                break;
            }
            default:
                break; // text only
        }
        return b;
    }

    /** Track as is, progress clipped to the track's shape (anti-aliased). */
    private static void bar(Canvas cv, Paint q, RectF shape, float rr, float p, boolean vertical, int tr, int pr) {
        q.setStyle(Paint.Style.FILL);
        q.setColor(tr);
        cv.drawRoundRect(shape, rr, rr, q);
        int sc = cv.saveLayer(null, null);
        q.setColor(pr);
        if (vertical) cv.drawRect(shape.left, shape.bottom - shape.height() * p, shape.right, shape.bottom, q);
        else cv.drawRect(shape.left, shape.top, shape.left + shape.width() * p, shape.bottom, q);
        q.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        q.setColor(0xFF000000);
        cv.drawRoundRect(shape, rr, rr, q);
        q.setXfermode(null);
        cv.restoreToCount(sc);
    }
}
