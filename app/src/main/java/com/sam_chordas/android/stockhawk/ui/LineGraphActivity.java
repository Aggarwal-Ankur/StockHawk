package com.sam_chordas.android.stockhawk.ui;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalData;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LineGraphActivity extends AppCompatActivity {

    private static final String TAG = LineGraphActivity.class.getSimpleName();

    public static final String SYMBOL_KEY = "symbol";

    private String mSymbolValue;

    private LineChart mLineChart;

    private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private List<HistoricalData.HistoricalStockDataItem> mStockDataList;

    private ProgressDialog mDialog;

    private HistoricalQuotesFetchTask mHistoricalQuotesFetchTask;

    private long MILLIS_IN_A_DAY = 1000* 60 * 60 * 24;
    private long NO_OF_DAYS = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mLineChart = (LineChart) findViewById(R.id.linechart);

        mSymbolValue = getIntent().getStringExtra(SYMBOL_KEY);
        setTitle(mSymbolValue);

        //Build the dialog
        mDialog = new ProgressDialog(this);
        mDialog.setIndeterminate(true);
        mDialog.setMessage(getResources().getString(R.string.fetching_data));


        StringBuilder urlStringBuilder = new StringBuilder();
        try{
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol "
                    + "= '" + mSymbolValue + "'", "UTF-8"));


            Date today = new Date();

            long todayInMillis = today.getTime();

            //Start date is date 7 days back. Can be changed later
            long offset = NO_OF_DAYS * MILLIS_IN_A_DAY;
            Date startDate = new Date();
            startDate.setTime(todayInMillis - offset);

            String startDateValue = DATE_FORMAT.format(startDate);
            String endDateValue = DATE_FORMAT.format(today);


            urlStringBuilder.append(URLEncoder.encode(" and startDate = '" + startDateValue + "' and endDate "
                    + "= '" + endDateValue + "'", "UTF-8"));

            // finalize the URL for the API query.
            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        mHistoricalQuotesFetchTask = new HistoricalQuotesFetchTask();
        mHistoricalQuotesFetchTask.execute(new String[]{urlStringBuilder.toString()});

        mDialog.show();

    }



    /**
     * Utility Asynctask to fetch the historical Quotes
     */
    private class HistoricalQuotesFetchTask extends AsyncTask<String, Void, List<HistoricalData.HistoricalStockDataItem>> {
        private OkHttpClient client;
        private String responseJson;


        @Override
        protected List<HistoricalData.HistoricalStockDataItem> doInBackground(String... params) {
            if(params == null || params[0] == null || params[0].isEmpty()){
                return null;
            }

            client = new OkHttpClient();

            try {
                Request request = new Request.Builder()
                        .url(params[0])
                        .build();
                Response response = client.newCall(request).execute();

                responseJson = response.body().string();
                Log.d(TAG, "List Json = " + responseJson);

                Gson gson = new Gson();
                List<HistoricalData.HistoricalStockDataItem> stockDataList = gson.fromJson(responseJson, HistoricalData.class).getHistoricalDataQuery().getHistoricalDataQuote().getHistoricalStockDataList();


                mDialog.dismiss();
                return stockDataList;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<HistoricalData.HistoricalStockDataItem> stockDataList) {
            super.onPostExecute(stockDataList);
            if(isCancelled()){
                return;
            }

            if(stockDataList == null || stockDataList.size()< 1){
                Toast.makeText(LineGraphActivity.this, getResources().getString(R.string.fetch_error), Toast.LENGTH_SHORT).show();
            }else{
                ArrayList<Entry> entries = new ArrayList<>();

                ArrayList<String> labels = new ArrayList<String>();


                int size = stockDataList.size();
                int entryCount = 0;

                //WE want the oldest date first
                for(int count =size -1; count>= 0; count--){
                    HistoricalData.HistoricalStockDataItem currentItem = stockDataList.get(count);

                    //This is done to remove the year from the label
                    String dateString = currentItem.getDate();
                    String dateLabel = dateString.substring(5);

                    labels.add(dateLabel);

                    float closingValue = Float.parseFloat(String.format("%.2f", currentItem.getClosingPrice()));

                    entries.add(new Entry(closingValue, entryCount++));
                }

                LineDataSet dataset = new LineDataSet(entries, getResources().getString(R.string.line_graph_x_axis_string));
                LineData data = new LineData(labels, dataset);

                //dataset.setColors(ColorTemplate.COLORFUL_COLORS);
                dataset.setDrawCubic(false);
                dataset.setDrawFilled(false);

                mLineChart.setData(data);
                mLineChart.setBackgroundColor(ColorTemplate.COLOR_NONE);
                mLineChart.setDescription(getResources().getString(R.string.empty));
            }
        }
    }

}
