package com.example.mdp_android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.UUID;

public class Reconfigure extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    TextView f1;
    TextView f2;
    Button f1Btn;
    Button f2Btn;
    BluetoothDevice myBTConnectionDevice;
    public static final String mypreference="mypref";
    public static final String F1="f1";
    public static final String F2="f2";
    private static final String TAG = "Reconfig";
    //UUID
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //REGISTER BROADCAST RECEIVER FOR IMCOMING MSG
        LocalBroadcastManager.getInstance(this).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reconfigure);
        f1= findViewById(R.id.editf1);
        f2= findViewById(R.id.editf2);
        f1Btn = findViewById(R.id.f1Btn);
        f2Btn = findViewById(R.id.f2Btn);
        sharedPreferences=getSharedPreferences(mypreference, Context.MODE_PRIVATE);

        if (sharedPreferences.contains(F1)){
            f1.setText(sharedPreferences.getString(F1,""));
        }
        if (sharedPreferences.contains(F2)){
            f2.setText(sharedPreferences.getString(F2,""));
        }

        onClickF1Btn();
        onClickF2Btn();

    }

    /*
       ONCLICKLISTENER FOR FORWARD MOVEMENT BUTTON
   */
    public void onClickF1Btn() {

        f1Btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                String tempF1 = sharedPreferences.getString(F1,"");
                byte[] bytes = tempF1.getBytes(Charset.defaultCharset());
                BluetoothChat.writeMsg(bytes);
                Toast.makeText(Reconfigure.this, "F1 Button Pressed!!",
                        Toast.LENGTH_SHORT).show();

            }

        });

    }
    /*
       ONCLICKLISTENER FOR FORWARD MOVEMENT BUTTON
   */
    public void onClickF2Btn() {

        f2Btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                String tempF2 = sharedPreferences.getString(F2,"");
                byte[] bytes = tempF2.getBytes(Charset.defaultCharset());
                BluetoothChat.writeMsg(bytes);
                Toast.makeText(Reconfigure.this, "F2 Button Pressed!!",
                        Toast.LENGTH_SHORT).show();

            }

        });

    }

    public void Save(View view){
        String strf1 = f1.getText().toString();
        String strf2 = f2.getText().toString();
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putString(F1,strf1);
        editor.putString(F2,strf2);
        editor.commit();
    }


    public void Clear(View view){
        f1=(TextView) findViewById(R.id.editf1);
        f2=(TextView) findViewById(R.id.editf2);
        f1.setText("");
        f2.setText("");
    }

    public void Retrieve(View view){
        f1=(TextView) findViewById(R.id.editf1);
        f2=(TextView) findViewById(R.id.editf2);
        sharedPreferences=getSharedPreferences(mypreference, Context.MODE_PRIVATE);
        if (sharedPreferences.contains(F1)){
            f1.setText(sharedPreferences.getString(F1,"String Not Found"));
        }
        if (sharedPreferences.contains(F2)){
            f2.setText(sharedPreferences.getString(F2,"String Not Found"));
        }
    }


    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        getMenuInflater().inflate(R.menu.reconfigure, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        if (item.getItemId() == R.id.main) {
            Intent intent = new Intent(Reconfigure.this, SettingsFragment.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }

        if (item.getItemId() == R.id.connect) {
            Intent intent = new Intent(Reconfigure.this, Connect.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        Intent intent = new Intent(Reconfigure.this, SettingsFragment.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }


    //BROADCAST RECEIVER FOR BLUETOOTH CONNECTION STATUS
    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Receiving btConnectionStatus Msg!!!");

            String connectionStatus = intent.getStringExtra("ConnectionStatus");
            myBTConnectionDevice = intent.getParcelableExtra("Device");

            //DISCONNECTED FROM BLUETOOTH CHAT
            if(connectionStatus.equals("disconnect")){

                Log.d("ConnectAcitvity:","Device Disconnected");

                //Stop Bluetooth Connection Service
                //stopService(connectIntent);

                //RECONNECT DIALOG MSG
                AlertDialog alertDialog = new AlertDialog.Builder(Reconfigure.this).create();
                alertDialog.setTitle("BLUETOOTH DISCONNECTED");
                alertDialog.setMessage("Connection with device: '"+myBTConnectionDevice.getName()+"' has ended. Do you want to reconnect?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //startBTConnection(myBTConnectionDevice, myUUID);
                                //START BT CONNECTION SERVICE
                                Intent connectIntent = new Intent(Reconfigure.this, BluetoothConnectionService.class);
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

                if(!isFinishing()){ //here activity means your activity class
                    alertDialog.show();
                }
            }

            //SUCCESSFULLY CONNECTED TO BLUETOOTH DEVICE
            else if(connectionStatus.equals("connect")){


                Log.d("ConnectAcitvity:","Device Connected");
                Toast.makeText(Reconfigure.this, "Connection Established: "+ myBTConnectionDevice.getName(),
                        Toast.LENGTH_LONG).show();
            }

            //BLUETOOTH CONNECTION FAILED
            else if(connectionStatus.equals("connectionFail")) {
                Toast.makeText(Reconfigure.this, "Connection Failed: "+ myBTConnectionDevice.getName(),
                        Toast.LENGTH_LONG).show();
            }

        }
    };
}
