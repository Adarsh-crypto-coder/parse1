package com.example.parse;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        // Fetch data from the URL
        new FetchDataTask().execute("https://fetch-hiring.s3.amazonaws.com/hiring.json");
    }

    private class FetchDataTask extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... urls) {
            List<String> resultList = new ArrayList<>();

            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                try {
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream()));

                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }

                    // Parse JSON response
                    JSONArray jsonArray = new JSONArray(stringBuilder.toString());
                    resultList = processJsonArray(jsonArray);

                    bufferedReader.close();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return resultList;
        }

        @Override
        protected void onPostExecute(List<String> resultList) {
            // Update UI with the result
            adapter.addAll(resultList);
        }
    }

    private List<String> processJsonArray(JSONArray jsonArray) throws JSONException {
        List<String> resultList = new ArrayList<>();

        // Group items by "listId"
        Map<Integer, List<String>> groupedItems = new HashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            String name = item.optString("name", "");

            // Filter out items with blank or null "name"
            if (!name.isEmpty()) {
                int listId = item.optInt("listId", -1);
                groupedItems.computeIfAbsent(listId, k -> new ArrayList<>()).add(name);
            }
        }

        // Sort results by "listId" and then by "name"
        for (Map.Entry<Integer, List<String>> entry : groupedItems.entrySet()) {
            Collections.sort(entry.getValue(), Comparator.naturalOrder());
            resultList.add("ListId: " + entry.getKey() + "\n" + String.join("\n", entry.getValue()));
        }

        Collections.sort(resultList);
        return resultList;
    }
}
