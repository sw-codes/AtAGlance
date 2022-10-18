package com.swcode.ataglance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView textViewDayOfWeek;
    TextView textViewGreeting;
    private TextView textViewWeatherMain;
    TextView textViewWeatherDescription;
    LocalDateTime localDateTime;

    ImageView imageViewWeatherIcon;

    LocationManager locationManager;
    LocationListener locationListener;

    Double deviceLongitude;
    Double deviceLatitude;

    WeatherDownloadTask task;

    ListView listView;
    ArrayAdapter arrayAdapter;
    ArrayList<String> newsTitles = new ArrayList<>();
    ArrayList<String> newsContent = new ArrayList<>();

    public void setTextViewWeatherMain(String weatherMain) {
        this.textViewWeatherMain.setText(weatherMain);
    }

    public void setTextViewWeatherDescription(String weatherDescription) {
        this.textViewWeatherDescription.setText(weatherDescription);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        localDateTime = LocalDateTime.now();



        findViews();
        getDeviceLocation();
        setGreeting();

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, newsTitles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", newsContent.get(i));

                startActivity(intent);
            }
        });

        Log.i("weather api string", "https://api.openweathermap.org/data/2.5/weather?lat="+deviceLatitude+"&lon="+deviceLongitude+"&appid=0f46cad5db2d5a3d270ea221ef07f373");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }


        NewsDownloadTask task = new NewsDownloadTask();
        task.execute("http://api.mediastack.com/v1/news?access_key=8a11f200718e0d8fabd962beeab7e13e&languages=en&sources=bbc");

//        getWeatherData();



    }

    public void setGreeting() {
        if (localDateTime.getHour() >= 0 && localDateTime.getHour() < 12) {
            this.textViewGreeting.setText("Good Morning!");
        } else {
            this.textViewGreeting.setText("Good Afternoon!");
        }

        this.textViewDayOfWeek.setText("Today is " + localDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "!");
    }

    public void findViews() {
        this.textViewDayOfWeek = findViewById(R.id.textViewDayOfWeek);
        this.textViewGreeting = findViewById(R.id.textViewGreeting);
        this.textViewWeatherMain = findViewById(R.id.textViewWeatherMain);
        this.textViewWeatherDescription = findViewById(R.id.textViewWeatherDescription);
        this.imageViewWeatherIcon = findViewById(R.id.imageViewWeatherIcon);
        this.listView = findViewById(R.id.listViewNewsArticles);
    }

    public void getDeviceLocation() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.i("location: ", location.toString());

                deviceLongitude = location.getLongitude();
                deviceLatitude = location.getLatitude();

                Log.i("longitude", String.valueOf(deviceLongitude));
                Log.i("latitude", String.valueOf(deviceLatitude));

                getWeatherData(location.getLongitude(), location.getLatitude());
            }
        };
    }

    public void getWeatherData(Double deviceLongitudeNum, Double deviceLatitudeNum) {
        task = new WeatherDownloadTask();
        Log.i("deviceLongitudeNum", String.valueOf(deviceLongitudeNum));
        Log.i("deviceLatitudeNum", String.valueOf(deviceLatitudeNum));

        String deviceLongitudeNumFormatted = String.valueOf(deviceLongitudeNum);
        String deviceLatitudeNumFormatted = String.valueOf(deviceLatitudeNum);

        Log.i("api string inside getWeatherData", "https://api.openweathermap.org/data/2.5/weather?lat="+deviceLatitudeNumFormatted+"&lon="+deviceLongitudeNumFormatted+"&appid=0f46cad5db2d5a3d270ea221ef07f373");

        String weatherApiUrl = "https://api.openweathermap.org/data/2.5/weather?lat="+deviceLatitudeNumFormatted+"&lon="+deviceLongitudeNumFormatted+"&appid=0f46cad5db2d5a3d270ea221ef07f373";
        task.execute(weatherApiUrl);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }
    }

    public class WeatherDownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            String weatherIconCode = "";

            Log.i("json", s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                String weatherInfo = jsonObject.getString("weather");
//                Log.i("weather info: ", weatherInfo);

                JSONArray arr = new JSONArray(weatherInfo);

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject jsonPart = arr.getJSONObject(i);
//                    Log.i("main", jsonPart.getString("main"));
                    setTextViewWeatherMain(jsonPart.getString("main"));
//                    Log.i("description", jsonPart.getString("description"));
                    setTextViewWeatherDescription(jsonPart.getString("description"));

                    weatherIconCode = jsonPart.getString("icon");
                    Log.i("weather icon code: ", weatherIconCode);

                    switch (weatherIconCode) {
                        case "04d":
                            imageViewWeatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_broken_clouds_day));
                            break;
                        case "10d":
                            imageViewWeatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_rain_day));
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class NewsDownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Chrome");
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                System.out.println("response code: " + responseCode);
                if (responseCode != 200) {
                    System.out.println("error reading web page");
                }

                BufferedReader inputReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String line;
                while ((line = inputReader.readLine()) != null) {
                    System.out.println(line);
                    result += line;
                }
                inputReader.close();

                return result;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
//        return toReturn;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try {
                JSONObject jsonObject = new JSONObject(s);

                String data = jsonObject.getString("data");
                Log.i("data: ", data);

                JSONArray array = new JSONArray(data);
                Log.i("array length: ", String.valueOf(array.length()));
                for (int i = 0; i < 5; i++) {
                    JSONObject jsonPart = array.getJSONObject(i);

                    Log.i("title: ", jsonPart.getString("title"));
                    Log.i("url: ", jsonPart.getString("url"));

                    newsTitles.add(jsonPart.getString("title"));
                    newsContent.add(jsonPart.getString("url"));

                    arrayAdapter.notifyDataSetChanged();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}