package com.agenthun.chaser.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.agenthun.chaser.R;
import com.agenthun.chaser.fragment.ScanNfcDeviceFragment;
import com.agenthun.chaser.location.LocationMaster;
import com.agenthun.chaser.location.LocationReceiver;
import com.agenthun.chaser.utils.ActivityUtils;
import com.agenthun.chaser.utils.DataLogUtils;
import com.agenthun.chaser.utils.PreferencesHelper;
import com.pekingopera.versionupdate.UpdateHelper;
import com.pekingopera.versionupdate.listener.ForceListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;

/**
 * @project ESeal
 * @authors agenthun
 * @date 2017/2/14 16:49.
 */

public class ScanNfcDeviceActivity extends AppCompatActivity {

    private static final String TAG = "ScanNfcDeviceActivity";

    private LocationMaster mLocationMaster;

    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {
        @Override
        protected void onLocationReceived(Context context, Location loc) {
            Log.d(TAG, String.format("Got location: %1$f,%2$f", loc.getLatitude(), loc.getLongitude()));

            LocationLogTask log = new LocationLogTask();
            log.execute(loc);
        }
    };

    public static void start(Context context) {
        Intent starter = new Intent(context, ScanNfcDeviceActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar_frame);

        checkUpdate();

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        attachDeviceFragment();

        supportPostponeEnterTransition();

        mLocationMaster = LocationMaster.get(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(mLocationReceiver, new IntentFilter(LocationMaster.ACTION_LOCATION));
        mLocationMaster.startLocationUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();

        unregisterReceiver(mLocationReceiver);
        mLocationMaster.stopLocationUpdates();
    }

    private void attachDeviceFragment() {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        ScanNfcDeviceFragment fragment = (ScanNfcDeviceFragment) supportFragmentManager.findFragmentById(R.id.content_main);
        if (fragment == null) {
            fragment = ScanNfcDeviceFragment.newInstance();
            ActivityUtils.replaceFragmentToActivity(supportFragmentManager, fragment, R.id.content_main);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_sign_out:
                signOut(true);
                return true;
            case R.id.action_about:
                AboutActivity.start(this);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void signOut(boolean isSave) {
        PreferencesHelper.signOut(this, isSave);
        LoginActivity.start(this, isSave);
        ActivityCompat.finishAfterTransition(this);
    }

    private void checkUpdate() {
        UpdateHelper.getInstance().setForceListener(new ForceListener() {
            @Override
            public void onUserCancel(boolean force) {
                if (force) {
                    finish();
                }
            }
        }).check(this);
    }

    private class LocationLogTask extends AsyncTask<Location, Void, String> {
        @Override
        protected String doInBackground(Location... params) {
            Log.i(TAG, "LocationLogTask doInBackground...");

            Location loc = params[0];

            if (loc == null) {
                return "";
            }

            DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
            DateFormat dateFormat2 = new SimpleDateFormat("yyyy/MM/dd");
            DateFormat dateFormat3 = new SimpleDateFormat("hh:mm:ss");

//            String fileName = String.format("ChaserDataLog_%s.txt", dateFormat1.format(new Date()));
            String fileName = DataLogUtils.DATA_LOG_FILE_NAME;

            try {
                DataLogUtils.logToFileInit(fileName);

                String data = String.format("%s %s %f %s %f %s 0 1 1",
                        dateFormat2.format(new Date()),
                        dateFormat3.format(new Date()),
                        loc.getLatitude(),
                        loc.getLatitude() > 0 ? "N" : "S",
                        loc.getLongitude(),
                        loc.getLongitude() > 0 ? "E" : "W");

                DataLogUtils.logToFile(DataLogUtils.LOCATION_TYPE, data);
            } catch (Exception ex) {
                ex.printStackTrace();

                return "";
            }

            return fileName;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "LocationLogTask onPostExecute: " + result);

            if (result.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Log failed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Log succeeded, file name: " + result, Toast.LENGTH_SHORT).show();
            }

            DataLogUtils.logToFileFinish();
        }
    }
}
