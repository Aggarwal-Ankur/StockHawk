package com.sam_chordas.android.stockhawk.data;

/**
 * Created by Ankur on 8/11/2016.
 */
public class WidgetDataPojo {
    private String price;
    private String percentChange;
    private boolean isUp;

    public WidgetDataPojo(){

    }

    public WidgetDataPojo(String price, String percentChange, boolean isUp){
        this.price = price;
        this.percentChange = percentChange;
        this.isUp = isUp;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(String percentChange) {
        this.percentChange = percentChange;
    }

    public boolean isUp() {
        return isUp;
    }

    public void setUp(boolean up) {
        isUp = up;
    }
}
