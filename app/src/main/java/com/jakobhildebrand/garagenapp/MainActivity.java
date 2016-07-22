package com.jakobhildebrand.garagenapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    private SharedPreferences prefs;
    private String hostname;
    private int port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        hostname = prefs.getString("pref_hostname", "");
        port = Integer.parseInt(prefs.getString("pref_port", ""));
        new RetrieveFeedTask(hostname, port).execute();
        //TODO: Check if connected to Wifi and server is accessible
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onRefresh(){
        new RetrieveFeedTask(hostname, port).execute();
    }

    public void toggleGarage(View view){
        //TODO: Implement volley request to yoctohub
        //TODO: Implement singleton for Volley RequestQueue
    }

    private boolean isWifiOnAndConnected() {
        if (prefs.getBoolean("pref_use_mobile", true)) return true;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        } else {
            return false;
        }
    }

    class RetrieveFeedTask extends AsyncTask<Void, Void, Void> {
        private Exception exception;
        private boolean isServerAccessible;
        private String hostname;
        private int port;

        public RetrieveFeedTask(String hostname, int port){
            this.hostname = hostname;
            this.port = port;
        }
        protected Void doInBackground(Void... params){
            try {
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(this.hostname, this.port);
                socket.connect(socketAddress, 1000);
                socket.close();
                isServerAccessible = true;
            } catch (Exception e) {
                this.exception = e;
                isServerAccessible = false;
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            ImageButton myButton = (ImageButton) findViewById(R.id.imageButton);
            if (isWifiOnAndConnected() && isServerAccessible) {
                myButton.setEnabled(true);
            } else {
                myButton.setEnabled(false);
            }
        }
    }
}
