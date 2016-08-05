package com.sam_chordas.android.stockhawk.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Ankur on 01-Aug-16.
 */

public class HistoricalData {
    @SerializedName("query")
    private HistoricalDataQuery historicalDataQuery;

    public HistoricalDataQuery getHistoricalDataQuery() {
        return historicalDataQuery;
    }


    public class HistoricalDataQuery{
        @SerializedName("results")
        private HistoricalDataQuote historicalDataQuote;

        public HistoricalDataQuote getHistoricalDataQuote() {
            return historicalDataQuote;
        }
    }

    public class HistoricalDataQuote{
        @SerializedName("quote")
        private List<HistoricalStockDataItem> historicalStockDataItems;

        public List<HistoricalStockDataItem> getHistoricalStockDataList() {
            return historicalStockDataItems;
        }
    }

    public class HistoricalStockDataItem {
        @SerializedName("Symbol")
        private String symbol;

        @SerializedName("Date")
        private String date;

        @SerializedName("Close")
        private Double closingPrice;

        public String getSymbol() {
            return symbol;
        }

        public String getDate() {
            return date;
        }

        public Double getClosingPrice() {
            return closingPrice;
        }
    }
}
