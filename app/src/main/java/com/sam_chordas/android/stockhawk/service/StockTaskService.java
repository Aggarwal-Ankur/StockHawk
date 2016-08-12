package com.sam_chordas.android.stockhawk.service;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.data.WidgetDataPojo;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.widget.StockWidgetProvider;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public static final int ERROR = -1;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate) {
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    }
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentVals(getResponse));

                    if(isUpdate) {
                        updateWidgets();
                    }
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                return ERROR;
            }
        }

        return result;
    }


    private void updateWidgets(){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(mContext,
                StockWidgetProvider.class));


        HashMap<String, WidgetDataPojo> stockPriceMap = new HashMap<>();

        //Get the data from the DB and update the widgets
        try {
            Cursor initQueryCursor = mContext.getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL,
                            QuoteColumns.BIDPRICE, QuoteColumns.ISCURRENT,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.ISUP},
                    null, null, null);

            if (initQueryCursor != null) {
                initQueryCursor.moveToFirst();

                int columnIndexSymbol = initQueryCursor.getColumnIndex(QuoteColumns.SYMBOL);
                int columnIndexPrice = initQueryCursor.getColumnIndex(QuoteColumns.BIDPRICE);
                int columnIndexIsCurrent = initQueryCursor.getColumnIndex(QuoteColumns.ISCURRENT);
                int columnIndexPercentChange = initQueryCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE);
                int columnIndexIsUp = initQueryCursor.getColumnIndex(QuoteColumns.ISUP);


                int elementCount = initQueryCursor.getCount();

                for (int i = 0; i < elementCount; i++) {
                    boolean isCurrent = initQueryCursor.getInt(columnIndexIsCurrent) == 1 ? true : false;

                    if(!isCurrent){
                        initQueryCursor.moveToNext();
                        continue;
                    }

                    String currentSymbol = initQueryCursor.getString(columnIndexSymbol);
                    String currentPrice = initQueryCursor.getString(columnIndexPrice);
                    String percentChange = initQueryCursor.getString(columnIndexPercentChange);
                    boolean isUp = (initQueryCursor.getInt(columnIndexIsUp) == 1) ? true : false;

                    WidgetDataPojo currentData = new WidgetDataPojo(currentPrice, percentChange, isUp);

                    stockPriceMap.put(currentSymbol, currentData);
                    initQueryCursor.moveToNext();
                }

                initQueryCursor.close();

            }

            if(!stockPriceMap.isEmpty()){
                //Common drawables first
                int upColor = R.color.material_green_700;
                int downColor = R.color.material_red_700;

                for(int currentAppWidgetId : appWidgetIds){
                    String key = StockWidgetProvider.SYMBOL_KEY_PREFIX
                            + StockWidgetProvider.SYMBOL_KEY_SEPARATOR + Integer.toString(currentAppWidgetId);

                    String widgetSymbol = loadSymbolPref(mContext, key);

                    if(widgetSymbol == null || widgetSymbol.trim().isEmpty()){
                        continue;
                    }

                    //Else check if the symbol is in hashmap

                    if(stockPriceMap.containsKey(widgetSymbol)){
                        WidgetDataPojo currentData = stockPriceMap.get(widgetSymbol);

                        //Update in the widget
                        int layoutId = R.layout.single_stock_widget_layout;
                        RemoteViews views = new RemoteViews(mContext.getPackageName(), layoutId);

                        views.setTextViewText(R.id.stock_symbol, widgetSymbol);
                        views.setTextViewText(R.id.bid_price, currentData.getPrice());


                        if (currentData.isUp()){
                            views.setInt(R.id.change, "setBackgroundResource", upColor);
                        } else{
                            views.setInt(R.id.change, "setBackgroundResource", downColor);
                        }

                        //We will always show percent change on the widget
                        views.setTextViewText(R.id.change, currentData.getPercentChange());

                        // Create an Intent to launch MainActivity
                        Intent launchIntent = new Intent(mContext, MyStocksActivity.class);
                        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, launchIntent, 0);
                        views.setOnClickPendingIntent(R.id.widget, pendingIntent);

                        // Tell the AppWidgetManager to perform an update on the current app widget
                        appWidgetManager.updateAppWidget(currentAppWidgetId, views);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String loadSymbolPref(Context context, String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, null);
    }

}
