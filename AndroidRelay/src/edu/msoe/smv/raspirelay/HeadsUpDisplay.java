/**
 * Project: Raspberry Pi Data Hub
 * Date: 10/25/14
 */

package edu.msoe.smv.raspirelay;

/**
 * @author austin
 * @version 0.0001
 */

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * the main activity
 */
public class HeadsUpDisplay extends Activity {

    // layout references
    public TextView console;
    public TextView webConnected, piConnected;

    // old
    public WebClientConnectionAgent webAgent;
    private ServerSocket piServerSocket;
    //    public WebListener myWebListener;
    public PiListener myPiListener;
    public boolean isPiDataServerRunning = true;
    private long startTime;

    // new connection controllers
    private VehicleConnectionAgent vehicleConnectionAgent = null;
    private boolean vehicleConnectionBound = false;

    private WebClientConnectionAgent webClientConnectionAgent = null;
    private boolean clientConnectionHandlerBound = false;

    /**
     *
     */
    private ServiceConnection connection = new ServiceConnection() {
        /**
         *
         * @param name
         * @param service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // compare classnames so we can reuse this class for both client and vehicle connections
            if (name.getClass().equals(VehicleConnectionAgent.class)) {
                // we have a vehicle service connection
                vehicleConnectionAgent = ((VehicleConnectionAgent.VehicleConnectionServiceBinder) service).getService();
                vehicleConnectionBound = true;
            } else if (name.getClass().equals(WebClientConnectionAgent.class)) {
                // TODO
                // we have a client service connection agent
//                webClientConnectionAgent = ((WebClientConnectionAgent.ClientConnectionServiceBinder) service).getBinder();
//                clientConnectionHandlerBound = true;
            }
            // else do nothing
        }

        /**
         *
         * @param name
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (name.getClass().equals(VehicleConnectionAgent.class)) {
                vehicleConnectionAgent = null;
                vehicleConnectionBound = false;
            } else if (name.getClass().equals(WebClientConnectionAgent.class)) {
                webClientConnectionAgent = null;
                clientConnectionHandlerBound = false;
            }
            // else do nothing
        }
    };
    //just to supply the server with random data
    Thread randomDataThread;
    public void changeContinuousRandomDataThread(View v) {
        CheckBox running=(CheckBox)findViewById(R.id.sendContinuousCB);
        if(running.isChecked()) {
            randomDataThread = new Thread(new Runnable() {
                public void run() {
                    CheckBox running=(CheckBox)findViewById(R.id.sendContinuousCB);
                    while(running.isChecked()) {
                        sendRandomData();
                        try {
                            Thread.sleep(250);
                        } catch (Exception e) {

                        }
                    }
                }
            });
            randomDataThread.start();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.headsupdisplay);

        startTime = SystemClock.currentThreadTimeMillis();
//        String hostName = "155.92.179.102";

        console = (TextView) findViewById(R.id.console);

        // get public IP from Wifi Manager
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        updateConsole("my ip: " + ip);

        // get connection status displays
        webConnected = (TextView) findViewById(R.id.webConnected);
        piConnected = (TextView) findViewById(R.id.piConnected);

        // TODO




        new Thread(new Runnable() {
            public void run() {
//                startService(new Intent(getBaseContext(), VehicleConnectionAgent.class));
//
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                stopService(new Intent(getBaseContext(), VehicleConnectionAgent.class));
            }
        }).start();



        // TODO - what if we used sqlite to store relational data?

    }




    public void sendData_onClick(View v) {
        sendRandomData();
    }

    public void sendRandomData() {
//        if(myWebListener!=null){
//            String data=new Data().randomize().toString();
//            myWebListener.sendData(data);
//        }
        if (webAgent != null) {
            String data = new Data().randomize().toString();
            webAgent.sendData(data);
        }
    }

    /**
     * onClick method for the startWebListener button
     *
     * @param v
     */
    public void startWebClick(View v) {
        startWebServerListener();
    }

    /**
     * onClick method for the stopWebListener button
     *
     * @param v
     */
    public void stopWebClick(View v) {
        stopWebServerListener();
    }

    /**
     * onClick method for the startPiListener button
     *
     * @param v
     */
    public void startPiClick(View v) {
        startPiListener();
    }

    /**
     * onClick method for the stopPiListener button
     *
     * @param v
     */
    public void stopPiClick(View v) {
        stopPiListener();
    }

    /**
     *
     */
    public void startWebServerListener() {
        /*if (myWebListener != null)
            stopWebServerListener();
        isWebDataServerRunning=true;
        myWebListener=new WebListener();
        myWebListener.execute();*/
        if (webAgent != null) {
            webAgent.stopServer();
        }
        webAgent = new WebClientConnectionAgent(this);
        webAgent.startServer();
    }

    /**
     *
     */
    public void stopWebServerListener() {
        /*try {
            webServerSocket.close();
            webServerOutput.close();
        } catch(Exception e) {
            updateConsole("Error: " + e.toString());
        }
        isWebDataServerRunning=false;
        myWebListener=null;
        setWebConnected(false);
        updateConsole("--WebListener stopped--");*/
        webAgent.stopServer();
    }

    /**
     *
     */
    public void startPiListener() {
        if (myPiListener != null)
            stopPiListener();
        isPiDataServerRunning = true;
        myPiListener = new PiListener();
        myPiListener.execute();
    }

    /**
     *
     */
    public void stopPiListener() {
        try {
            piServerSocket.close();
        } catch (Exception e) {
            updateConsole("Error: " + e.toString());
        }
        isPiDataServerRunning = false;
        myPiListener = null;
        setPiConnected(false);
        updateConsole("--PiListener stopped--");
    }

    /**
     * @param val
     */
    public void setWebConnected(boolean val) {
        webConnected.setText(val ? "Connected" : "Not Connected");
    }

    /**
     * @param val
     */
    public void setPiConnected(boolean val) {
        piConnected.setText(val ? "Connected" : "Not Connected");
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (piServerSocket != null) {
            try {
                //close the connections
                piServerSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * update the console text display view with the specified text
     *
     * @param line the line of text to write to the console
     */
    public void updateConsole(String line) {
        try {
            console.append(line + "\n");
        } catch (Exception e) {
            System.out.println(console);
            console.append(e.toString());
        }
    }

    /**
     * instantiates the webServerOutput Buffered Writer
     * @param stream
     */
    /*public void instantiateWriter(OutputStream stream) {
        webServerOutput = new BufferedWriter(new OutputStreamWriter(stream));
    }*/

    /**
     * WebServer Listener Task for asynchronous communication between the phone and the PitView Web Server
     *
     * java why u make us use void as a type :-(
     */
    /*public class WebListener extends AsyncTask<Void, Void, Void> {

        // the message queue string
        String pendingData = "";

        *//**
     * appends the string to the message queue
     * @param s
     *//*
        public void sendData(String s) {
            //make sure there's no empty line if it's the first message in the queue
            if (pendingData.equals(""))
                pendingData = s;
            else
                pendingData += s + "\n";
        }

        *//**
     * updates the console text with the specified string
     *//*
        public void updateUI(final String s) {
            //cannot access UI stuff on a non-UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateConsole(s);
                }
            });
        }

        *//**
     * updates the Web Listener concoction indicator with the specified value
     *//*
        public void updateConnected(final boolean val) {
            //cannot access UI stuff on a non-UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setWebConnected(val);
                }
            });
        }

        *//**
     * stops the web server
     *//*
        public void bail() {
            //cannot access UI stuff on a non-UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopWebServerListener();
                }
            });
        }

        *//**
     * task's main method, initiates connection/writer/sockets, continuous while loop
     * overrides the default doInBackground method
     * @param params an arbitrary number (0..*) of Void objects.
     *               Yep, void freaking objects. don't pass this anything.
     * @return void.
     *      yes, you can in fact return void type objects in java. don't do this in graded code.
     *//*
        @Override
        protected Void doInBackground(Void... params) {
            try {
                //update the console display
                updateUI("Starting WebListener");

                //instantiate the server socket for PitView to connect to on port 1112
                webServerSocket = new ServerSocket(1112);
                //instantiate the connection with the PitView web server as the accepted socket from the PitView connection request
                //      CODE WAITS HERE UNTIL PITVIEW SENDS CONNECTION REQUEST
                webServerDataSocket = webServerSocket.accept();
                //update the connection indicator
                updateConnected(true);
                //welcome message
                updateUI("PitView v1.0");
                updateUI("Connected to PitView (IP = " + webServerDataSocket.getRemoteSocketAddress().toString().substring(1) + ":1112)");
                //instantiate the data writer to relay the data to PitView
                instantiateWriter(webServerDataSocket.getOutputStream());
                //add data to the message queue
                sendData("Connection initialized");
                //repeat while server is on
                while (isWebDataServerRunning){
                    //if queue isn't empty
                    if(!pendingData.equals("")) {
                        //write the data
                        webServerOutput.write(pendingData);
                        // IMPORTANT: write a new line, necessary to actually send the data
                        webServerOutput.newLine();
                        //send the data
                        webServerOutput.flush();

                        pendingData = "";
                    }
                }
            } catch (SocketException se) {
                //connection was interrupted, closed on one end, or nullified
                updateUI("WebServer connection interrupted, shutting down...");
                bail();
            } catch (Exception e) {
                updateUI("Error: " + e.toString());
            }
            return null;
        }
    }*/

    /**
     * WebServer Listener Task for asynchronous communcation between the phone and the PitView Web Server
     */
    public class PiListener extends AsyncTask<Void, Void, Void> {

        /**
         * Updates the console text with the specified string
         *
         * @param s
         */
        public void updateUI(final String s) {
            //cannot access UI stuff on a non-UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateConsole(s);
                }
            });
        }

        /**
         * updates the Web Listener connection indicator with the specified value
         *
         * @param val
         */
        public void updateConnected(final boolean val) {
            //cannot access UI stuff on a non-UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setPiConnected(val);
                }
            });
        }

        /**
         * stops the web server
         */
        public void bail() {
            //cannot access UI stuff on a non-UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopPiListener();
                }
            });
        }

        /**
         * task's main method, initiates connection/reader/sockets, continuous while loop
         * overrides the default doInBackground method
         *
         * @param params
         * @return
         */
        @Override
        protected Void doInBackground(Void... params) {
            try {

                /**
                 * raspberry pi will act as the server, we are the client
                 *
                 * !!1!
                 */


                //update the console display
                updateUI("Starting PiListener");
                //instantiate the server socket for the pi to connect to on port 1111
                piServerSocket = new ServerSocket(1111);
                //instantiate the connection with the pi as the accepted socket from the pi connection request
                //      CODE WAITS HERE UNTIL PITVIEW SENDS CONNECTION REQUEST
                Socket echoSocket = piServerSocket.accept();
                //update connection indicator
                updateConnected(true);
                if (echoSocket == null) {
                    updateUI("Socket connection failed");
                } else {
                    //while the server is running
                    while (isPiDataServerRunning) {
                        //read a line of the message
                        String line = new BufferedReader(new InputStreamReader(echoSocket.getInputStream())).readLine();
                        // line can be null for end of message, let's fix that and
                        final String output = line = line != null ? line : "";

                        // update ui
                        if (!line.isEmpty()) {
                            updateUI(output);
                            //append the data to the web server message queue
                            //myWebListener.sendData(output);
                            webAgent.sendData(output);
                        }
                    }
                    updateUI("done receiving - now sent to blake");
                }
            } catch (SocketException se) {
                //connection was interrupted, closed on one end, or nullified
                updateUI("Pi connection interrupted, shutting down...");
                bail();
            } catch (Exception e) {
                updateUI("Error: " + e.toString());
            }

            return null;
        }
    }
}