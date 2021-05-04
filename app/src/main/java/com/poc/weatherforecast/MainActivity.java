package com.poc.weatherforecast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements LocationListener {

    private TextView txtLat, temperature, feels_like, min_max, humidityvalue, pressurevalue, logitude, latitude, timezone, visibility;
    private TextView description, windspeed, sunrise, sunset, locationvalue, cloudvalue, datevalue, dayvalue;
    LocationManager locationManager;
    double latitudevalue, longitudevalue;
    Timer timer;
    Date date;

    @Override
    protected void onStart() {
        super.onStart();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //Checking the permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        if (checkLocationPermission()) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 1, this);
        }
    }

    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperature = (TextView) findViewById(R.id.temperature);
        feels_like = (TextView) findViewById(R.id.feels_like);
        min_max = (TextView) findViewById(R.id.min_max);
        humidityvalue = (TextView) findViewById(R.id.humidityvalue);
        pressurevalue = (TextView) findViewById(R.id.pressurevalue);
        latitude = (TextView) findViewById(R.id.latitude);
        logitude = (TextView) findViewById(R.id.longitude);
        timezone = (TextView) findViewById(R.id.timezonevalue);
        visibility = (TextView) findViewById(R.id.visibilityvalue);
        description = (TextView) findViewById(R.id.description);
        windspeed = (TextView) findViewById(R.id.windvalue);
        sunrise = (TextView) findViewById(R.id.sunrisevalue);
        sunset = (TextView) findViewById(R.id.sunsetvalue);
        locationvalue = (TextView) findViewById(R.id.location);
        cloudvalue = (TextView) findViewById(R.id.cloudvalue);
        datevalue = (TextView) findViewById(R.id.date);
        dayvalue = (TextView) findViewById(R.id.day);

        datevalue.setText(DateFormat.getDateInstance().format(new Date()));
        Calendar calendar = Calendar.getInstance();
        date = calendar.getTime();
        dayvalue.setText(new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date.getTime()));

        //Making API Call for every 2 Hours
        timer = new Timer ();
        TimerTask hourlyTask = new TimerTask() {
            @Override
            public void run () {
                // your code here...
                api_request(String.valueOf(latitudevalue), String.valueOf(longitudevalue));
            }
        };

        timer.schedule (hourlyTask, 0,TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS));

    }

    public boolean checkLocationPermission() {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permission granted", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

        latitudevalue = location.getLatitude();
        longitudevalue = location.getLongitude();

        latitude.setText("Latitude - " + String.format("%.2f", latitudevalue));
        logitude.setText("Longitude - " + String.format("%.2f", longitudevalue));

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitudevalue, longitudevalue, 1);

            String stateName = addresses.get(0).getAdminArea();
            String countryName = addresses.get(0).getCountryName();
            locationvalue.setText(stateName + ", " + countryName);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    public void api_request(String latitudevalue, String longitudevalue) {

        //API Call
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        String url = "https:api.openweathermap.org/data/2.5/weather?lat=" + latitudevalue + "&lon=" + longitudevalue + "&appid=5ad7218f2e11df834b0eaf3a33a39d2a";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {

            //Response from API
            Log.d("home", response.toString());
            try {

                JSONObject main = response.getJSONObject("main");
                temperature.setText(getCelsius(main.getString("temp")));
                feels_like.setText("Feels_Like: " + getCelsius(main.getString("feels_like")));
                String temp_min = getCelsius(main.getString("temp_min"));
                String temp_max = getCelsius(main.getString("temp_max"));
                min_max.setText("Min: " + temp_min + " - " + "Max: " + temp_max);
                pressurevalue.setText(main.getString("pressure") + "hpa");
                humidityvalue.setText(main.getString("humidity") + "%");

                timezone.setText(response.getString("timezone"));
                visibility.setText(response.getString("visibility"));

                JSONArray wind1 = response.getJSONArray("weather");
                JSONObject a = wind1.getJSONObject(0);
                description.setText("Description: " + a.getString("description"));

                JSONObject wind = response.getJSONObject("wind");
                windspeed.setText(wind.getString("speed") + " m/s");

                JSONObject sys = response.getJSONObject("sys");
                sunrise.setText(getTime(sys.getString("sunrise")));
                sunset.setText(getTime(sys.getString("sunset")));

                JSONObject clouds = response.getJSONObject("clouds");
                cloudvalue.setText(clouds.getString("all"));

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }, error -> error.printStackTrace());
        requestQueue.add(request);
    }

    public String getCelsius(String kelvin) {

        //converting kelvin to celsius
        Double value = Double.parseDouble(kelvin) - 273.15;
        int roundedvalue = (int) Math.rint(value);

        return roundedvalue + "\u2103";
    }

    public String getTime(String unix_time) {

        //converting unix_time to time
        long unix_seconds = Long.parseLong(unix_time);
        Date datevalue = new Date(unix_seconds * 1000L);
        SimpleDateFormat jdf = new SimpleDateFormat("HH:mm:ss z");

        return jdf.format(datevalue);
    }
}
