package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by Ankur on 07-Aug-16.
 */

public class StockWidgetProvider extends AppWidgetProvider {

    public static final String SYMBOL_KEY_PREFIX = "id";
    public static final String SYMBOL_KEY_SEPARATOR = "_";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent serviceIntent = new Intent(context, StockIntentService.class);
        serviceIntent.putExtra("tag", "widget");
        context.startService(serviceIntent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        Intent serviceIntent = new Intent(context, StockIntentService.class);
        serviceIntent.putExtra("tag", "widget");
        context.startService(serviceIntent);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);
        Intent serviceIntent = new Intent(context, StockIntentService.class);
        serviceIntent.putExtra("tag", "widget");
        context.startService(serviceIntent);
    }


}
