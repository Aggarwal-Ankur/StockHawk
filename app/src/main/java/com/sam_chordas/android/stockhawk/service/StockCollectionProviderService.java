package com.sam_chordas.android.stockhawk.service;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.LineGraphActivity;

/**
 * Created by Ankur on 11-Aug-16.
 */

public class StockCollectionProviderService extends RemoteViewsService {
    private String[] QUOTE_COLUMNS = {
            "Distinct " + QuoteColumns.SYMBOL, QuoteColumns._ID,
            QuoteColumns.BIDPRICE,
            QuoteColumns.ISCURRENT,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.ISUP};

    private static final int INDEX_ID = 1;
    private static final int INDEX_SYMBOL = 0;
    private static final int INDEX_BIDPRICE = 2;
    private static final int INDEX_ISCURRENT = 3;
    private static final int INDEX_PERCENT_CHANGE = 4;
    private static final int INDEX_ISUP = 5;

    //Common drawables first
    private int upColor = R.color.material_green_700;
    private int downColor = R.color.material_red_700;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor cursor = null;

            @Override
            public void onCreate() {
                //Do nothing
            }

            @Override
            public void onDataSetChanged() {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }

                final long identityToken = Binder.clearCallingIdentity();
                cursor = getContentResolver().query(
                        QuoteProvider.Quotes.CONTENT_URI,
                        QUOTE_COLUMNS,
                        QuoteColumns.ISCURRENT + " = 1",
                        null, null);

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }

            @Override
            public int getCount() {
                return cursor == null ? 0 : cursor.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        cursor == null || !cursor.moveToPosition(position)) {
                    return null;
                }

                String currentSymbol = cursor.getString(INDEX_SYMBOL);
                String currentPrice = cursor.getString(INDEX_BIDPRICE);
                String percentChange = cursor.getString(INDEX_PERCENT_CHANGE);
                boolean isUp = (cursor.getInt(INDEX_ISUP) == 1) ? true : false;

                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.single_stock_widget_layout);

                views.setTextViewText(R.id.stock_symbol, currentSymbol);
                views.setTextViewText(R.id.bid_price, currentPrice);


                if (isUp){
                    views.setInt(R.id.change, "setBackgroundResource", upColor);
                } else{
                    views.setInt(R.id.change, "setBackgroundResource", downColor);
                }

                //We will always show percent change on the widget
                views.setTextViewText(R.id.change, percentChange);

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(LineGraphActivity.SYMBOL_KEY, currentSymbol);
                views.setOnClickFillInIntent(R.id.widget, fillInIntent);
                
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.single_stock_widget_layout);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (cursor.moveToPosition(position))
                    return cursor.getLong(INDEX_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
