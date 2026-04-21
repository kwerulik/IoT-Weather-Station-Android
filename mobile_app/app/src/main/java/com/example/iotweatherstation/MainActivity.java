package com.example.iotweatherstation;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private TextView tvTemp, tvWilg, tvAir, tvPress;
    private Button btnRefresh, btnInfo;
    private float currentTemp = 0f;
    private float currentHum = 0f;
    private int currentAir = 0;
    private static final String CHANNEL_ID = "meteo_alerts";
    private final String URL = "https://api.thingspeak.com/channels/3334045/feeds.json?results=1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTemp = findViewById(R.id.tvTemp);
        tvWilg = findViewById(R.id.tvWilg);
        tvAir = findViewById(R.id.tvAir);
        tvPress = findViewById(R.id.tvPress);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnInfo = findViewById(R.id.btnInfo);

        androidx.cardview.widget.CardView cardTemp = findViewById(R.id.cardTemp);
        androidx.cardview.widget.CardView cardWilg = findViewById(R.id.cardWilg);
        androidx.cardview.widget.CardView cardAir = findViewById(R.id.cardAir);
        androidx.cardview.widget.CardView cardPress = findViewById(R.id.cardPress);

        if (cardTemp != null) cardTemp.setOnClickListener(v -> openChart("Historia Temperatury", "field1"));
        if (cardWilg != null) cardWilg.setOnClickListener(v -> openChart("Historia Wilgotności", "field2"));
        if (cardAir != null) cardAir.setOnClickListener(v -> openChart("Historia Jakości Powietrza", "field3"));
        if (cardPress != null) cardPress.setOnClickListener(v -> openChart("Historia Ciśnienia", "field4"));

        if (btnRefresh != null) btnRefresh.setOnClickListener(v -> fetchData());
        if (btnInfo != null) btnInfo.setOnClickListener(v -> showEnvironmentInfo());

        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        fetchData();
    }

    private void fetchData() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, URL, null, response -> {
                    try {
                        JSONArray feeds = response.getJSONArray("feeds");

                        if (feeds.length() > 0) {
                            JSONObject latestData = feeds.getJSONObject(0);

                            String t = latestData.optString("field1", "Brak");
                            String h = latestData.optString("field2", "Brak");
                            String a = latestData.optString("field3", "Brak");
                            String p = latestData.optString("field4", "Brak");

                            if (tvTemp != null) tvTemp.setText(t + " °C");
                            if (tvWilg != null) tvWilg.setText(h + " %");
                            if (tvAir != null) tvAir.setText(a);
                            if (tvPress != null) tvPress.setText(p + " hPa");

                            try {
                                currentTemp = Float.parseFloat(t);
                                if (currentTemp < 19){
                                    sendAlert("ALARM TEMPERATURY", "Wykryto za niską temperaturę: " + currentTemp + " °C");
                                } else if (currentTemp > 25) {
                                    sendAlert("ALARM TEMPERATURY", "Wykryto za wysoką temperaturę: " + currentTemp + " °C");
                                }
                            } catch (Exception ignored) {}

                            try {
                                currentHum = Float.parseFloat(h);
                                if(currentHum < 40) {
                                    sendAlert("ALARM WILGOTNOŚCI", "Wykryto za niską wilgotność: " + currentHum);
                                } else if (currentHum > 60) {
                                    sendAlert("ALARM WILGOTNOŚCI", "Wykryto za wysoką wilgotność: " + currentHum);
                                }
                            } catch (Exception ignored) {}

                            try {
                                currentAir = Integer.parseInt(a);
                                if (currentAir > 1200) {
                                    sendAlert("ALARM JAKOŚCI POWIETRZA", "Wykryto wysokie zanieczyszczenie: " + currentAir);
                                }
                            } catch (Exception ignored) {}

                            Toast.makeText(MainActivity.this, "Zaktualizowano dane!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Błąd odczytu JSON", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    Toast.makeText(MainActivity.this, "Błąd sieci! Sprawdź internet.", Toast.LENGTH_SHORT).show();
                });

        queue.add(jsonObjectRequest);
    }

    private void openChart(String title, String fieldName) {
        Intent intent = new Intent(this, ChartActivity.class);
        intent.putExtra("CHART_TITLE", title);
        intent.putExtra("FIELD_NAME", fieldName);
        startActivity(intent);
    }

    private void showEnvironmentInfo() {
        StringBuilder info = new StringBuilder();

        info.append("🌡️ Temperatura: ");
        if (currentTemp < 19) info.append("Zimno. Warto podkręcić ogrzewanie.\n\n");
        else if (currentTemp <= 24) info.append("Optymalna.\n\n");
        else info.append("Gorąco! Otwórz okno lub włącz wiatrak.\n\n");

        info.append("💧 Wilgotność: ");
        if (currentHum < 40) info.append("Za sucho. Użyj nawilżacza powietrza.\n\n");
        else if (currentHum <= 60) info.append("W normie.\n\n");
        else info.append("Za wysoka Przewietrz pokój!\n\n");

        info.append("🍃 Powietrze: ");
        if (currentAir < 750) info.append("Czyste, bardzo dobre warunki.\n");
        else if (currentAir <= 1200) info.append("Umiarkowane.\n");
        else info.append("ZŁE! Zamknij okna i włącz oczyszczacz!\n");

        new AlertDialog.Builder(this)
                .setTitle("Raport o środowisku")
                .setMessage(info.toString())
                .setPositiveButton("Zamknij", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Alerty pogodowe", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    private void sendAlert(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(1, builder.build());
        }
    }
}