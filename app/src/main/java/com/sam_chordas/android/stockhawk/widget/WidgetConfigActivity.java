package com.sam_chordas.android.stockhawk.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.io.IOException;

public class WidgetConfigActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private Context mContext;
    private ListView mListView;
    private CharSequence[] mSymbolList;
    private ArrayAdapter<String> mAdapter;

    private SymbolListFetchTask mSymbolListFetchTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);

        //Set it as a fallback. Actual success is set later
        setResult(Activity.RESULT_CANCELED);

        mContext = this;

        setTitle(getResources().getString(R.string.config_activity_title));

        //Set the appwidget ID
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        mListView = (ListView) findViewById(android.R.id.list);

        mSymbolListFetchTask = new SymbolListFetchTask();
        mSymbolListFetchTask.execute();
    }

    @Override
    protected void onDestroy() {
        if(mSymbolListFetchTask != null && !mSymbolListFetchTask.isCancelled()) {
            mSymbolListFetchTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Toast.makeText(mContext, "You chose " + mSymbolList[i], Toast.LENGTH_LONG).show();
        saveSymbolPref(mContext, mAppWidgetId, mSymbolList[i].toString());
        startWidget();
    }

    private void startWidget() {

        // this intent is essential to show the widget
        // if this intent is not included,you can't show
        // widget on homescreen
        Intent intent = new Intent();
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(Activity.RESULT_OK, intent);

        // finish this activity
        this.finish();

    }

    // Write the prefix to the SharedPreferences object for this widget
    private void saveSymbolPref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.putString(Integer.toString(appWidgetId), text);
        prefs.commit();
    }

    /**
     * Utility Asynctask to fetch the symbols stored in the DB
     */
    private class SymbolListFetchTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Cursor initQueryCursor = mContext.getContentResolver().query(
                        QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                        null, null);

                if (initQueryCursor != null) {
                    initQueryCursor.moveToFirst();

                    int columnIndex = initQueryCursor.getColumnIndex("symbol");
                    int elementCount = initQueryCursor.getCount();

                    mSymbolList = new CharSequence[elementCount];
                    for (int i = 0; i < elementCount; i++) {
                        mSymbolList[i] = initQueryCursor.getString(columnIndex);
                        initQueryCursor.moveToNext();
                    }

                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            super.onPostExecute(params);
            if(isCancelled()){
                return;
            }

            mAdapter = new ArrayAdapter(mContext, android.R.layout.simple_list_item_1, mSymbolList);
            mListView.setAdapter(mAdapter);

            mListView.setOnItemClickListener(WidgetConfigActivity.this);
        }
    }
}
