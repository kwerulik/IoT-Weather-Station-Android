package com.example.iotweatherstation;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChartActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView tvChartTitle;
    private String fieldToFetch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        lineChart = findViewById(R.id.lineChart);
        tvChartTitle = findViewById(R.id.tvChartTitle);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        lineChart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                Toast.makeText(ChartActivity.this, "Odczyt: " + e.getY(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected() {
            }
        });

        String title = getIntent().getStringExtra("CHART_TITLE");
        fieldToFetch = getIntent().getStringExtra("FIELD_NAME");

        if (title != null) {
            tvChartTitle.setText(title);
        }

        fetchChartData();
    }

    private void fetchChartData() {
        String url = "https://api.thingspeak.com/channels/3334045/feeds.json?results=20";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray feeds = response.getJSONArray("feeds");
                        List<Entry> entries = new ArrayList<>();

                        for (int i = 0; i < feeds.length(); i++) {
                            JSONObject feed = feeds.getJSONObject(i);
                            String valueStr = feed.optString(fieldToFetch, "0");

                            try {
                                float val = Float.parseFloat(valueStr);
                                entries.add(new Entry(i, val));
                            } catch (NumberFormatException ignored) {}
                        }

                        if (!entries.isEmpty()) {
                            LineDataSet dataSet = new LineDataSet(entries, "Ostatnie pomiary");
                            dataSet.setColor(android.graphics.Color.BLUE);
                            dataSet.setLineWidth(3f);
                            dataSet.setDrawCircles(true);
                            dataSet.setCircleColor(android.graphics.Color.RED);
                            dataSet.setDrawValues(false);

                            LineData lineData = new LineData(dataSet);

                            if ("field3".equals(fieldToFetch)) {
                                com.github.mikephil.charting.components.LimitLine warningLine = new com.github.mikephil.charting.components.LimitLine(400f, "Próg ostrzegawczy");
                                warningLine.setLineColor(android.graphics.Color.RED);
                                warningLine.setLineWidth(2f);
                                warningLine.setTextColor(android.graphics.Color.RED);
                                warningLine.setTextSize(12f);
                                warningLine.enableDashedLine(15f, 10f, 0f); // Przerywana linia

                                lineChart.getAxisLeft().removeAllLimitLines(); // Czyścimy stare linie
                                lineChart.getAxisLeft().addLimitLine(warningLine);

                                // TO SĄ TE DWIE KLUCZOWE LINIJKI: Wymuszamy wysokość osi na 450!
                                lineChart.getAxisLeft().setAxisMaximum(450f);
                                lineChart.getAxisLeft().setAxisMinimum(0f);

                            } else {
                                // Jeśli to inny wykres, usuwamy linię...
                                lineChart.getAxisLeft().removeAllLimitLines();
                                // ... i pozwalamy osiom znów dobierać się automatycznie
                                lineChart.getAxisLeft().resetAxisMaximum();
                                lineChart.getAxisLeft().resetAxisMinimum();
                            }

                            lineChart.setData(lineData);
                            lineChart.getDescription().setEnabled(false);
                            lineChart.invalidate(); // "Zmuś" wykres do odświeżenia i narysowania
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Błąd pobierania wykresu", Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }
}