package com.agenthun.chaser.bean.base;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @project Chaser
 * @authors agenthun
 * @date 2017/2/23 19:51.
 */

public class LatLng implements Parcelable {
    public final double latitude;
    public final double longitude;
    public final double latitudeE6;
    public final double longitudeE6;

    //latitude,longitude
    public LatLng(double lat, double lng) {
        double var5 = lat * 1000000.0D;
        double var7 = lng * 1000000.0D;
        this.latitudeE6 = var5;
        this.longitudeE6 = var7;
        this.latitude = var5 / 1000000.0D;
        this.longitude = var7 / 1000000.0D;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitudeE6() {
        return latitudeE6;
    }

    public double getLongitudeE6() {
        return longitudeE6;
    }

    public String toString() {
        String var1 = new String("latitude: ");
        var1 = var1 + this.latitude;
        var1 = var1 + ", longitude: ";
        var1 = var1 + this.longitude;
        return var1;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeDouble(this.latitudeE6);
        dest.writeDouble(this.longitudeE6);
    }

    protected LatLng(Parcel in) {
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.latitudeE6 = in.readDouble();
        this.longitudeE6 = in.readDouble();
    }

    public static final Parcelable.Creator<LatLng> CREATOR = new Parcelable.Creator<LatLng>() {
        @Override
        public LatLng createFromParcel(Parcel source) {
            return new LatLng(source);
        }

        @Override
        public LatLng[] newArray(int size) {
            return new LatLng[size];
        }
    };
}
