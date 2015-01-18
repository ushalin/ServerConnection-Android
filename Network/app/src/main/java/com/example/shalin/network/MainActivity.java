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

public class MainActivity extends Activity {
    EditText etResponse;
    TextView tvConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        etResponse = (EditText) findViewById(R.id.response);
        tvConnected = (TextView) findViewById(R.id.connected);
        Button press = (Button) findViewById(R.id.open);

        press.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectionTest()) {
                    tvConnected.setText("Connected");
                } else {
                    tvConnected.setText("Not Connected");
                }
                new HttpAsyncTask().execute("http://freedns.afraid.org/api/?action=getdyndns&sha=65d9b1d2ec7ce1269cc0732893f75f155f45a342&style=xml");
            }
        });
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
                etResponse.setText("Not Found");
            } else {
                String ip = result.substring(99, 113);
                //.setText(ip);

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