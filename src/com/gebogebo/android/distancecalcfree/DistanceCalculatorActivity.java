package com.gebogebo.android.distancecalcfree;

import java.text.SimpleDateFormat;
//import java.util.Random;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

//import com.admob.android.ads.AdView;
import com.gebogebo.android.distancecalcfree.DistanceCalculatorService.DistanceServiceBinder;
import com.sensedk.AswAdLayout;
import com.sensedk.AswAdListener;

/**
 * @author viraj
 * 
 */
public class DistanceCalculatorActivity extends Activity implements OnClickListener, DistanceCalculatorServiceListener, AswAdListener {
    private static final String DATE_FORMAT_NOW = "dd-MMM-yyyy HH:mm";

    private static String currentSpeedFormat = null;
    // indicates state of app (calculating distance or not)
    private boolean isCalculatingDistance = false; 
    private boolean isPaused = false;
    private DistanceCalculatorService locationService = null;
    private DistanceCalculatorUtilities util;
    private String hoursStr = null;
    private String timeElapsedStr = null;
    private TextView distanceText = null;
    private TextView speedText = null;
    private TextView timeText = null;
    private TextView errorText = null;

    private float multiplier = 1.0F;
    private String distanceSuffix;
//    private Random random = new Random(System.currentTimeMillis());

    /**
     * set activity variable as per its current operation
     */
    private void setActivityState() {
        Button button = (Button) findViewById(R.drawable.button);
        if (isCalculatingDistance) {
            // if app was brought to distance calculating mode
            // reset everything. don't worry it will get set if service was running in background
            button.setText(R.string.stop);
            distanceText.setText(R.string.dots);
            speedText.setText(R.string.dots);
            timeText.setText(R.string.empty);
            float actualDistance = locationService.getCurrentDistance();
            if (actualDistance >= 0.0) {
                long timeElapsed = locationService.getTimeElapsed();
                String visualDistance = DistanceCalculatorUtilities.getVisualDistance(actualDistance, timeElapsed, multiplier, distanceSuffix,
                        hoursStr);
                distanceText.setText(visualDistance);
                timeText.setText(DistanceCalculatorUtilities.getVisualTime(timeElapsed, timeElapsedStr));
                errorText.setText(R.string.empty);
            } else {
                // it means that we are trying to find location of user
                errorText.setText(R.string.service_temp_not_available);
            }
            util = new DistanceCalculatorUtilities();
            Log.i("activity", "handled on click event for button. calculating distance now.");
        } else {
            // if app was brought to non distance calculating mode
            button.setText(R.string.start);
            Button pauseButton = (Button) findViewById(R.drawable.pause);
            pauseButton.setText(R.string.pause);
            errorText.setText(R.string.empty);
            Log.i("activity", "handled on click event for button. not calculating distance now.");
        }
        SharedPreferences defPref = PreferenceManager.getDefaultSharedPreferences(this);
        String action = defPref.getString(DistanceCalculatorPrefActivity.PREF_KEY_ACTION,
                DistanceCalculatorPrefActivity.PREF_BIKING);
        TextView actionText = (TextView) findViewById(R.drawable.action_display);
        actionText.setText(String.format(getString(R.string.current_action), action));
    }

    /**
     * sets distance calculating parameters as per chosen settings
     */
    private void setDistanceParameters() {
        SharedPreferences defPref = PreferenceManager.getDefaultSharedPreferences(this);
        // default selection is miles
        String selectedMetricId = defPref.getString(DistanceCalculatorPrefActivity.PREF_KEY_DISTANCE_UNIT,
                DistanceCalculatorPrefActivity.PREF_MILES);
        Log.i("activity", "got selected metric: " + selectedMetricId);
        multiplier = DistanceCalculatorPrefActivity.getDistanceMultiplierForDistanceUnit(selectedMetricId);
        if (DistanceCalculatorPrefActivity.PREF_KM.equals(selectedMetricId)) {
            distanceSuffix = getString(R.string.km);
            Log.i("menu", "KMs selected: " + distanceSuffix);
        } else if (DistanceCalculatorPrefActivity.PREF_MILES.equals(selectedMetricId)) {
            distanceSuffix = getString(R.string.miles);
            Log.i("menu", "Miles selected: " + distanceSuffix);
        }
    }

    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("serviceConn", "Service disconnected to activity");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("serviceConn", "Service connected to activity");
            locationService = ((DistanceServiceBinder) service).getService();
            float currDistance = locationService.getCurrentDistance();
            if (currDistance < 0.0) {
                isCalculatingDistance = false;
            } else {
                isCalculatingDistance = true;
            }
            setActivityState();
        }
    };

    // DistanceCalculatorServiceListener methods
    @Override
    public void onDistanceChange(float newDistanceInMeters, long totalTimeInSecs, Location newLocation) {
        errorText.setText(R.string.empty);
        Log.i("serviceListener", "distance changed");
        String visualDistance = DistanceCalculatorUtilities.getVisualDistance(newDistanceInMeters, totalTimeInSecs, multiplier,
                distanceSuffix, hoursStr);
        distanceText.setText(visualDistance);
        String speed = null;
        if (newLocation.hasSpeed()) {
            speed = util.getVisualCurrentSpeed(newLocation.getSpeed(), multiplier, distanceSuffix, hoursStr,
                    currentSpeedFormat);
            speedText.setText(speed);
        }
        timeText.setText(DistanceCalculatorUtilities.getVisualTime(totalTimeInSecs, timeElapsedStr));

//        if (random.nextInt(10) < 3) {
//            // with a probability of 30%
//            AdView adView = (AdView) findViewById(R.id.ad);
//            adView.requestFreshAd();
//        }
    }

    @Override
    public void onServiceStatusChange(int errorCode) {
        int textId = util.getErrorTextId(errorCode);
        if (textId > 0) {
            errorText.setText(textId);
        }
    }

    // activity overrides
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.i("activity", "set activity as content view: " + this);
        
        util = new DistanceCalculatorUtilities();
        distanceText = (TextView) findViewById(R.drawable.distance);
        speedText = (TextView) findViewById(R.drawable.currentSpeed);
        timeText = (TextView) findViewById(R.drawable.timeElapsed);
        errorText = (TextView) findViewById(R.drawable.errors);
        hoursStr = getString(R.string.Hr);
        timeElapsedStr = getString(R.string.timeElapsedFormat);
        currentSpeedFormat = getString(R.string.currSpeedFormat);
        Button button = (Button) findViewById(R.drawable.button);
        Button pauseButton = (Button) findViewById(R.drawable.pause);
        button.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        Log.i("activity", "added button click listener");

        setDistanceParameters();

        Intent serviceStart = new Intent(Intent.ACTION_MAIN);
        serviceStart.setClassName("com.gebogebo.android.distancecalcfree",
                "com.gebogebo.android.distancecalcfree.DistanceCalculatorService");
        startService(serviceStart);
        bindService(serviceStart, serviceConn, BIND_AUTO_CREATE);

        // AdManager.setTestDevices( new String[] { AdManager.TEST_EMULATOR,
        // "CA101E12F9C3DF4E8301247EF68FB13C" } );
//        AdView adView = (AdView) findViewById(R.id.ad);
//        adView.requestFreshAd();
        
        try {
            AswAdLayout senseAd = (AswAdLayout)findViewById(R.id.adview);
            senseAd.setActivity(this);
            senseAd.setListener(this);
            //adView.userDemandToDeleteHisData();
            //adView.userOptOutFromRecommendation();
        } catch (Throwable t) {
            //ignore
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        Log.i("menu", "Options menu created");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_exit) {
            Log.i("activity", "Exit selected");
            this.finish();
        } else if (item.getItemId() == R.id.settings) {
            Log.i("activity", "Settings selected");
            Intent prefIntent = new Intent(Intent.ACTION_EDIT);
            prefIntent.addCategory(Intent.CATEGORY_PREFERENCE);
            startActivityForResult(prefIntent, 1);
        } else if (item.getItemId() == R.id.menu_help) {
            Log.i("activity", "Help selected");
            Intent helpIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://distancecalculator.gebogebo.com/help"));
            startActivity(helpIntent);
        } else if (item.getItemId() == R.id.menu_capture) {
            DistanceCalculatorUtilities.saveParentView(distanceText.getRootView(), this);
            Log.i("activity", "successfully captured screenshot");
        } else if (item.getItemId() == R.id.menu_report) {
            DistanceCalcReport report = getSummaryReport();
            startReportActivity(report, false);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        Log.i("lifecycle", "in destroy");
        if (locationService != null) {
            locationService.removeListener(this);
        }

        if (isFinishing()) {
            Log.i("lifecycle", "activity is destory-finishing");
            // if acticity was
            Intent serviceStop = new Intent(Intent.ACTION_MAIN);
            serviceStop.setClassName("com.gebogebo.android.distancecalcfree",
                    "com.gebogebo.android.distancecalcfree.DistanceCalculatorService");
            unbindService(serviceConn);
            stopService(serviceStop);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.i("activity", "BACK button pressed");
        moveTaskToBack(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // there is only one activity we call.. so no checks are added here
        setDistanceParameters();
    }

    // onClickListener overrides
    @Override
    public void onClick(View v) {
        if (v.getId() == R.drawable.button) {
            // for "start/stop calculating" button
            isCalculatingDistance = !isCalculatingDistance;
            if (isCalculatingDistance) {
                //when START is clicked
                locationService.start();
                locationService.addListener(this);
            } else {
                //when STOP is clicked
                SharedPreferences defPref = PreferenceManager.getDefaultSharedPreferences(this);
                boolean autoReport = defPref.getBoolean(DistanceCalculatorPrefActivity.PREF_KEY_AUTO_REPORT, false);
                Log.i("activity", "auto report is: " + autoReport);
                
                boolean autoSave = false;
                DistanceCalcReport report = null;
                if(autoReport) {
                    autoSave = defPref.getBoolean(DistanceCalculatorPrefActivity.PREF_KEY_AUTO_SAVE, false);
                    report = getSummaryReport();
                }
                Log.i("activity", "auto save is: " + autoSave);
                
                locationService.removeListener(this);
                locationService.stop();
                
                if(report != null) {
                    startReportActivity(report, autoSave);
                }
            }
            setActivityState();
        } else if (v.getId() == R.drawable.pause && isCalculatingDistance) {
            // pause/resume only makes sense if system is calculating distance
            isPaused = !isPaused;
            Button pauseButton = (Button) v;
            if (isPaused) {
                pauseButton.setText(R.string.resume);
                locationService.pause();
            } else {
                pauseButton.setText(R.string.pause);
                locationService.resume();
                errorText.setText(R.string.service_temp_not_available);
            }
        }
    }
    
    /**
     * starts the report activity for given report object. if given report object is null, error message
     * is displayed and nothing else is done
     * 
     * @param report object containing information required to display report activity
     * @param saveWhenRendered will save the report automatically when rendered fully on screen
     */
    private void startReportActivity(DistanceCalcReport report, boolean saveWhenRendered) {
        if (report == null) {
            Toast.makeText(this, getString(R.string.reportGenerationError), Toast.LENGTH_LONG).show();
            return;
        }
        Intent reportIntent = new Intent("action.distancecalculator.REPORT");
        reportIntent.putExtra(DistanceCalculatorReportActivity.INTENT_PARAM_AUTO_REPORT, report);
        reportIntent.putExtra(DistanceCalculatorReportActivity.INTENT_PARAM_AUTO_SAVE, saveWhenRendered);
        startActivity(reportIntent);
        Log.i("activity", "Generate report selected");
    }
    
    /**
     * obtains summary report object for this session. returns null if report can't be generated for some reason
     * 
     * @return report object for this session
     */
    private DistanceCalcReport getSummaryReport() {
        Log.i("mainactivity", "generating report");
        // obtains report object from service and populates strings as per user preferences
        DistanceCalcReport report = locationService.getSummaryReport();
        if (report == null) {
            return null;
        }
     
        report.setTotalDistanceString(DistanceCalculatorUtilities.getDistanceForReport(report.getTotalDistance(), multiplier, distanceSuffix));
        report.setTotalTimeString(DistanceCalculatorUtilities.getTimeForReport(report.getTotalTime()));
        report.setTotalTimePausedString(DistanceCalculatorUtilities.getTimeForReport(report.getTotalTimePaused()));
        report.setMinSpeedString(DistanceCalculatorUtilities.getSpeedForReport(report.getMinSpeed(), multiplier, distanceSuffix, hoursStr));
        report.setMaxSpeedString(DistanceCalculatorUtilities.getSpeedForReport(report.getMaxSpeed(), multiplier, distanceSuffix, hoursStr));
        report.setAvgSpeed(DistanceCalculatorUtilities.getAverageSpeedForReport(report.getTotalDistance(), report.getTotalTime(), multiplier,
                distanceSuffix, hoursStr));

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        report.setCurrentTime(sdf.format(System.currentTimeMillis()));
        return report;
    }

    @Override
    public void onAdClicked(int arg0) {
        Log.w("ad", "sense ad is clicked");
    }

    @Override
    public void onAdFailedToLoad(int arg0) {
        Log.w("ad", "sense ad failed to load");
    }

    @Override
    public void onAdReceived(int arg0) {
        Log.w("ad", "sense ad received");
    }
}
