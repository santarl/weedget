package io.rfsj.tmr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

/** Styles: 0 ring, 1 pie, 2 vertical bar, 3 fill */
final class Painter {
    static Bitmap draw(int style, float p, int col) {
        final int S = 160;
        final int dim = 0x33FFFFFF;
        Bitmap b = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setColor(0xE61C1C20);
        c.drawRoundRect(new RectF(0, 0, S, S), 36, 36, q);
        p = Math.max(0, Math.min(1, p));
        if (style == 0) {
            RectF r = new RectF(18, 18, S - 18, S - 18);
            q.setStyle(Paint.Style.STROKE);
            q.setStrokeWidth(14);
            q.setStrokeCap(Paint.Cap.ROUND);
            q.setColor(dim);
            c.drawArc(r, 0, 360, false, q);
            if (p > 0) { q.setColor(col); c.drawArc(r, -90, 360 * p, false, q); }
        } else if (style == 1) {
            RectF r = new RectF(14, 14, S - 14, S - 14);
            q.setColor(dim);
            c.drawOval(r, q);
            q.setColor(col);
            c.drawArc(r, -90, 360 * p, true, q);
        } else {
            RectF shape = style == 2 ? new RectF(16, 16, 56, S - 16) : new RectF(0, 0, S, S);
            float rad = style == 2 ? 20 : 36;
            int sc = c.saveLayer(null, null);
            q.setColor(dim);
            c.drawRoundRect(shape, rad, rad, q);
            q.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
            q.setColor(style == 3 ? (col & 0x00FFFFFF) | 0xB0000000 : col);
            c.drawRect(shape.left, shape.bottom - shape.height() * p, shape.right, shape.bottom, q);
            q.setXfermode(null);
            c.restoreToCount(sc);
        }
        return b;
    }
}
