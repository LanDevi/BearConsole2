package com.altimeter.bdureau.bearconsole.telemetry;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.LocationService;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.Config3DR;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AltimeterStatus extends AppCompatActivity {
    private static ViewPager mViewPager;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    public LocationBroadCastReceiver receiver = null;
    SectionsStatusPageAdapter adapter;
    Tab1StatusFragment statusPage1 = null;
    Tab2StatusFragment statusPage2 = null;
    Tab3StatusFragment statusPage3 = null;

    Marker marker, markerDest;
    Polyline polyline1 = null;
    public Double rocketLatitude = 48.8698;
    public Double rocketLongitude = 2.2190;
    LatLng dest = new LatLng(rocketLatitude, rocketLongitude);

    Button btnDismiss, btnRecording;
    private ConsoleApplication myBT;
    Thread altiStatus;
    boolean status = true;
    boolean recording = false;
    public String TAG = "AltimeterStatus.class";


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 9:
                    // Value 9 contains the output 1 status
                    statusPage1.setOutput1Status((String) msg.obj);
                    break;
                case 10:
                    // Value 10 contains the output 2 status
                    statusPage1.setOutput2Status((String) msg.obj);
                    break;
                case 11:
                    // Value 11 contains the output 3 status
                    statusPage1.setOutput3Status((String) msg.obj);
                    break;
                case 12:
                    //Value 12 contains the output 4 status
                    statusPage1.setOutput4Status((String) msg.obj);
                    break;
                case 1:
                    String myUnits;
                    //Value 1 contains the current altitude
                    if (myBT.getAppConf().getUnits().equals("0"))
                        //Meters
                        myUnits = getResources().getString(R.string.Meters_fview);
                    else
                        //Feet
                        myUnits = getResources().getString(R.string.Feet_fview);
                    if (statusPage1 != null)
                        statusPage1.setAltitude((String) msg.obj + " " + myUnits);
                    break;
                case 13:
                    //Value 13 contains the battery voltage
                    String voltage = (String) msg.obj;
                    if (voltage.matches("\\d+(?:\\.\\d+)?")) {
                        statusPage1.setVoltage(voltage + " Volts");
                    } else {
                        statusPage1.setVoltage("NA");
                    }
                    break;
                case 14:
                    //Value 14 contains the temperature
                    statusPage1.setTemperature((String) msg.obj + "°C");
                    break;

                case 15:
                    //Value 15 contains the EEprom usage
                    statusPage1.setEEpromUsage((String) msg.obj + " %");
                    break;

                case 16:
                    //Value 16 contains the number of flight
                    statusPage1.setNbrOfFlight((String) msg.obj);
                    break;
                case 18:
                    //Value 18 contains the latitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String latitude = (String) msg.obj;
                        if (latitude.matches("\\d+(?:\\.\\d+)?")) {
                            double latitudeVal = Double.parseDouble(latitude) / 100000;
                            statusPage2.setLatitudeValue("" + latitudeVal);
                        }
                    }
                    break;
                case 19:
                    //Value 19 contains the longitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String longitude = (String) msg.obj;
                        if (longitude.matches("\\d+(?:\\.\\d+)?")) {
                            double longitudeVal = Double.parseDouble(longitude) / 100000;
                            statusPage2.setLongitudeValue("" + longitudeVal);
                        }
                    }
                    break;
                case 20:
                    //Value 20 contains the number of satellites
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String nbrOfSatellite = (String) msg.obj;
                        if (nbrOfSatellite.matches("\\d+(?:\\.\\d+)?")) {
                            int nbrOfSatelliteVal = Integer.parseInt(nbrOfSatellite);
                            statusPage2.setSatellitesVal("" + nbrOfSatelliteVal);
                        }
                    }
                    break;
                case 21:
                    //Value 21 contains the hdop
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String hdop = (String) msg.obj;
                        if (hdop.matches("\\d+(?:\\.\\d+)?")) {
                            int hdopVal = Integer.parseInt(hdop);
                            statusPage2.setHdopVal("" + hdopVal);
                        }
                    }
                    break;
                case 22:
                    //Value 22 contains the location age
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String locationAge = (String) msg.obj;
                        if (locationAge.matches("\\d+(?:\\.\\d+)?")) {
                            int locationAgeVal = Integer.parseInt(locationAge);
                            statusPage2.setLocationAgeValue("" + locationAgeVal);
                        }
                    }
                    break;
                case 23:
                    //Value 23 contains the GPS altitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String GPSAltitude = (String) msg.obj;
                        if (GPSAltitude.matches("\\d+(?:\\.\\d+)?")) {
                            int GPSAltitudeVal = Integer.parseInt(GPSAltitude);
                            statusPage2.setGPSAltitudeVal("" + GPSAltitudeVal);
                        }
                    }
                    break;
                case 24:
                    // Value 24 contains the GPS Speed
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String GPSSpeed = (String) msg.obj;
                        if (GPSSpeed.matches("\\d+(?:\\.\\d+)?")) {
                            int GPSSpeedVal = Integer.parseInt(GPSSpeed);
                            statusPage2.setGPSSpeedVal("" + GPSSpeedVal);
                        }
                    }
                    break;
                case 25:
                    // Value 25 contains the time for sat acquisition
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String TimeSat = (String) msg.obj;
                        if (TimeSat.matches("\\d+(?:\\.\\d+)?")) {
                            int TimeSatVal = Integer.parseInt(TimeSat);
                            statusPage2.setTimeSatValue(String.format("%.2f", (double) (TimeSatVal / 1000)) + " secs");
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        myBT = (ConsoleApplication) getApplication();
        receiver = new LocationBroadCastReceiver();
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                startService();
            }
        } else {
            startService();
        }
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_altimeter_status);
        if (myBT.getAppConf().getRocketLatitude().matches("\\d+(?:\\.\\d+)?"))
            rocketLatitude = Double.parseDouble(myBT.getAppConf().getRocketLatitude());

        if (myBT.getAppConf().getRocketLongitude().matches("\\d+(?:\\.\\d+)?"))
            rocketLongitude = Double.parseDouble(myBT.getAppConf().getRocketLongitude());
        mViewPager = (ViewPager) findViewById(R.id.container);

        setupViewPager(mViewPager);

        myBT.setHandler(handler);
        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnRecording = (Button) findViewById(R.id.butRecording);
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS") && myBT.getAppConf().getManualRecording())
            btnRecording.setVisibility(View.VISIBLE);
        else
            btnRecording.setVisibility(View.INVISIBLE);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnDismiss()");
                if (recording) {
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Confirm Exit Recording")
                            .setMessage("Recording in progress... Are you sure you want to exit?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "about to Stop recording !!!!");
                                    /*if (status) {
                                        status = false;
                                        myBT.write("h;\n".toString());
                                        myBT.setExit(true);
                                        myBT.clearInput();
                                        myBT.flush();
                                    }*/
                                    //turn off telemetry
                                    /*myBT.flush();
                                    myBT.clearInput();
                                    myBT.write("y0;\n".toString());*/

                                    //exit recording if running
                                    if (recording) {
                                        recording = false;
                                        myBT.flush();
                                        myBT.clearInput();
                                        myBT.write("w0;\n".toString());
                                        myBT.clearInput();
                                        myBT.flush();
                                        Log.d(TAG, "Stopped recording !!!!");
                                        msg("Stopped recording");
                                    }
                                    finish();      //exit the  activity
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    if (status) {
                        status = false;
                        myBT.write("h;\n".toString());
                        myBT.setExit(true);
                        myBT.clearInput();
                        myBT.flush();
                    }
                    //turn off telemetry
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y0;\n".toString());
                    finish();      //exit the  activity
                }
            }
        });

        btnRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (recording) {
                    recording = false;
                    myBT.write("w0;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                    msg("Stopped recording");
                } else {
                    recording = true;
                    myBT.write("w1;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                    msg("Started recording");
                }

            }
        });


        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!status) break;
                    myBT.ReadResult(10000);
                }
            }
        };

        altiStatus = new Thread(r);
        altiStatus.start();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        //super.onBackPressed();
        if (recording) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Exit Recording")
                    .setMessage("Recording in progress... Are you sure you want to exit?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            //turn off telemetry
                           /* myBT.flush();
                            myBT.clearInput();
                            myBT.write("y0;\n".toString());*/

                            //exit recording if running
                            if (recording) {
                                recording = false;
                                myBT.flush();
                                myBT.clearInput();
                                myBT.write("w0;\n".toString());
                                myBT.clearInput();
                                myBT.flush();
                                msg("Stopped recording");
                            }
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            //turn off telemetry
            myBT.flush();
            myBT.clearInput();
            myBT.write("y0;\n".toString());
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        //switch off output
        if (statusPage1.switchOutput1.isChecked()) {
            myBT.write("k1F;\n".toString());
            myBT.clearInput();
            myBT.flush();
        }
        if (statusPage1.switchOutput2.isChecked()) {
            myBT.write("k2F;\n".toString());
            myBT.clearInput();
            myBT.flush();
        }
        if (statusPage1.switchOutput3.isChecked()) {
            myBT.write("k3F;\n".toString());
            myBT.clearInput();
            myBT.flush();
        }
        if (statusPage1.switchOutput4.isChecked()) {
            myBT.write("k4F;\n".toString());
            myBT.clearInput();
            myBT.flush();
        }
        //turn off telemetry
        myBT.flush();
        myBT.clearInput();
        myBT.write("y0;\n".toString());

        //exit recording if running
        if (recording) {
            recording = false;
            myBT.flush();
            myBT.clearInput();
            myBT.write("w0;\n".toString());
            myBT.clearInput();
            myBT.flush();
            msg("Stopped recording");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (myBT.getConnected() && !status) {
            myBT.flush();
            myBT.clearInput();

            myBT.write("y1;".toString());
            status = true;
            //altiStatus = new Thread(r);
            //altiStatus.stop();
            /*if(altiStatus != null)
                altiStatus.start();*/
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        if (!status) break;
                        myBT.ReadResult(10000);
                    }
                }
            };
            altiStatus = new Thread(r);
            altiStatus.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");

        if (recording) {
            recording = false;
            myBT.write("w0;\n".toString());
            myBT.clearInput();
            myBT.flush();
        }

        if (status) {
            status = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
            //finish();
        }


        myBT.flush();
        myBT.clearInput();
        myBT.write("h;\n".toString());
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        String myMessage = "";
        long timeOut = 10000;
        long startTime = System.currentTimeMillis();

        myMessage = myBT.ReadResult(10000);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsStatusPageAdapter(getSupportFragmentManager());
        statusPage1 = new Tab1StatusFragment(myBT);
        statusPage2 = new Tab2StatusFragment(myBT);
        statusPage3 = new Tab3StatusFragment(myBT/*, mMap*/);

        adapter.addFragment(statusPage1, "TAB1");
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            adapter.addFragment(statusPage2, "TAB2");
            adapter.addFragment(statusPage3, "TAB3");
        }

        linearDots = findViewById(R.id.idAltiStatusLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(viewListener);

    }

    public void agregaIndicateDots(int pos, int nbr) {
        dotsSlide = new TextView[nbr];
        linearDots.removeAllViews();

        for (int i = 0; i < dotsSlide.length; i++) {
            dotsSlide[i] = new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if (dotsSlide.length > 0) {
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            agregaIndicateDots(i, adapter.getCount());
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

    public class SectionsStatusPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsStatusPageAdapter(FragmentManager fm) {
            super(fm);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }

    public static class Tab1StatusFragment extends Fragment {
        private static final String TAG = "Tab1StatusFragment";
        private boolean ViewCreated = false;
        private TextView txtStatusAltiName, txtStatusAltiNameValue;
        private TextView txtViewOutput1Status, txtViewOutput2Status, txtViewOutput3Status, txtViewOutput4Status;
        private TextView txtViewAltitude, txtViewVoltage, txtViewLink, txtTemperature, txtEEpromUsage, txtNbrOfFlight;
        private TextView txtViewOutput4, txtViewBatteryVoltage, txtViewOutput3, txtViewEEprom, txtViewFlight;
        ConsoleApplication lBT;
        private Switch switchOutput1, switchOutput2, switchOutput3, switchOutput4;

        public Tab1StatusFragment(ConsoleApplication bt) {
            lBT = bt;
        }

        public void setOutput1Status(String value) {
            if (ViewCreated)
                this.txtViewOutput1Status.setText(outputStatus(value));
        }

        public void setOutput2Status(String value) {
            if (ViewCreated)
                this.txtViewOutput2Status.setText(outputStatus(value));
        }

        public void setOutput3Status(String value) {
            if (ViewCreated)
                this.txtViewOutput3Status.setText(outputStatus(value));
        }

        public void setOutput4Status(String value) {
            if (ViewCreated)
                this.txtViewOutput4Status.setText(outputStatus(value));
        }

        public void setAltitude(String value) {
            if (ViewCreated)
                this.txtViewAltitude.setText(value);
        }

        public void setVoltage(String value) {
            if (ViewCreated)
                this.txtViewVoltage.setText(value);
        }

        public void setTemperature(String value) {
            if (ViewCreated)
                this.txtTemperature.setText(value);
        }

        public void setEEpromUsage(String value) {
            if (ViewCreated)
                this.txtEEpromUsage.setText(value);
        }

        public void setNbrOfFlight(String value) {
            if (ViewCreated)
                this.txtNbrOfFlight.setText(value);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_altimeter_status_tab1, container, false);

            txtStatusAltiName = (TextView) view.findViewById(R.id.txtStatusAltiName);
            txtStatusAltiNameValue = (TextView) view.findViewById(R.id.txtStatusAltiNameValue);
            txtViewOutput1Status = (TextView) view.findViewById(R.id.txtViewOutput1Status);
            txtViewOutput2Status = (TextView) view.findViewById(R.id.txtViewOutput2Status);
            txtViewOutput3Status = (TextView) view.findViewById(R.id.txtViewOutput3Status);
            txtViewOutput4Status = (TextView) view.findViewById(R.id.txtViewOutput4Status);
            txtViewAltitude = (TextView) view.findViewById(R.id.txtViewAltitude);
            txtViewVoltage = (TextView) view.findViewById(R.id.txtViewVoltage);
            txtViewLink = (TextView) view.findViewById(R.id.txtViewLink);
            txtViewOutput3 = (TextView) view.findViewById(R.id.txtViewOutput3);
            txtViewOutput4 = (TextView) view.findViewById(R.id.txtViewOutput4);
            txtViewBatteryVoltage = (TextView) view.findViewById(R.id.txtViewBatteryVoltage);
            txtTemperature = (TextView) view.findViewById(R.id.txtViewTemperature);
            txtEEpromUsage = (TextView) view.findViewById(R.id.txtViewEEpromUsage);
            txtNbrOfFlight = (TextView) view.findViewById(R.id.txtViewNbrOfFlight);
            txtViewEEprom = (TextView) view.findViewById(R.id.txtViewEEprom);
            txtViewFlight = (TextView) view.findViewById(R.id.txtViewFlight);

            txtStatusAltiNameValue.setText(lBT.getAltiConfigData().getAltimeterName());
            if (lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                txtViewVoltage.setVisibility(View.VISIBLE);
                txtViewBatteryVoltage.setVisibility(View.VISIBLE);
            } else {
                txtViewVoltage.setVisibility(View.INVISIBLE);
                txtViewBatteryVoltage.setVisibility(View.INVISIBLE);
            }
            if (!lBT.getAltiConfigData().getAltimeterName().equals("AltiDuo")) {
                txtViewOutput3Status.setVisibility(View.VISIBLE);
                txtViewOutput3.setVisibility(View.VISIBLE);
            } else {
                txtViewOutput3Status.setVisibility(View.INVISIBLE);
                txtViewOutput3.setVisibility(View.INVISIBLE);
            }

            if (lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
                txtViewOutput4Status.setVisibility(View.VISIBLE);
                txtViewOutput4.setVisibility(View.VISIBLE);
            } else {
                txtViewOutput4Status.setVisibility(View.INVISIBLE);
                txtViewOutput4.setVisibility(View.INVISIBLE);
            }
            //hide eeprom
            if (lBT.getAltiConfigData().getAltimeterName().equals("AltiDuo")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
                txtViewEEprom.setVisibility(View.INVISIBLE);
                txtViewFlight.setVisibility(View.INVISIBLE);
                txtEEpromUsage.setVisibility(View.INVISIBLE);
                txtNbrOfFlight.setVisibility(View.INVISIBLE);
            } else {
                txtViewEEprom.setVisibility(View.VISIBLE);
                txtViewFlight.setVisibility(View.VISIBLE);
                txtEEpromUsage.setVisibility(View.VISIBLE);
                txtNbrOfFlight.setVisibility(View.VISIBLE);
            }

            txtViewLink.setText(lBT.getConnectionType());

            switchOutput1 = (Switch) view.findViewById(R.id.switchOutput1);
            switchOutput2 = (Switch) view.findViewById(R.id.switchOutput2);
            switchOutput3 = (Switch) view.findViewById(R.id.switchOutput3);
            switchOutput4 = (Switch) view.findViewById(R.id.switchOutput4);
            if (!lBT.getAltiConfigData().getAltimeterName().equals("AltiDuo"))
                switchOutput3.setVisibility(View.VISIBLE);
            else
                switchOutput3.setVisibility(View.INVISIBLE);
            if (lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32") ||
                    lBT.getAltiConfigData().getAltimeterName().equals("AltiServo") ||
                    lBT.getAltiConfigData().getAltimeterName().equals("AltiGPS"))
                switchOutput4.setVisibility(View.VISIBLE);
            else
                switchOutput4.setVisibility(View.INVISIBLE);

            switchOutput1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (switchOutput1.isChecked())
                        lBT.write("k1T;\n".toString());
                    else
                        lBT.write("k1F;\n".toString());

                    lBT.flush();
                    lBT.clearInput();
                }
            });


            switchOutput1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (switchOutput1.isChecked())
                        lBT.write("k1T;\n".toString());
                    else
                        lBT.write("k1F;\n".toString());

                    lBT.flush();
                    lBT.clearInput();

                }
            });

            switchOutput2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (switchOutput2.isChecked())
                        lBT.write("k2T;\n".toString());
                    else
                        lBT.write("k2F;\n".toString());

                    lBT.flush();
                    lBT.clearInput();
                }
            });
            switchOutput2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (switchOutput2.isChecked())
                        lBT.write("k2T;\n".toString());
                    else
                        lBT.write("k2F;\n".toString());

                    lBT.flush();
                    lBT.clearInput();

                }
            });
            switchOutput3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (switchOutput3.isChecked())
                        lBT.write("k3T;\n".toString());
                    else
                        lBT.write("k3F;\n".toString());

                    lBT.flush();
                    lBT.clearInput();
                }
            });
            switchOutput3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (switchOutput3.isChecked())
                        lBT.write("k3T;\n".toString());
                    else
                        lBT.write("k3F;\n".toString());

                    lBT.flush();
                    lBT.clearInput();

                }
            });
            switchOutput4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (switchOutput4.isChecked())
                        lBT.write("k4T;\n".toString());
                    else
                        lBT.write("k4F;\n".toString());

                    lBT.flush();
                    lBT.clearInput();
                }
            });
            switchOutput4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (switchOutput4.isChecked())
                        lBT.write("k4T;\n".toString());
                    else
                        lBT.write("k4F;\n".toString());

                    lBT.flush();
                    lBT.clearInput();

                }
            });
            ViewCreated = true;
            return view;
        }

        @Override
        public void onDestroyView() {

            super.onDestroyView();
            ViewCreated = false;
        }

        public boolean isViewCreated() {
            return ViewCreated;
        }

        private String outputStatus(String msg) {
            String res = "";
            switch (msg) {
                case "0":
                    res = getResources().getString(R.string.no_continuity);
                    break;
                case "1":
                    res = this.getContext().getResources().getString(R.string.continuity);
                    break;
                case "-1":
                    res = getResources().getString(R.string.disabled);
                    break;
            }
            return res;
        }
    }

    public static class Tab2StatusFragment extends Fragment {
        private static final String TAG = "Tab2StatusFragment";
        private boolean ViewCreated = false;
        private TextView txtViewLatitude, txtViewLongitude, txtViewLatitudeValue, txtViewLongitudeValue;
        private TextView txtViewSatellitesVal, txtViewHdopVal, txtViewGPSAltitudeVal, txtViewGPSSpeedVal;
        private TextView txtViewLocationAgeValue, txtViewTimeSatValue;
        ConsoleApplication lBT;

        public Tab2StatusFragment(ConsoleApplication bt) {
            lBT = bt;
        }

        public void setLatitudeValue(String value) {
            if (ViewCreated)
                this.txtViewLatitudeValue.setText(value);
        }

        public void setLongitudeValue(String value) {
            if (ViewCreated)
                this.txtViewLongitudeValue.setText(value);
        }

        public void setSatellitesVal(String value) {
            if (ViewCreated)
                this.txtViewSatellitesVal.setText(value);
        }

        public void setHdopVal(String value) {
            if (ViewCreated)
                this.txtViewHdopVal.setText(value);
        }

        public void setGPSAltitudeVal(String value) {
            if (ViewCreated)
                this.txtViewGPSAltitudeVal.setText(value);
        }

        public void setGPSSpeedVal(String value) {
            if (ViewCreated)
                this.txtViewGPSSpeedVal.setText(value);
        }

        public void setLocationAgeValue(String value) {
            if (ViewCreated)
                this.txtViewLocationAgeValue.setText(value);
        }

        public void setTimeSatValue(String value) {
            if (ViewCreated)
                this.txtViewTimeSatValue.setText(value);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_altimeter_status_tab2, container, false);

            txtViewLatitude = (TextView) view.findViewById(R.id.txtViewLatitude);
            txtViewLongitude = (TextView) view.findViewById(R.id.txtViewLongitude);
            txtViewLatitudeValue = (TextView) view.findViewById(R.id.txtViewLatitudeValue);
            txtViewLongitudeValue = (TextView) view.findViewById(R.id.txtViewLongitudeValue);
            txtViewSatellitesVal = (TextView) view.findViewById(R.id.txtViewSatellitesVal);
            txtViewHdopVal = (TextView) view.findViewById(R.id.txtViewHdopVal);
            txtViewGPSAltitudeVal = (TextView) view.findViewById(R.id.txtViewGPSAltitudeVal);
            txtViewGPSSpeedVal = (TextView) view.findViewById(R.id.txtViewGPSSpeedVal);
            txtViewLocationAgeValue = (TextView) view.findViewById(R.id.txtViewLocationAgeValue);
            txtViewTimeSatValue = (TextView) view.findViewById(R.id.txtViewTimeSatValue);


            //hide GPS
           /* if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")){
                txtViewLatitude.setVisibility(View.VISIBLE);
                txtViewLongitude.setVisibility(View.VISIBLE);
                txtViewLatitudeValue.setVisibility(View.VISIBLE);
                txtViewLongitudeValue.setVisibility(View.VISIBLE);
            } else {
                txtViewLatitude.setVisibility(View.INVISIBLE);
                txtViewLongitude.setVisibility(View.INVISIBLE);
                txtViewLatitudeValue.setVisibility(View.INVISIBLE);
                txtViewLongitudeValue.setVisibility(View.INVISIBLE);
            }*/
            ViewCreated = true;
            return view;
        }
    }

    public static class Tab3StatusFragment extends Fragment {
        private static final String TAG = "Tab3StatusFragment";
        private boolean ViewCreated = false;

        public GoogleMap lMap = null;
        ConsoleApplication lBT;
        Button butBack, butShareMap;

        public Tab3StatusFragment(ConsoleApplication bt) {
            lBT = bt;
        }

        public GoogleMap getlMap() {
            return lMap;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_altimeter_status_tab3, container, false);

            butBack = (Button) view.findViewById(R.id.butBack);
            butShareMap = (Button) view.findViewById(R.id.butShareMap);
            SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapStatus);

            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if (lMap == null) {
                        lMap = googleMap;
                        lMap.setMapType(Integer.parseInt(lBT.getAppConf().getMapType()));
                    }
                }
            });

            butBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mViewPager.setCurrentItem(0);

                }
            });
            butShareMap.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    takeMapScreenshot();
                }
            });
            ViewCreated = true;
            return view;
        }
        private void takeMapScreenshot() {
            GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                Bitmap bitmap;

                @Override
                public void onSnapshotReady(Bitmap snapshot) {
                    // Callback is called from the main thread, so we can modify the ImageView safely.
                    bitmap = snapshot;
                    shareScreenshot(bitmap);
                }
            };
            lMap.snapshot(callback);
        }

        private void shareScreenshot(Bitmap bitmap) {
            try {
                // Save the screenshot to a file
                String filePath = MediaStore.Images.Media.insertImage(this.getContext().getContentResolver(),
                        bitmap, "Title", null);
                Uri fileUri = Uri.parse(filePath);
                // Share the screenshot
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/*");
                share.putExtra(Intent.EXTRA_STREAM, fileUri);
                startActivity(Intent.createChooser(share, "Share Map screenshot"));
            } catch (Exception e) {
                //Toast.makeText(this, "Error saving/sharing Map screenshot", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    public class LocationBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("coordinate", intent.getAction());
            if (intent.getAction().equals("ACT_LOC")) {
                double latitude = intent.getDoubleExtra("latitude", 0f);
                double longitude = intent.getDoubleExtra("longitude", 0f);
                Log.d("coordinate", "latitude is:" + latitude + " longitude is: " + longitude);

                if (statusPage3.getlMap() != null) {
                    LatLng latLng = new LatLng(latitude, longitude);
                    if (marker != null) {
                        marker.setPosition(latLng);
                    } else {
                        BitmapDescriptor manIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_person_map);
                        marker = statusPage3.getlMap().addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(latLng).icon(manIcon));
                    }

                    if (markerDest != null) {
                        markerDest.setPosition(dest);
                    } else {
                        BitmapDescriptor rocketIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_rocket_map);
                        markerDest = statusPage3.getlMap().addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(dest).icon(rocketIcon));
                    }
                    List<LatLng> coord;
                    coord = new ArrayList();

                    dest = new LatLng(rocketLatitude, rocketLongitude);
                    coord.add(0, dest);

                    coord.add(1, latLng);
                    if (polyline1 == null)
                        polyline1 = statusPage3.getlMap().addPolyline(new PolylineOptions().clickable(false));
                    //Get the line color from the config
                    polyline1.setColor(myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getMapColor())));
                    polyline1.setPoints(coord);
                    if (statusPage3.getlMap().getCameraPosition().zoom > 10)
                        statusPage3.getlMap().animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, statusPage3.getlMap().getCameraPosition().zoom));
                    else
                        statusPage3.getlMap().animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
            }
        }
    }

    private void startService() {

        IntentFilter filter = new IntentFilter("ACT_LOC");
        registerReceiver(receiver, filter);

        Intent intent = new Intent(AltimeterStatus.this, LocationService.class);
        startService(intent);
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

}
