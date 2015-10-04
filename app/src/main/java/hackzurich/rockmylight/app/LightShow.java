package hackzurich.rockmylight.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import hackzurich.rockmylight.app.util.SntpClient;
import hackzurich.rockmylight.app.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import 	java.io.InputStreamReader;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class LightShow extends Activity {

    // ##### DRAW vars ######

    final Handler drawHandler = new Handler();
    protected ColorView contentView;
    protected TextView activeUsers;
    protected TextView nextUpdateIn;
    protected TextView textTime;
    // color drawing
    private Timer timerColor;
    private boolean runOffline = false;
    private long nextColorAt;
    private long dt = 0;
    // GPS
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected double latitude;
    protected double longitude;
    // Light sequence
    protected StepsBuffer stepsBuffer;
    // Communication
    protected ServerCommunication communication;
    protected int serverCheckInterval = 3000;
    //protected String baseURL = "http://www.rockmylight.com/api/dj/";
    //protected String baseURL = "https://dl.dropboxusercontent.com/u/12073958/example_pattern.json";
    //protected String baseURL = "http://rockmylight.com/api/dj/1/";
    protected String baseURL = "http://rockmylight.com/api/dj/auto/";
    protected String deviceID;


    // ######  ANDROID ######

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_light_show);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        contentView = (ColorView) findViewById(R.id.fullscreen_content);
        activeUsers = (TextView) findViewById(R.id.userCount);
        final Switch switchRunOffline = (Switch) findViewById(R.id.switch_run_offline);
        nextUpdateIn = (TextView) findViewById(R.id.text_next_frame);
        textTime = (TextView) findViewById(R.id.text_time);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        //syncTime();

        SharedPreferences settings = getSharedPreferences("rockmylight", 0);
        dt = settings.getLong("dt_delay", 0);

        stepsBuffer = new StepsBuffer();

        // GPS
        locationManager = (LocationManager) getSystemService(getApplicationContext().LOCATION_SERVICE);
        locationListener = new GPSListener(this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        //Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


        // get device id
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        deviceID = tm.getDeviceId();

        // button listeners
        switchRunOffline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                runOffline = isChecked;
                stepsBuffer.removeStale();
            }
        });

        //updateMusicServer();

        // reoccurring requests from the server
        new Timer().scheduleAtFixedRate(askServerForBuffer(), 0, serverCheckInterval);
        

        // color playback!
        timerColor = new Timer();
        nextColorAt = getTime();
        timerColor.scheduleAtFixedRate(new ColorTask(), 1000, 10); //schedule(new ColorTask(), 1000);

        showMsg("dt = " + dt);
        //showMsg("deviceID = "+Double.valueOf(deviceID));
        setLocation(Double.valueOf(deviceID), 0.0);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    // ######### CUSTOM METHODS #########


    public void setLocation(double latitudeNew, double longitudeNew) {
        showMsg("lat: "+latitudeNew+"\nlon: "+longitudeNew);
        this.latitude = latitudeNew;
        this.longitude = longitudeNew;
    }

    public void setDt(long dt){
        this.dt = dt;
        Log.i("time", "time sync with dt="+dt);
        SharedPreferences settings = getSharedPreferences("rockmylight", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("dt_delay", dt);
        editor.commit();
    }

    class ColorTask extends TimerTask {
        public void run() {
            drawHandler.post(colorUpdateRunnable);
        }
    }

    private boolean lockThread = false;

    final Runnable colorUpdateRunnable = new Runnable() {
        public void run() {
            if(!lockThread) {
                lockThread = true;
                showTime();
                long timeDiff = nextColorAt - getTime();
                if (timeDiff <= 0) {
                    replaceColors();
                } else if (timeDiff <= 15) { // wait // locks things!!!
                    while (nextColorAt - getTime() > 0) {
                    }
                    replaceColors();
                }
                lockThread = false;
            }
        }
    };

    public void replaceColors(){
        // get color from buffer if there is any
        LightStep ls = stepsBuffer.getNext();
        if (ls == null || runOffline) {
            localColors();
        } else { // there intel from the server
            scheduleColor(stepsBuffer.getNextTimestamp()); // massive hack with abs!!!
            //sendMsg("scheduled in "+(stepsBuffer.getNextTimestamp() - System.currentTimeMillis()));
            contentView.setColor(Color.parseColor(ls.getColor()));
        }
        try {
            activeUsers.setText("Lights around you: " + stepsBuffer.getDevicesInNetwork());
            contentView.invalidate();

        } catch (Exception e) {
            Log.e("draw", "screen update failed!");
        }
    }

    public int randomColor(){
        return Color.rgb(
                (int)(Math.random()*256),
                (int)(Math.random()*256),
                (int)(Math.random()*256)  );
    }

    public String queryURL() {
        return baseURL + "?id=" + deviceID + "&lat=" + latitude + "&lon=" + longitude ;
    }

    public TimerTask askServerForBuffer() {
        return new TimerTask() {
            @Override
            public void run() {
                new ServerCommunication(stepsBuffer).execute(queryURL());
            }
        };
    }

    private void localColors(){
        scheduleColor( getTime()/1000 *1000 +1000 );
        contentView.setColor(randomColor());
        // todo add execution timer
    }

    public void showMsg(String st){
        Toast.makeText(getApplicationContext(), st, Toast.LENGTH_SHORT).show();
    }

    public void scheduleColor(long at){
        long dt = at - getTime(); // synchronised?
        long in = Math.abs(dt) < 10000 ? Math.abs(dt) : 10000;
        //timerColor.schedule( new ColorTask(), in );
        nextColorAt = at;
        nextUpdateIn.setText("Next update in (ms): " + in);
    }

    public void offColor(){

    }

    public void showTime(){
        textTime.setText(""+ (System.currentTimeMillis() / 1000 % 60));
    }

    public long getTime(){
        return System.currentTimeMillis() - dt;
    }







    public void syncTime(){
        if( isOnline() ) {
            final Resources res = this.getResources();
            final int id = Resources.getSystem().getIdentifier(
                    "config_ntpServer", "string", "android");
            final String defaultServer = res.getString(id);
            SntpClient server = new SntpClient();
            server.requestTime(defaultServer, 0); //hack: must be on the same network!
            setDt((server.getNtpTime() - System.currentTimeMillis())%1000   );
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    // dropbox file server crashes?
    public void updateMusicServer() {
        if(isOnline()) {

            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet("https://dl.dropboxusercontent.com/u/12073958/music_server.txt");
                HttpResponse response = client.execute(request);

                String html = "";
                InputStream in = response.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder str = new StringBuilder();
                String line = null;
                while((line = reader.readLine()) != null)
                {
                    str.append(line);
                }
                in.close();
                baseURL = str.toString().trim();
            } catch (Exception e){}
        }
    }
}
