package com.proper.wifimonitor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proper.data.adapters.WifiEntryAdapter;
import com.proper.data.diagnostics.WifiLogEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lebel on 18/02/2015.
 */
public class ActLogView extends Activity {
    private SharedPreferences prefs = null;
    private Button btnExit;
    private ListView lvLogs;
    private WifiEntryAdapter adapter = null;
    private WifiLogEntry selectedEntry;
    private List<WifiLogEntry> entries = new ArrayList<WifiLogEntry>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lyt_logview);
        btnExit = (Button) this.findViewById(R.id.btnExitActLogView);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        lvLogs = (ListView) this.findViewById(R.id.lvLogList);
        lvLogs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listItemClicked(parent, view, position, id);
            }
        });
        //get List Data
        entries = getLogs();
        if(!entries.isEmpty()) {
            adapter = new WifiEntryAdapter(this, entries);
            lvLogs.setAdapter(adapter);
        }
    }

    private void listItemClicked(AdapterView<?> parent, View view, int position, long id) {
        if (entries != null && !entries.isEmpty()) {
            selectedEntry = entries.get(position);
            lvLogs.setItemChecked(position, true);
            view.setSelected(true);
        }
    }

    private List<WifiLogEntry> getLogs() {
        List<WifiLogEntry> entries = null;
        prefs = getSharedPreferences("WIFILOGS", MODE_PRIVATE);
        if (prefs.contains("LOG")){
            String logString = prefs.getString("LOG", "");
            ObjectMapper mapper = new ObjectMapper();
            if (!logString.isEmpty()) {
                try {
                    entries = mapper.readValue(logString,new TypeReference<List<WifiLogEntry>>(){});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return entries;
    }

    private void buttonClicked(View v) {
        if (v == btnExit) {
            Intent i = new Intent();
            setResult(0, i);
            finish();
        }
    }
}