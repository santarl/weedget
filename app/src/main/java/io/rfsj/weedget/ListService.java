package io.rfsj.weedget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;
import java.util.List;

public class ListService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent i) { return new F(getApplicationContext()); }

    static class F implements RemoteViewsFactory {
        final Context c;
        List<Item> l = new ArrayList<>();

        F(Context c) { this.c = c; }

        public void onCreate() { }
        public void onDataSetChanged() { l = Store.all(c); }
        public void onDestroy() { }
        public int getCount() { return l.size(); }

        public RemoteViews getViewAt(int p) {
            if (p >= l.size()) return null;
            Item it = l.get(p);
            long now = System.currentTimeMillis();
            RemoteViews r = new RemoteViews(c.getPackageName(), R.layout.w_row);
            r.setTextViewText(R.id.rname, it.name);
            r.setInt(R.id.dot, "setBackgroundColor", Widgets.col(it, it.state(now)));
            Widgets.time(r, it, now);
            r.setOnClickFillInIntent(R.id.row, new Intent().putExtra("id", it.id));
            return r;
        }

        public RemoteViews getLoadingView() { return null; }
        public int getViewTypeCount() { return 1; }
        public long getItemId(int p) { return p < l.size() ? l.get(p).id : p; }
        public boolean hasStableIds() { return true; }
    }
}
