package com.basudev.openweathermap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    TextView address, updated_at, status, temperature, temp_min, temp_max, sunrise, sunset, winds, pressures, humiditys;
    EditText city_name;
    ImageView search,iconss;
    ResultReceiver resultReceiver;
    String loca, city;
    String  desc, icon;
    static double lati, longi;
    String URL = "https://openweathermap.org/img/wn/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * Initialization
         */
        resultReceiver = new AddressResultReceiver(new Handler());
        address = findViewById(R.id.address);
        iconss = findViewById(R.id.icon);
        updated_at = findViewById(R.id.updated_at);
        status = findViewById(R.id.status);
        temperature = findViewById(R.id.temp);
        temp_min = findViewById(R.id.temp_min);
        temp_max = findViewById(R.id.temp_max);
        sunrise = findViewById(R.id.sunrise);
        sunset = findViewById(R.id.sunset);
        winds = findViewById(R.id.wind);
        pressures = findViewById(R.id.pressure);
        humiditys = findViewById(R.id.humidity);
        city_name = findViewById(R.id.city_name);
        search = findViewById(R.id.search);

        /**
         * Calling Different City Weather Report Search Function
         */

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                city = Objects.requireNonNull(city_name.getText()).toString();
                WeatherForOtherCity(city);
            }
        });

        /**
         * Calling Current Location Function
         */

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission is denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
//        progressBar.setVisibility(View.VISIBLE);
        com.google.android.gms.location.LocationRequest locationRequest = new com.google.android.gms.location.LocationRequest();
        locationRequest.setInterval(100000000);
//        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback() {

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(getApplicationContext())
                                .removeLocationUpdates(this);
                        if (locationResult != null && locationResult.getLocations().size() > 0) {
                            int latestlocIndex = locationResult.getLocations().size() - 1;
                            lati = locationResult.getLocations().get(latestlocIndex).getLatitude();
                            longi = locationResult.getLocations().get(latestlocIndex).getLongitude();
                            Location location = new Location("providerNA");
                            location.setLongitude(longi);
                            location.setLatitude(lati);
                            WeatherForCurrentLocation(longi, lati);
                            fetchaddressfromlocation(location);

                        }
                    }
                }, Looper.getMainLooper());

    }

    private class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == Constants.SUCCESS_RESULT) {
                loca = resultData.getString(Constants.LOCALITY);
            } else {
                Toast.makeText(MainActivity.this, resultData.getString(Constants.RESULT_DATA_KEY), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchaddressfromlocation(Location location) {
        Intent intent = new Intent(this, FetchAddressIntentServices.class);
        intent.putExtra(Constants.RECEVIER, resultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        startService(intent);
    }

    /**
     * OpenWeatherMap API Calling Function USING Volley For Current Location Weather Report by Latitude and Longitude
     */

    private void WeatherForCurrentLocation(double longitude, double latitude) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&units=metric&appid=4ab93491f4bba58cda3263863085eefc",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String name = jsonObject.getString("name");
                            JSONObject sys = jsonObject.getJSONObject("sys");
                            String country = sys.getString("country");
                            long rise = sys.getLong("sunrise");
                            long set = sys.getLong("sunset");
                            JSONArray weather = jsonObject.getJSONArray("weather");
                            for (int i = 0; i <= weather.length(); i++) {
                                JSONObject desc1 = weather.getJSONObject(0);
                                desc = desc1.getString("description");
                                icon = desc1.getString("icon");
                            }
                            JSONObject main = jsonObject.getJSONObject("main");
                            String temp = main.getString("temp");
                            String min = main.getString("temp_min");
                            String max = main.getString("temp_max");
                            String press = main.getString("pressure");
                            String humid = main.getString("humidity");
                            JSONObject wind = jsonObject.getJSONObject("wind");
                            String speed = wind.getString("speed");
                            long updatedAt = jsonObject.getLong("dt");
                            String updatedAtText = "Updated at: " + new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(new Date(updatedAt * 1000));
                            findViewById(R.id.mainContainer).setVisibility(View.VISIBLE);
                            address.setText(name+", "+country);
                            updated_at.setText(updatedAtText);
                            Glide.with(getApplicationContext())
                                    .load(URL+icon+"@2x.png")
                                    .into(iconss);
                            status.setText(desc.toUpperCase(Locale.ROOT));
                            temperature.setText(temp+"°C");
                            temp_min.setText("Min Temp: "+min+"°C");
                            temp_max.setText("Max Temp: "+max+"°C");
                            sunrise.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date(rise * 1000)));
                            sunset.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date(set * 1000)));
                            winds.setText(speed+" m/s");
                            pressures.setText(press+" hPa");
                            humiditys.setText(humid+" %");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                        System.out.println(error.getMessage());
                    }
                });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    /**
     * OpenWeatherMap API Calling Function USING Volley For Different Location Weather Report by City Name
     */

    private void WeatherForOtherCity(String City) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://api.openweathermap.org/data/2.5/weather?q="+City+"&units=metric&appid=4ab93491f4bba58cda3263863085eefc",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String name = jsonObject.getString("name");
                            JSONObject sys = jsonObject.getJSONObject("sys");
                            String country = sys.getString("country");
                            long rise = sys.getLong("sunrise");
                            long set = sys.getLong("sunset");
                            JSONArray weather = jsonObject.getJSONArray("weather");
                            for (int i = 0; i <= weather.length(); i++) {
                                JSONObject desc1 = weather.getJSONObject(0);
                                desc = desc1.getString("description");
                                icon = desc1.getString("icon");
                            }
                            JSONObject main = jsonObject.getJSONObject("main");
                            String temp = main.getString("temp");
                            String min = main.getString("temp_min");
                            String max = main.getString("temp_max");
                            String press = main.getString("pressure");
                            String humid = main.getString("humidity");
                            JSONObject wind = jsonObject.getJSONObject("wind");
                            String speed = wind.getString("speed");
                            long updatedAt = jsonObject.getLong("dt");
                            String updatedAtText = "Updated at: " + new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(new Date(updatedAt * 1000));
                            findViewById(R.id.mainContainer).setVisibility(View.VISIBLE);
                            address.setText(name+","+country);
                            updated_at.setText(updatedAtText);
                            Glide.with(getApplicationContext())
                                    .load(URL+icon+"@2x.png")
                                    .into(iconss);
                            status.setText(desc.toUpperCase(Locale.ROOT));
                            temperature.setText(temp+"°C");
                            temp_min.setText("Min Temp: "+min+"°C");
                            temp_max.setText("Max Temp: "+max+"°C");
                            sunrise.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date(rise * 1000)));
                            sunset.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date(set * 1000)));
                            winds.setText(speed+" m/s");
                            pressures.setText(press+" hPa");
                            humiditys.setText(humid+" %");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                        System.out.println(error.getMessage());
                    }
                });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }
    
}