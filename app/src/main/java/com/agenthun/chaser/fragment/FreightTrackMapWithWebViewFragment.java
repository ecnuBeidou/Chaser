package com.agenthun.chaser.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.agenthun.chaser.R;
import com.agenthun.chaser.activity.LoginActivity;
import com.agenthun.chaser.activity.TimePickerActivity;
import com.agenthun.chaser.bean.base.LatLng;
import com.agenthun.chaser.bean.base.LocationDetail;
import com.agenthun.chaser.connectivity.manager.RetrofitManager2;
import com.agenthun.chaser.connectivity.service.PathType;
import com.agenthun.chaser.utils.DistanceUtil;
import com.agenthun.chaser.utils.LanguageUtil;
import com.agenthun.chaser.utils.PreferencesHelper;
import com.agenthun.chaser.view.BottomSheetDialogView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @project ESeal
 * @authors agenthun
 * @date 2017/2/15 10:53.
 */

public class FreightTrackMapWithWebViewFragment extends Fragment {
    private static final String TAG = "FreightTrackMapFragment";

    private static final String ARGUMENT_FREIGHT_ID = "ARGUMENT_FREIGHT_ID";
    private static final String ARGUMENT_FREIGHT_NAME = "ARGUMENT_FREIGHT_NAME";

    // 通过设置间隔时间和距离可以控制速度和图标移动的距离
    private static final int TIME_INTERVAL = 80;
    private static final double DISTANCE_RATIO = 10000000.0D;
    private static final double MOVE_DISTANCE_MIN = 0.0001;
    private static final int LOCATION_RADIUS = 50;

    private static final double[] BAIDU_MAP_ZOOM = {
            50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 25000,
            50000, 100000, 200000, 500000, 1000000, 2000000
    };

    private double moveDistance = 0.0001;

    @Bind(R.id.webView)
    WebView webView;
    @Bind(R.id.blurredMap)
    ImageView blurredMap;

    private String mFreightId = null;
    private String mFreightName = null;
    private boolean mIsFreightTrackMode = false;
    private LocationDetail mLocationDetail = null;
    private List<LocationDetail> mLocationDetailList = new ArrayList<>();

    private boolean mUsingWebView = true;

    public static FreightTrackMapWithWebViewFragment newInstance(String id, String name) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_FREIGHT_ID, id);
        arguments.putString(ARGUMENT_FREIGHT_NAME, name);
        FreightTrackMapWithWebViewFragment fragment = new FreightTrackMapWithWebViewFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    public FreightTrackMapWithWebViewFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_freight_track_map_with_webview, container, false);
        ButterKnife.bind(this, view);

        mFreightId = getArguments().getString(ARGUMENT_FREIGHT_ID);
        mFreightName = getArguments().getString(ARGUMENT_FREIGHT_NAME);

        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setOnClickListener(mOnFabClickListener);

        //根据当前系统语言设置加载不同的Map Ui
        mUsingWebView = "zh-CN".equals(LanguageUtil.getLanguage()) ? false : true;
        mUsingWebView = true; //for test webviewMap
        setupWebView();

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        loadFreightLocation(false, PreferencesHelper.getTOKEN(getActivity()), mFreightId, null, null);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_track_search:
                mIsFreightTrackMode = true;
                startTimePickerActivity();
                return true;
            case R.id.action_location_search:
                mIsFreightTrackMode = false;
                loadFreightLocation(false, PreferencesHelper.getTOKEN(getActivity()), mFreightId, null, null);
                return true;
            case R.id.action_sign_out:
                signOut(true);
                return true;
            default:
                mIsFreightTrackMode = false;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == TimePickerActivity.RESULT_PICK_TIME) {
            String from = data.getStringExtra(TimePickerActivity.PICK_TIME_FROM);
            String to = data.getStringExtra(TimePickerActivity.PICK_TIME_TO);
            Log.d(TAG, "from: " + from + ", to: " + to);

            loadFreightLocation(true, PreferencesHelper.getTOKEN(getActivity()),
                    mFreightId, from, to);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private View.OnClickListener mOnFabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mIsFreightTrackMode) {
                if (mLocationDetailList != null && !mLocationDetailList.isEmpty()) {
                    showFreightDataListByBottomSheet(PreferencesHelper.getTOKEN(getActivity()),
                            mFreightId, mFreightName, mLocationDetailList);
                }
            } else {
                if (mLocationDetail != null) {
                    showFreightDataListByBottomSheet(PreferencesHelper.getTOKEN(getActivity()),
                            mFreightId, mFreightName, Arrays.asList(mLocationDetail));
                }
            }
        }
    };

    private void setupMapUi(boolean usingWebView) {
        if (usingWebView) {
            blurredMap.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        } else {
            blurredMap.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
        }
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        WebSettings webSettings = webView.getSettings();
        webSettings.setAllowFileAccess(true);
        webSettings.setBuiltInZoomControls(true);
    }

    /**
     * 加载轨迹数据至WebView Google地图
     */
    private void showWebViewMap(List<LocationDetail> locationDetails) {
        blurredMap.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        if (locationDetails == null || locationDetails.size() == 0) return;

        final List<LatLng> polylines = new ArrayList<>();
        for (LocationDetail locationDetail :
                locationDetails) {
            if (locationDetail.isInvalid()) continue;

            LatLng lng = locationDetail.getLatLng();
            polylines.add(lng);
        }

        Collections.reverse(polylines); //按时间正序

        webView.post(new Runnable() {
            @Override
            public void run() {
                String data = buildHtmlMap(polylines);
//                String data = buildHtmlSample();

                //loadData不支持#、%、\、? 四种字符，用loadDataWithBaseURL
                webView.loadDataWithBaseURL(null, data, "text/html", "UTF-8", null);
            }
        });
    }

    private String buildHtmlSample() {
        return "<html><body><font color='red'>hello baidu!</font></body></html>";
    }

    private String buildHtmlMap(List<LatLng> polylines) {
        StringBuffer html = new StringBuffer();

        double minLat = polylines.get(0).latitude;
        double maxLat = polylines.get(0).latitude;
        double minLng = polylines.get(0).longitude;
        double maxLng = polylines.get(0).longitude;

        LatLng point;

        html.append("<!DOCTYPE html>");
        html.append("<head>");
        html.append("<meta charset='utf-8'>");
        html.append("<style>");
        html.append("#map {height: 100%;}");
        html.append("html, body {height: 100%;margin: 0;padding: 0;}");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div id='map'></div>");
        html.append("<script>");
        html.append("var neighborhoods = [");

        html.append("{lat: " + minLat + ", lng: " + minLng + "},");

        for (int i = 1; i < polylines.size(); i++) {
            point = polylines.get(i);
            if (point.latitude < minLat) minLat = point.latitude;
            if (point.latitude > maxLat) maxLat = point.latitude;
            if (point.longitude < minLng) minLng = point.longitude;
            if (point.longitude > maxLng) maxLng = point.longitude;

            html.append("{lat: " + point.latitude + ", lng: " + point.longitude + "},");
        }

        //设置中心点,以及缩放参数
        double centerLat = (maxLat + minLat) / 2;
        double centerLng = (maxLng + minLng) / 2;
        int zoom = getGoogleMapZoom(minLat, maxLat, minLng, maxLng);

        html.deleteCharAt(html.lastIndexOf(","));

        html.append("];");
        html.append("var markers = [];");
        html.append("var map;");
        html.append("function initMap() {");
        html.append("map = new google.maps.Map(document.getElementById('map'), {");
        html.append("zoom: " + zoom + ",");
        html.append("center: {lat: " + centerLat + ", lng: " + centerLng + "}");
        html.append("});");

        if (polylines.size() == 1) {
            html.append("drop();");
        }

        html.append("var flightPath = new google.maps.Polyline({");
        html.append("path: neighborhoods,");
        html.append("geodesic: true,");
        html.append("strokeColor: '#FF0000',");
        html.append("strokeOpacity: 1.0,");
        html.append("strokeWeight: 2");
        html.append("});");
        html.append("flightPath.setMap(map);");
        html.append("}");
        html.append("function drop() {");
        html.append("clearMarkers();");
        html.append("for (var i = 0; i < neighborhoods.length; i++) {");
        html.append("addMarkerWithTimeout(neighborhoods[i], i * 200);");
        html.append("}");
        html.append("}");
        html.append("function addMarkerWithTimeout(position, timeout) {");
        html.append("window.setTimeout(function() {");
        html.append("markers.push(new google.maps.Marker({");
        html.append("position: position,");
        html.append("map: map");
//        html.append("animation: google.maps.Animation.DROP");
        html.append("}));");
        html.append("}, timeout);");
        html.append("}");
        html.append("function clearMarkers() {");
        html.append("for (var i = 0; i < markers.length; i++) {");
        html.append("markers[i].setMap(null);");
        html.append("}");
        html.append("markers = [];");
        html.append("}");
        html.append("</script>");
        html.append("<script async defer src='https://maps.googleapis.com/maps/api/js?key=AIzaSyAp2aNol3FhJypghIA2IUZIOkNTwo6YPbY&callback=initMap'></script>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 获取Google地图显示等级
     * 范围0-18级
     */
    private int getGoogleMapZoom(double minLat, double maxLat, double minLng, double maxLng) {
        LatLng minLatLng = new LatLng(minLat, minLng);
        LatLng maxLatLng = new LatLng(maxLat, maxLng);
//        double distance = DistanceUtil.getDistance(minLatLng, maxLatLng);
        double distance = DistanceUtil.getDistance(minLat, minLng, maxLat, maxLng);

        if (distance == 0.0d) {
            return 12;
        }
        if (distance > 0.0d && distance <= 100.0d) {
            return 18;
        }

        for (int i = 0; i < BAIDU_MAP_ZOOM.length; i++) {
            if (BAIDU_MAP_ZOOM[i] - distance > 0) {
                moveDistance = (BAIDU_MAP_ZOOM[i] - distance) / DISTANCE_RATIO;
                Log.d(TAG, "getZoom() moveDistance = " + moveDistance);
                return 18 - i;
            }
        }
        return 12;
    }

    /**
     * UTC时间转本地时间
     */
    private String utc2Local(String utcTime, String utcTimePatten, String localTimePatten) {
        SimpleDateFormat utcFormater = new SimpleDateFormat(utcTimePatten);
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date gpsUTCDate = null;
        try {
            gpsUTCDate = utcFormater.parse(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat localFormater = new SimpleDateFormat(localTimePatten);
        localFormater.setTimeZone(TimeZone.getDefault());
        String localTime = localFormater.format(gpsUTCDate.getTime());
        return localTime;
    }

    private void showLoadingFreightLocationError() {
        blurredMap.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        showMessage(getString(R.string.error_query_freight_location));
    }

    private void showMessage(String message) {
        Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                .setAction("Action", null);
        ((TextView) (snackbar.getView().findViewById(R.id.snackbar_text)))
                .setTextColor(ContextCompat.getColor(getContext(), R.color.blue_grey_100));
        snackbar.show();
    }

    private void loadFreightLocation(final boolean isFreightTrackMode,
                                     @NonNull final String token, @NonNull String id,
                                     @Nullable String from, @Nullable String to) {
        if (isFreightTrackMode) {
            //获取时间段内位置列表
            RetrofitManager2.builder(PathType.WEB_SERVICE_V2_TEST).getFreightLocationListObservable(token, id, from, to)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .unsubscribeOn(Schedulers.io())
                    .subscribe(new Observer<List<LocationDetail>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            showLoadingFreightLocationError();
                        }

                        @Override
                        public void onNext(List<LocationDetail> locationDetails) {
                            mLocationDetailList = locationDetails;
                            showWebViewMap(locationDetails);
                        }
                    });
        } else {
            //获取最新位置点
            RetrofitManager2.builder(PathType.WEB_SERVICE_V2_TEST).getFreightLocationObservable(token, id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .unsubscribeOn(Schedulers.io())
                    .subscribe(new Observer<LocationDetail>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            showLoadingFreightLocationError();
                        }

                        @Override
                        public void onNext(LocationDetail locationDetail) {
                            mLocationDetail = locationDetail;
                            showWebViewMap(Arrays.asList(locationDetail));
                        }
                    });
        }
    }

    private void startTimePickerActivity() {
        Intent startIntent = TimePickerActivity.getStartIntent(getContext());
        startActivityForResult(startIntent, TimePickerActivity.REQUEST_PICK_TIME);
        /*ActivityCompat.startActivityForResult(getActivity(),
                startIntent,
                TimePickerActivity.REQUEST_PICK_TIME,
                ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle());*/
    }

    private void showFreightDataListByBottomSheet(String token, String containerId, final String containerNo, List<LocationDetail> details) {
        BottomSheetDialogView.show(getContext(), containerNo, details);
    }

    private void signOut(boolean isSave) {
        PreferencesHelper.signOut(getContext(), isSave);
        LoginActivity.start(getActivity(), isSave);
        ActivityCompat.finishAfterTransition(getActivity());
    }
}
