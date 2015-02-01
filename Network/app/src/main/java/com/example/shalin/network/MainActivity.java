package com.example.shalin.network;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity {
    static EditText etResponse;
    TextView tvConnected;

    private GoogleMap map;

    static Button sethome;
    static Button press;

    GPSTracker gps;

    static int check = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        //etResponse = (EditText) findViewById(R.id.response);
        tvConnected = (TextView) findViewById(R.id.connected);
        press = (Button) findViewById(R.id.open);
        press.setEnabled(false);
        // press.setText("");
        sethome = (Button) findViewById(R.id.home);

        //etResponse.setKeyListener(null);

        gps = new GPSTracker(MainActivity.this);

        if (gps.canGetLocation()) {
            double cur_latitude = gps.getLatitude();
            double cur_longitude = gps.getLongitude();
            LatLng cur_location = new LatLng(cur_latitude, cur_longitude);
            map.addMarker(new MarkerOptions().position(cur_location).title("Current Location"));
        } else {
            gps.showSettingsAlert();
        }

        press.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DoorCheck();
                if (ConnectionTest()) {
                    tvConnected.setText("Connected");
                    tvConnected.setTextColor(Color.GREEN);
                } else {
                    tvConnected.setText("Not Connected");
                    tvConnected.setTextColor(Color.RED);
                }
                new HttpAsyncTask().execute("http://freedns.afraid.org/api/?action=getdyndns&sha=65d9b1d2ec7ce1269cc0732893f75f155f45a342&style=xml");
            }
        });

        sethome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sethome.setText("        Set Home       ");
                check = 1;
                map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng point) {

                        if (check == 1) {
                            map.clear();
                            map.addMarker(new MarkerOptions().position(point).title("Home"));
                            sethome.setText("    Change Home    ");
                            //Toast.makeText(getApplicationContext(), point.toString(), Toast.LENGTH_SHORT).show();
                            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(point, 8);
                            map.animateCamera(update);

                            gps = new GPSTracker(MainActivity.this);

                            if (gps.canGetLocation()) {
                                double cur_latitude = gps.getLatitude();
                                double cur_longitude = gps.getLongitude();
                                LatLng cur_location = new LatLng(cur_latitude, cur_longitude);
                                map.addMarker(new MarkerOptions().position(cur_location).title("Current Location"));
                                //Toast.makeText(getApplicationContext(), "Your location is -\nLat: " + cur_latitude + "\nLong: " + cur_longitude, Toast.LENGTH_LONG).show();
                                double length = Distance(point, cur_latitude, cur_longitude);
                                //Toast.makeText(getApplicationContext(), "Length: " + length, Toast.LENGTH_LONG).show();

                                if ((length * 1000) > 50) {
                                    press.setEnabled(false);
                                    //press.setText("");
                                } else {
                                    press.setEnabled(true);
                                }
                            } else {
                                gps.showSettingsAlert();
                            }
                        }
                        check = 0;
                    }
                });
            }
        });
    }

    public static void DoorCheck() {
        if (press.getText() == "     Open Door      ") {
            press.setText("    Close Door      ");
        } else {
            press.setText("     Open Door      ");
        }
    }

    public static double Distance(LatLng point, double cur_latitude, double cur_longitude) {
        double lat1 = point.latitude;
        double lat2 = cur_latitude;
        double lon1 = point.longitude;
        double lon2 = cur_longitude;
        int Radius = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));

        return Radius * c;
    }

    public static String GetURL(String url) {
        InputStream is = null;
        String message = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();

            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            is = httpResponse.getEntity().getContent();

            if (is != null) {
                message = IStoS(is);
            } else {
                message = "Did not work!";
            }

        } catch (IOException e) {
        }
        return message;
    }

    private static String IStoS(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String read = "";
        String message = "";

        while ((read = br.readLine()) != null) {
            message += read;
        }
        is.close();
        return message;
    }

    public boolean ConnectionTest() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return GetURL(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();
            ///etResponse.setText(result);

            int index = result.indexOf("<address>");

            if (index == -1) {
                //etResponse.setText("Not Found");
            } else {
                String ip = result.substring(99, 113);
                //etResponse.setText(ip);

                try {
                    SocketConnection(ip);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void SocketConnection(String ip) throws IOException {
            String serverName = "localhost";
            int port = 4321;
            byte ch;
            String msg = "";

            Socket client = null;
            InetAddress ian = null;
            OutputStream outToServer = null;
            DataOutputStream out = null;
            InputStream inFromServer = null;
            DataInputStream in = null;

            try {
                ian = InetAddress.getByName(ip);

                //etResponse.setText("Connecting to:" + serverName + " on port:" + port);

                client = new Socket(ian, port);

                //etResponse.setText("Just connected to: " + client.getRemoteSocketAddress());

                outToServer = client.getOutputStream();

                out = new DataOutputStream(outToServer);

                inFromServer = client.getInputStream();

                in = new DataInputStream(inFromServer);

                while (true) {
                    ch = in.readByte();

                    if (ch == 10) {
                        break;
                    } else {
                        msg += (char) ch;
                    }
                }
                etResponse.setText(msg);
                out.writeBytes(msg + "\r");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}