package com.example.mdp_android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.Charset;
import java.util.UUID;

public class SettingsFragment extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "SettingsFragment";

    private SensorManager sensorManager;
    private Sensor sensor;
    ImageButton forwardBtn;
    ImageButton leftBtn;
    ImageButton backwardBtn;
    ImageButton rightBtn;
    Button waypoint;
    Button startpoint;
    Button ftpBtn;
    Button explorationBtn;
    Button manualBtn;
    Switch tiltBtn;
    ToggleButton autoBtn;
    TextView incomingText;
    TextView robotStatus;
    TextView connectionStatusBox;
    TextView md5ExplorationText;
    TextView md5ObstacleText;
    static String connectedDevice;
    boolean connectedState;
    boolean currentActivity;
    BluetoothDevice myBTConnectionDevice;
    static Context context;
    MazeView myMaze;
    boolean autoUpdate;
    boolean tiltNavi;


    //UUID
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(" SettigsFragment:", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);
        myMaze = findViewById(R.id.mapView);

        connectedDevice = null;
        connectedState = false;
        currentActivity = true;
        autoUpdate = true;
        tiltNavi = false;
        forwardBtn = findViewById(R.id.forwardBtn);
        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);
        backwardBtn = findViewById(R.id.backwardBtn);
        connectionStatusBox = findViewById(R.id.connectionStatus);
        waypoint = findViewById(R.id.wpBtn);
        startpoint = findViewById(R.id.spBtn);
        explorationBtn = findViewById(R.id.explorationBtn);
        ftpBtn = findViewById(R.id.ftpBtn);
        robotStatus = findViewById(R.id.robotStatus);
        manualBtn = findViewById(R.id.manualBtn);
        autoBtn = findViewById(R.id.autoBtn);
        tiltBtn = findViewById(R.id.tiltSwitch);
        incomingText = findViewById(R.id.incomingText);
        md5ExplorationText = findViewById(R.id.md5ExplorationText);
        md5ObstacleText = findViewById(R.id.md5ObstacleText);

        //SET ONCLICKLISTENER FOR NAVIGATION BUTTON
        onClickNavigationForward();
        onClickNavigationBackward();
        onClickNavigationRight();
        onClickNavigationLeft();
        onClickSetWaypoint();
        onClickSetStartPoint();
        onClickStartSp();
        onClickStartExploration();
        onClickManualUpdate();
        onClickAutoUpdate();
        onClickTiltSwitch();

        //MAKE TEXTFIELD SCROLLABLE
        incomingText.setMovementMethod(new ScrollingMovementMethod());
        md5ObstacleText.setMovementMethod(new ScrollingMovementMethod());
        md5ExplorationText.setMovementMethod(new ScrollingMovementMethod());

        //DECLARING SENSOR MANAGER AND SENSOR TYPE
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //REGISTER TILT MOTION SENSOR
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);


        //REGISTER BROADCAST RECEIVER FOR INCOMING MSG
        LocalBroadcastManager.getInstance(this).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));

        //REGISTER BROADCAST RECEIVER FOR IMCOMING MSG
        LocalBroadcastManager.getInstance(this).registerReceiver(incomingMsgReceiver, new IntentFilter("IncomingMsg"));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Log or put a breakpoint here to check if it's being called
        Log.d("SettingsFragment", "Back pressed");
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    //RESUME ACTIVITY
    @Override
    protected void onResume() {
        super.onResume();

        currentActivity = true;

        //REGISTER TILT MOTION SENSOR
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        Log.d(" MainAcitvity:", "OnResume:" + connectedState);

        //CHECK FOR EXISTING CONNECTION
        if (connectedState) {
            Log.d(" MainAcitvity:", "OnResume1");

            //SET TEXTFIELD TO DEVICE NAME
            connectionStatusBox.setText(connectedDevice);
        } else {
            Log.d(" MainAcitvity:", "OnResume2");

            //SET TEXTFIELD TO NOT CONNECTED
            connectionStatusBox.setText(R.string.btStatusOffline);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity: onDestroyed: destroyed");

        //unregisterReceiver(btConnectionReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*
        SIDE MENUS ONCLICK
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //ONCLICK BLUETOOTH SEARCH BUTTON
        if (item.getItemId() == R.id.connect) {
            currentActivity = false;

            Intent intent = new Intent(SettingsFragment.this, Connect.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }

        if (item.getItemId() == R.id.reconfigure) {
            currentActivity = false;

            Intent intent = new Intent(SettingsFragment.this, Reconfigure.class);
            // intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    //BROADCAST RECEIVER FOR INCOMING MESSAGE
    BroadcastReceiver incomingMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String msg = intent.getStringExtra("receivingMsg");
            incomingText.setText(msg);

            Log.d(TAG, "Receiving incomingMsg!!!" + msg);

            //FILTER EMPTY AND CONCATENATED STRING FROM RECEIVING CHANNEL
            if (msg.length() > 7 && msg.length() < 345) {

                //CHECK IS STARTING STRING FOR ANDROID
                if (msg.substring(0, 7).equals("Android")) {

                    String[] filteredMsg = delimiterMsg(msg.replaceAll(" ", "").replaceAll("\\n", "").trim(), "\\|");

                    Log.d(TAG, "Stage 1: " + filteredMsg[2]);

                    switch (filteredMsg[2]) {


                        case "arenaupdate":

                            String[] mazeInfo = delimiterMsg(filteredMsg[3], ",");

                            try {
                                //ENSURE ROBOT COORDINATES IS WITHIN RANGE
                                if (Integer.parseInt(mazeInfo[2]) > 0 && Integer.parseInt(mazeInfo[2]) < 14 && Integer.parseInt(mazeInfo[3]) > 0 && Integer.parseInt(mazeInfo[3]) < 19) {

                                    myMaze.updateMaze(mazeInfo, autoUpdate);

                                }
                                //SET ROBOT STATUS TO STOP FOR EXPLORATION WHEN ROBOT RETURN TO ORIGINAL POSITION
                                if (mazeInfo[2].equals("13") && mazeInfo[3].equals("18") && robotStatus.getText().equals(getString(R.string.FastestPath))) {
                                    robotStatus.setText(R.string.Robot_Idle);
                                }
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            break;

                        case "robotstatus":
                            Log.d(TAG, "RobotStatus: " + filteredMsg[3]);

                            try {
                                if (filteredMsg[3].equals("stop")) {
                                    robotStatus.setText(R.string.Robot_Idle);
                                } else if (filteredMsg[3].equals("moving")) {
                                    robotStatus.setText(R.string.Robot_Moving);
                                }
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            break;

                        case "hex":

                            try {
                                String[] mapDescriptor = delimiterMsg(filteredMsg[3], ",");
                                mapDescriptor[0] = "Part1: " + mapDescriptor[0].toUpperCase();
                                mapDescriptor[1] = "Part2: " + mapDescriptor[1].toUpperCase();

                                md5ExplorationText.setText(mapDescriptor[0]);
                                md5ObstacleText.setText(mapDescriptor[1]);
                                robotStatus.setText(R.string.Robot_Idle);
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            }
        }
    };

    /*
    ONCLICKLISTENER FOR START EXPLORATION BUTTTON
    */
    public void onClickStartExploration() {

        explorationBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                int robotSp[] = myMaze.getRobotStartPoint();

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(SettingsFragment.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    //SEND START POINT TO RPI
                    String spCoordinates = "Algorithm|Android|SetStartPoint|" + robotSp[0] + "," + robotSp[1];
                    byte[] bytes = spCoordinates.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);

                    //DELAY 1 SEC
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            //SEND START EXPLORATION MSG TO RPI
                            String startExploration = "Algorithm|Android|StartExploration|1000";
                            byte[] bytes = startExploration.getBytes(Charset.defaultCharset());
                            BluetoothChat.writeMsg(bytes);

                            Toast.makeText(SettingsFragment.this, "STARTING EXPLORATION...",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, 1000);

                    //SET ROBOT STATUS TO EXPLORATION
                    robotStatus.setText(R.string.Exploration);

                }
            }

        });

    }

    /*
      ONCLICKLISTENER FOR START SHORTEST PATH
  */
    public void onClickStartSp() {

        ftpBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                int wayPoint[] = myMaze.getWaypoint();

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(SettingsFragment.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {

                    //ENSURE WAYPOINT IS SET FIRST (20 cos waypoint is inverse when calling getWayPoint())
                    if (wayPoint[0] == -1 && wayPoint[1] == 20) {
                        Toast.makeText(SettingsFragment.this, "Please Select Waypoint!",
                                Toast.LENGTH_SHORT).show();
                    }
                    //START SHORTEST PATH
                    else {

                        //SEND START FASTEST PATH MSG TO RPI
                        String startSP = "Algorithm|Android|StartFastestPath|1000";
                        byte[] bytes = startSP.getBytes(Charset.defaultCharset());
                        BluetoothChat.writeMsg(bytes);

                        Toast.makeText(SettingsFragment.this, "STARTING SHORTEST PATH...",
                                Toast.LENGTH_SHORT).show();

                        //SET ROBOT STATUS TO EXPLORATION
                        robotStatus.setText(R.string.FastestPath);

                    }


                }


            }

        });

    }

    /*
       ONCLICKLISTENER FOR TILT BUTTON
   */
    public void onClickTiltSwitch() {

        tiltBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    tiltNavi = true;
                    Toast.makeText(SettingsFragment.this, "Tilt Switch On!!", Toast.LENGTH_SHORT).show();

                } else {

                    tiltNavi = false;
                    Toast.makeText(SettingsFragment.this, "Tilt Switch Off!!", Toast.LENGTH_SHORT).show();


                }
            }
        });


    }

    /*
      ONCLICKLISTENER FOR SET WAY POINT BUTTON
  */
    public void onClickSetWaypoint() {

        waypoint.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Toast.makeText(SettingsFragment.this, "Select Waypoint for Shortest Path",
                        Toast.LENGTH_SHORT).show();
                myMaze.setWayPoint(true);


            }

        });

    }


    /*
     ONCLICKLISTENER FOR SET START POINT BUTTON
 */
    public void onClickSetStartPoint() {

        startpoint.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                myMaze.setStartPoint(true);
                Toast.makeText(SettingsFragment.this, "Select Start Point for Robot",
                        Toast.LENGTH_SHORT).show();

            }

        });

    }

    /*
        ONCLICKLISTENER FOR FORWARD MOVEMENT BUTTON
    */
    public void onClickNavigationForward() {

        forwardBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(SettingsFragment.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {

                    String navi = "Arduino|Android|F|01";
                    byte[] bytes = navi.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Toast.makeText(SettingsFragment.this, "Forward Movement Pressed!!",
                            Toast.LENGTH_SHORT).show();
                }

            }

        });

    }

    /*
        ONCLICKLISTENER FOR BACKWARD MOVEMENT BUTTON
    */
    public void onClickNavigationBackward() {

        backwardBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(SettingsFragment.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String navi = "Arduino|Android|T|Nil";
                    byte[] bytes = navi.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Toast.makeText(SettingsFragment.this, "Backward Movement Pressed!!",
                            Toast.LENGTH_SHORT).show();
                }
            }

        });

    }

    /*
        ONCLICKLISTENER FOR LEFTWARD MOVEMENT BUTTON
    */
    public void onClickNavigationRight() {

        rightBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(SettingsFragment.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String navi = "Arduino|Android|R|Nil";
                    byte[] bytes = navi.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Toast.makeText(SettingsFragment.this, "Right Turn Movement Pressed!!",
                            Toast.LENGTH_SHORT).show();
                }
            }

        });

    }

    /*
        ONCLICKLISTENER FOR RIGHTWARD MOVEMENT BUTTON
    */
    public void onClickNavigationLeft() {

        leftBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(SettingsFragment.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String navi = "Arduino|Android|L|Nil";
                    byte[] bytes = navi.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Toast.makeText(SettingsFragment.this, "Left Turn Movement Pressed!!",
                            Toast.LENGTH_SHORT).show();
                }
            }

        });

    }

    /*
      ONCLICKLISTENER FOR MANUAL MAZE UPDATE BUTTON
  */
    public void onClickManualUpdate() {

        manualBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {


                myMaze.refreshMap();
                Toast.makeText(SettingsFragment.this, "Maze Manual Update!!",
                        Toast.LENGTH_SHORT).show();

            }

        });

    }

    /*
    ONCLICKLISTENER FOR AUTO MAZE UPDATE BUTTON
   */
    public void onClickAutoUpdate() {

        autoBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    autoUpdate = true;

                    // Make a toast to display toggle button status
                    Toast.makeText(SettingsFragment.this,
                            "Auto Update Toggle on", Toast.LENGTH_SHORT).show();
                } else {

                    autoUpdate = false;

                    // Make a toast to display toggle button status
                    Toast.makeText(SettingsFragment.this,
                            "Auto Update Toggle off", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //BROADCAST RECEIVER FOR BLUETOOTH CONNECTION STATUS
    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Receiving btConnectionStatus Msg!!!");

            String connectionStatus = intent.getStringExtra("ConnectionStatus");
            myBTConnectionDevice = intent.getParcelableExtra("Device");
            //myBTConnectionDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //DISCONNECTED FROM BLUETOOTH CHAT
            if (connectionStatus.equals("disconnect")) {

                Log.d("SettingsFragment:", "Device Disconnected");
                connectedDevice = null;
                connectedState = false;
                connectionStatusBox.setText(R.string.btStatusOffline);

                if (currentActivity) {

                    //RECONNECT DIALOG MSG
                    AlertDialog alertDialog = new AlertDialog.Builder(SettingsFragment.this).create();
                    alertDialog.setTitle("BLUETOOTH DISCONNECTED");
                    alertDialog.setMessage("Connection with device: '" + myBTConnectionDevice.getName() + "' has ended. Do you want to reconnect?");
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    //START BT CONNECTION SERVICE
                                    Intent connectIntent = new Intent(SettingsFragment.this, BluetoothConnectionService.class);
                                    connectIntent.putExtra("serviceType", "connect");
                                    connectIntent.putExtra("device", myBTConnectionDevice);
                                    connectIntent.putExtra("id", myUUID);
                                    startService(connectIntent);
                                }
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();

                }
            }
            //SUCCESSFULLY CONNECTED TO BLUETOOTH DEVICE
            else if (connectionStatus.equals("connect")) {

                connectedDevice = myBTConnectionDevice.getName();
                connectedState = true;
                Log.d("MainActivity:", "Device Connected " + connectedState);
                connectionStatusBox.setText(connectedDevice);
                Toast.makeText(SettingsFragment.this, "Connection Established: " + myBTConnectionDevice.getName(),
                        Toast.LENGTH_LONG).show();
            }

            //BLUETOOTH CONNECTION FAILED
            else if (connectionStatus.equals("connectionFail")) {
                Toast.makeText(SettingsFragment.this, "Connection Failed: " + myBTConnectionDevice.getName(),
                        Toast.LENGTH_LONG).show();
            }

        }
    };

    private String[] delimiterMsg(String msg, String delimiter) {

        return (msg.toLowerCase()).split(delimiter);
    }

    //METHOD FOR TILT SENSING (NAVIGATION)
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];

        //CHECK IF TILT SWITCH IS ENABLED
        if (tiltNavi) {

            //CHECK IF CONNECTED TO DEVICE FIRST
            if (connectedDevice == null) {
                Toast.makeText(SettingsFragment.this, "Please Connect to a Device First!!",
                        Toast.LENGTH_SHORT).show();
            } else {

                if (Math.abs(x) > Math.abs(y)) {
                    if (x < 0) {
                        Log.d("SettingsFragment:", "RIGHT TILT!!");

                        String navi = "Arduino|Android|R|Nil";
                        byte[] bytes = navi.getBytes(Charset.defaultCharset());
                        BluetoothChat.writeMsg(bytes);
                       /* Toast.makeText(MainActivity.this, "Right Movement Detected!!",
                                Toast.LENGTH_SHORT).show();*/
                    }
                    if (x > 0) {
                        Log.d("SettingsFragment:", "LEFT TILT!!");

                        String navi = "Arduino|Android|L|Nil";
                        byte[] bytes = navi.getBytes(Charset.defaultCharset());
                        BluetoothChat.writeMsg(bytes);
                        /*Toast.makeText(MainActivity.this, "Left Movement Detected!!",
                                Toast.LENGTH_SHORT).show();*/
                    }
                } else {
                    if (y < 0) {
                        Log.d("SettingsFragment:", "UP TILT!!");

                        String navi = "Arduino|Android|F|01";
                        byte[] bytes = navi.getBytes(Charset.defaultCharset());
                        BluetoothChat.writeMsg(bytes);
                        /*Toast.makeText(MainActivity.this, "Forward Movement Detected!!",
                                Toast.LENGTH_SHORT).show();*/
                    }
                    if (y > 0) {
                        Log.d("SettingsFragment:", "DOWN TILT!!");

                        String navi = "Arduino|Android|T|Nil";
                        byte[] bytes = navi.getBytes(Charset.defaultCharset());
                        BluetoothChat.writeMsg(bytes);
                        /*Toast.makeText(MainActivity.this, "Down Movement Detected!!",
                                Toast.LENGTH_SHORT).show();*/
                    }
                }
       /* if (x > (-2) && x < (2) && y > (-2) && y < (2)) {
            Log.d("MainActivity:", "NOT TILTED!!");

        }*/
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

  /*  //REVERSE STRING FOR MAP DESCRIPTOR MD5
    public String reverseString(String md5){
        int count = 299 - 15;
        String temp = "";
        for(int i= count; count < md5.length(); i++){

        }



        return temp.toString();
    }

    //METHOD TO GET MAP DESCRIPTOR FOR EXPLORATION
    public String mapDescExploration(String md5){

        String tempMd5 = "11";

        for(int i = 0; i < md5.length(); i++)
        {
            char c = md5.charAt(i);

            if(c != '3'){
                tempMd5 += "1";
            }
            else{
                tempMd5 +="0";
            }

        }

        tempMd5 += "11";

        Log.d("MainActivity:", "map descriptor for exploration: " + tempMd5);

        return convertHex(tempMd5);
    }

    //METHOD TO GET MAP DESCRIPTOR FOR OBSTACLE
    public String mapDescObstacle(String md5){

        String tempMd5 = "";

        for(int i = 0; i < md5.length(); i++)
        {
            char c = md5.charAt(i);

            if(c != '3'){

                if(c == '2'){
                    tempMd5 += "1";
                }
                else{
                    tempMd5 += "0";

                }
            }

        }
        Log.d("MainActivity:", "map descriptor for obstacle: " + tempMd5);

        return convertHex(tempMd5);

    }

    //METHOD TO CONVERT BINARY STRING TO HEXADECIMAL
    public String convertHex(String md5){

        String hex = "";
        StringBuilder sb = new StringBuilder();

        int temp = md5.length() %4;

        if(temp != 0){

            for(int i=0; i<temp; i++){
                sb.append('0');
            }
            sb.append(md5);
        }

        int digitNumber = 1;
        int sum = 0;
        for(int i = 0; i < md5.length(); i++){
            if(digitNumber == 1)
                sum+=Integer.parseInt(md5.charAt(i) + "")*8;
            else if(digitNumber == 2)
                sum+=Integer.parseInt(md5.charAt(i) + "")*4;
            else if(digitNumber == 3)
                sum+=Integer.parseInt(md5.charAt(i) + "")*2;
            else if(digitNumber == 4 || i < md5.length()+1){
                sum+=Integer.parseInt(md5.charAt(i) + "")*1;
                digitNumber = 0;
                if(sum < 10) {
                    hex += String.valueOf(sum);
                }
                else if(sum == 10) {
                    hex += "A";
                    //System.out.print("A");
                }
                else if(sum == 11){
                    hex += "B";
                    // System.out.print("B");
                }
                else if(sum == 12) {
                    hex += "C";
                    //System.out.print("C");
                }
                else if(sum == 13) {
                    hex += "D";
                    //System.out.print("D");
                }
                else if(sum == 14) {
                    hex += "E";
                    // System.out.print("E");
                }
                else if(sum == 15) {
                    hex += "F";
                    //System.out.print("F");
                }
                sum=0;
            }
            digitNumber++;
        }

        Log.d("MainActivity:", "hex after convertion: " + hex);


        return hex;
    }*/
}


