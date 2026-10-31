package io.rfsj.weedget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Receiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        int id = i.getIntExtra("id", 0);
        if ("START".equals(a)) Engine.start(c, id);
        else if ("DONE".equals(a)) Engine.done(c, id);
        else { Engine.schedule(c); Widgets.refresh(c); } // TICK, boot, app update
    }
}
