package com.jakobhildebrand.garagenapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YRelay;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class MainActivity extends AppCompatActivity{
    private SharedPreferences prefs;
    private String hostname;
    private int port;
    private String relayName;
    private SwipeRefreshLayout swipeLayout;
    private YRelay mRelay;
    private ImageButton myButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mRelay = null;
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setRefreshing(true);
                new RetrieveFeedTask(hostname, port, relayName).execute();
                swipeLayout.setRefreshing(false);
            }
        });
        swipeLayout.setColorSchemeColors(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        myButton = (ImageButton) findViewById(R.id.imageButton);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.isEnabled()) {
                    toggleGarage();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        hostname = prefs.getString("pref_hostname", "");
        port = Integer.parseInt(prefs.getString("pref_port", ""));
        relayName = prefs.getString("pref_relayname", "");
        new RetrieveFeedTask(hostname, port, relayName).execute();
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

    public void toggleGarage(){
        new OpenGarageTask().execute();
    }

    private boolean isWifiOnAndConnected() {
        if (prefs.getBoolean("pref_use_mobile", true)) return true;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI);
    }

    @Override
    protected void onStop(){
        super.onStop();
        YAPI.FreeAPI();
    }

    //Checks if connected to wifi and if server is available
    class RetrieveFeedTask extends AsyncTask<Void, Void, Void> {
        private Exception exception;
        private boolean isServerAccessible;
        private String hostname;
        private int port;
        private String relayName;

        public RetrieveFeedTask(String hostname, int port, String relayName){
            this.hostname = hostname;
            this.port = port;
            this.relayName = relayName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //myButton.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... params){
            try {
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(this.hostname, this.port);
                socket.connect(socketAddress, 1000);
                socket.close();
                if(mRelay == null) {
                    YAPI.RegisterHub(hostname + ":" + port + "/");
                    mRelay = YRelay.FindRelay(relayName);
                }
                isServerAccessible = true;
            } catch (Exception e) {
                this.exception = e;
                e.printStackTrace();
                isServerAccessible = false;
                mRelay = null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            if (isWifiOnAndConnected() && isServerAccessible) {
                myButton.setEnabled(true);
            } else {
                myButton.setEnabled(false);
            }
        }
    }

    class OpenGarageTask extends AsyncTask<Void,Void,Void>{
        @Override
        public Void doInBackground(Void... params){
            if(mRelay != null){
                if(mRelay.isOnline()) {
                    Log.i("Reached:", "relay is online");
                    try {
                        mRelay.pulse(500);
                    } catch (YAPI_Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Relay is not online", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Couldn't reach relay", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v){
        }
    }
}
