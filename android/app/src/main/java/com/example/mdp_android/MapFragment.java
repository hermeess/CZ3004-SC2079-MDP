package com.example.mdp_android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import com.example.mdp_android.BluetoothConnectionService;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

//This class to store information about each image
class ImageInfo {
    private int row;
    private int col;
    private String direction;

    public ImageInfo(int row, int col, String direction) {
        this.row = row;
        this.col = col;
        this.direction = direction;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public String getDirection() {
        return direction;
    }

    public int getDirectionNum(){
        int dirs = -1;
        switch(this.direction.toLowerCase()) {
            case "north":
                dirs = 90;
                break;
            case "south":
                dirs = -90;
                break;
            case "east":
                dirs = 0;
                break;
            case "west":
                dirs = 180;
                break;
            default:
                break;
        }
        return dirs;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}

public class MapFragment extends Fragment implements ObstacleDialogListener{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View rootView;
    private ImageView selectedGridCell = null;
    private String selectedDirection = ""; // Variable to store the selected direction
    private ImageView selectedImageView = null; // Variable to store the selected ImageView

    private final HashMap<String, ImageInfo> imageInfoMap = new HashMap<>(); // Used to store my imageInfo for all images.

    private final HashMap<String, JSONObject> obstacleMap = new HashMap<>(); // Used to store the obstacle to send to bluetooth.

    private final int[] rowArr = new int[21]; // Used to store the row tick labels
    private static final String TAG = "MapFragment";

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
    TextView statusContent;
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

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    /*
   Note:
   Functions to be called:
   1.To update the robot, call the updateRobot function alongside the new x,y coordinates,direction and type.
   2.To update the target, call the updateTarget function alongside the target id & obstacle id

   Data structure that is keeping track of all the image etc:
   A hashmap is used to keep track of all the images.
    */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_map, container, false);

        LinearLayout linearLayout = rootView.findViewById(R.id.horizontalScrollViewLayout);
        GridLayout gridLayout = rootView.findViewById(R.id.gridLayout);

        Log.d("OnCreate view", "Creating view");

        // Find the buttons by their IDs
        Button buttonLeft = rootView.findViewById(R.id.buttonLeft);
        Button buttonRight = rootView.findViewById(R.id.buttonRight);
        Button buttonUp = rootView.findViewById(R.id.buttonUp);
        Button buttonDown = rootView.findViewById(R.id.buttonDown);
        Button buttonSendToRpi = rootView.findViewById(R.id.buttonSendObstacle);

        Button buttonAdd = rootView.findViewById(R.id.addObstacles);
        Button buttonStart = rootView.findViewById(R.id.startButton);

        connectedDevice = null;
        connectedState = false;
        currentActivity = true;
        connectionStatusBox = rootView.findViewById(R.id.connectionStatus);
        statusContent = rootView.findViewById(R.id.statusContent);
        incomingText = rootView.findViewById(R.id.statusHeader);
        incomingText.setMovementMethod(new ScrollingMovementMethod());
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageInfo robotInfo = imageInfoMap.get("robot");
                //send to RPi (Start)
                if(myBTConnectionDevice == null){
                    Toast.makeText(requireContext(), "Please connect to RPI.", Toast.LENGTH_SHORT).show();
                } else if(robotInfo.getCol() == -1 && robotInfo.getRow() == -1){
                    Toast.makeText(requireContext(), "Please place the robot in the grid.", Toast.LENGTH_SHORT).show();
                } else{
                    JSONObject json = new JSONObject();
                    try {
                        json.put("cat", "control");
                        json.put("value", "start");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    BluetoothChat.writeMsg(json.toString().getBytes(Charset.defaultCharset()));
                }
            }
        });

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObstacleDialogFragment dialog = new ObstacleDialogFragment();
                dialog.setTargetFragment(MapFragment.this, 0); // Set the parent fragment (MapFragment) as the target
                dialog.show(getFragmentManager(), "ObstacleDialog");
            }
        });


        // Set click listeners for the buttons
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call updateRobot function to move left
                ImageInfo robotInfo = imageInfoMap.get("robot");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow(), robotInfo.getCol() - 1, robotInfo.getDirectionNum(), "MOVE");
                }
            }
        });

        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  Call updateRobot function to move right
                ImageInfo robotInfo = imageInfoMap.get("robot");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow(), robotInfo.getCol() + 1, robotInfo.getDirectionNum(), "MOVE");
                }
            }
        });

        buttonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call updateRobot function to move up
                ImageInfo robotInfo = imageInfoMap.get("robot");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow() + 1, robotInfo.getCol(), robotInfo.getDirectionNum(), "MOVE");
                }
            }
        });

        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call updateRobot function to move down
                ImageInfo robotInfo = imageInfoMap.get("robot");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow() - 1, robotInfo.getCol(), robotInfo.getDirectionNum(), "MOVE");
                }
            }
        });

        buttonSendToRpi.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(myBTConnectionDevice == null){
                    Toast.makeText(requireContext(), "Please connect to RPI.", Toast.LENGTH_SHORT).show();
                } else{
                    // Call sendObstacleToRpi here, this is the function that compiles the JSON format
                    try {
                        sendObstacleToRpi();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });


        // Setting a rowArr for row tick labels
        int rowVal = 21;
        for(int i = 0; i < 21; i++){
            rowArr[i] = rowVal;
            rowVal--;
        }

        // Create my grid view
        for (int row = 0; row < 21; row++) {
            for (int col = 0; col < 21; col++) {
                if(col == 0 && row == 0){
                    continue;
                }
                // Generation of row ticks
                if(col == 0){
                    TextView rowLabelTextView = new TextView(requireContext());
                    rowLabelTextView.setText(String.valueOf(rowArr[row]));
                    rowLabelTextView.setTextSize(5);

                    // Set layout parameters for the row ticks
                    GridLayout.LayoutParams rowLabelParams = new GridLayout.LayoutParams();
                    rowLabelParams.width = 0;
                    rowLabelParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    rowLabelParams.rowSpec = GridLayout.spec(row,1f);
                    rowLabelParams.columnSpec = GridLayout.spec(col, 1f);
                    rowLabelTextView.setLayoutParams(rowLabelParams);

                    rowLabelTextView.setGravity(Gravity.CENTER);

                    // Add the row label to your gridLayout
                    gridLayout.addView(rowLabelTextView);

                } else if(row == 0){ // Generation of col ticks
                    TextView colLabelTextView = new TextView(requireContext());
                    colLabelTextView.setText(String.valueOf(col));
                    colLabelTextView.setTextSize(5);

                    // Set layout parameters for the column label
                    GridLayout.LayoutParams colLabelParams = new GridLayout.LayoutParams();
                    colLabelParams.width = 0;
                    colLabelParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    colLabelParams.rowSpec = GridLayout.spec(row,1f);
                    colLabelParams.columnSpec = GridLayout.spec(col, 1f);
                    colLabelTextView.setLayoutParams(colLabelParams);

                    colLabelTextView.setGravity(Gravity.CENTER);

                    // Add the column label to your gridLayout
                    gridLayout.addView(colLabelTextView);
                } else{
                // Create a new ImageView for each cell
                final ImageView cellImageView = new ImageView(requireContext());

                // Set the layout parameters to define the cell's size and position
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(rowArr[row], 1f);
                params.columnSpec = GridLayout.spec(col, 1f);
                params.setMargins(5,5,5,5);

                // Set background color for the cellImageView
                cellImageView.setBackgroundColor(Color.parseColor("#D3D3D3"));

                // Add the ImageView to the GridLayout
                cellImageView.setLayoutParams(params);
                gridLayout.addView(cellImageView);
                }
            }
        }

        // Load and add drawables dynamically to HorizontalScrollView
        for (int i = 0; i <= 40; i++) {
            //skip 9 & 10
            if(i == 9 || i == 10){
                continue;
            }
            // Create an ImageView
            ImageView imageView = new ImageView(requireContext());

            // Set the image resource based on the drawable name
            int drawableResource;
            // If its 0, i will load the robot icon
            if(i == 0){
                drawableResource = R.drawable.robot_icon;

                imageView.setTag("robot");
            } else{ // Loading other obstacles
                int obstacleId = i;
                String resourceName = "obstacle_" + obstacleId;
                drawableResource = getResources().getIdentifier(resourceName, "drawable", requireContext().getPackageName());

                // Assign a tag to the ImageView to represent the drawable
                imageView.setTag("obstacle_" + obstacleId); // Use a unique identifier for each drawable to add into hash maps
            }
            imageView.setImageResource(drawableResource);

            // Set layout parameters for the ImageView
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(25, 25);
            params.setMargins(10, 0, 10, 0);
            imageView.setLayoutParams(params);

            // Add the OnLongClickListener to start dragging
            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // Create a drag shadow
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                    v.startDragAndDrop(null, shadowBuilder, v, 0);

                    // Store the selected grid cell
                    selectedGridCell = (ImageView) v;

                    return true;
                }
            });

            // Add an OnClickListener to the image
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Store the selected ImageView and grid cell
                    selectedImageView = (ImageView) v;
                    selectedGridCell = (ImageView) v;

                    // Show a dialog to select a direction
                    showDirectionSelectionDialog();
                }
            });

            imageView.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(View v, DragEvent event) {
                    int action = event.getAction();
                    if(action == DragEvent.ACTION_DROP){
                            // Handle the drop event
                            ImageView draggedImage = (ImageView) event.getLocalState();

                            // Remove the image from its original parent (the GridLayout)
                            ViewGroup owner = (ViewGroup) draggedImage.getParent();
                            if (owner != null) {
                                owner.removeView(draggedImage);
                            }

                            // Retrieve the tag to identify the dropped drawable
                            String drawableTag = (String) draggedImage.getTag();

                            // Reset the imageInfo back to the default values.
                            ImageInfo imageInfo = imageInfoMap.get(drawableTag);
                            if (imageInfo != null) {
                                imageInfo.setDirection("None");
                                imageInfo.setRow(-1);
                                imageInfo.setCol(-1);
                                // Log to see if it is resets to original values
                                Log.d("ImageInfo", " Row: " + imageInfo.getRow() +
                                        " Column: " + imageInfo.getCol() +
                                        " Direction: " + imageInfo.getDirection());
                            }

                            setGridCellBorderColor("None");
                            // If the drop position is not valid within the grid,
                            // Set appropriate layout parameters for the image
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(25, 25);
                            params.setMargins(10, 0, 10, 0);
                            draggedImage.setLayoutParams(params);
                            draggedImage.setPadding(0,0,0,0);

                            // Add ImageView to the HorizontalScrollView
                            linearLayout.addView(draggedImage);
                            draggedImage.setVisibility(View.VISIBLE);

                            // Remove item from obstacleMap
                            obstacleMap.remove(drawableTag);
                            Log.d("Updated Obstacle Map", obstacleMap.toString());
                            //>>if want can send string "Obstacle ... removed"

                            TextView obstacleContent = rootView.findViewById(R.id.obstacleContent);

                            StringBuilder resultString = new StringBuilder();

                            // Iterate through the entries in the obstacleMap and extract key info
                            for (HashMap.Entry<String, JSONObject> entry : obstacleMap.entrySet()) {
                                String key = entry.getKey();
                                JSONObject obstacleData = entry.getValue();

                                int x = obstacleData.optInt("x", -1)+1;
                                int y = obstacleData.optInt("y", -1)+1;
                                int dir = obstacleData.optInt("d", -1);
                                String dirStr = "";
                                switch(dir) {
                                    case 90:
                                        dirStr = "N";
                                        break;
                                    case -90:
                                        dirStr = "S";
                                        break;
                                    case 180:
                                        dirStr = "W";
                                        break;
                                    case 0:
                                        dirStr = "E";
                                        break;
                                    default:
                                        dirStr = "None";
                                        break;
                                }

                                // Check if all required values are present
                                if (x != -1 && y != -1) {
                                    // Append the formatted string to the result
                                    resultString.append(key).append(": x: ").append(x).append(", y: ").append(y)
                                            .append(", dir: ").append(dirStr).append("\n");
                                }
                            }
                            // Convert the StringBuilder to a final string
                            String formattedObstacleData = resultString.toString();

                            obstacleContent.setText(formattedObstacleData);
                            return true;
                    }
                    return true;
                }
            });
            String imageTag = (String) imageView.getTag();

            // Initialise the imageInfo for each imageView
            ImageInfo imageInfo = new ImageInfo(-1, -1, "None");
            imageInfoMap.put(imageTag, imageInfo);

            // Add the ImageView to the LinearLayout
            linearLayout.addView(imageView);
        }

        // Set the drag listener to the grid layout and linear layout
        gridLayout.setOnDragListener(new MyDragListener());
        linearLayout.setOnDragListener(new MyDragListener());

        return rootView;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Other initialization, if needed
        //REGISTER BROADCAST RECEIVER FOR INCOMING MSG
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));

        // REGISTER BROADCAST RECEIVER FOR INCOMING MSG
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(incomingMsgReceiver, new IntentFilter("IncomingMsg"));
    }

    // Function to show a dialog for selecting a direction
    private void showDirectionSelectionDialog() {
        // Create an array of direction options
        final String[] directions = {"north", "south", "east", "west", "none"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Direction");
        builder.setItems(directions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get the selected direction
                selectedDirection = directions[which];

                // Set the border color of the selected grid cell based on the direction
                setGridCellBorderColor(selectedDirection);

                dialog.dismiss();
            }
        });
        builder.show();
    }
    public String constructObstacleJson(ImageInfo imageInfo, String selectedImageViewTag) {
        try {
            JSONObject mainObject = new JSONObject();
            JSONObject valueObject = new JSONObject();
            JSONArray obstaclesArray = new JSONArray();
            JSONObject obstacleObject = new JSONObject();

            // Subtracting 1 from the row and column for 0-based index
            obstacleObject.put("x", imageInfo.getCol() - 1); // Assuming Col is X
            obstacleObject.put("y", imageInfo.getRow() - 1); // Assuming Row is Y

            // Extract numeric portion from selectedImageViewTag and parse it
            String numericPart = selectedImageViewTag.replaceAll("[^\\d]", "");
            int obstacleId = Integer.parseInt(numericPart);

            obstacleObject.put("id", obstacleId);
            obstacleObject.put("d", imageInfo.getDirection());

            obstaclesArray.put(obstacleObject);
            valueObject.put("obstacles", obstaclesArray);
            mainObject.put("cat", "obstacles");
            mainObject.put("value", valueObject);

            return mainObject.toString();

        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Function to set the border color of the selected grid cell
    private void setGridCellBorderColor(String direction) {
        if (selectedImageView != null && selectedGridCell != null) {
            @ColorInt int color;
            switch (direction.toLowerCase()) {
                case "north":
                    color = Color.parseColor("#FF6961");
                    break;
                case "south":
                    color = Color.parseColor("#77DD77");
                    break;
                case "east":
                    color = Color.parseColor("#A7C7E7");
                    break;
                case "west":
                    color = Color.parseColor("#FFFAA0");
                    break;
                default:
                    color = Color.parseColor("#D3D3D3");
                    break;
            }
            // Set the selected Grid cell's color
            selectedGridCell.setBackgroundColor(color);

            // Update the image's direction in the map
            String selectedImageViewTag = (String) selectedImageView.getTag();
            ImageInfo imageInfo = imageInfoMap.get(selectedImageViewTag);
            if (imageInfo != null) {
                imageInfo.setDirection(direction);
//                String obstacleJson = constructObstacleJson(imageInfo, selectedImageViewTag);
//                if (obstacleJson != null) {
////                    BluetoothChat.writeMsg(obstacleJson.getBytes(Charset.defaultCharset()));
//                    Log.d("Obstacle json", obstacleJson.toString());
//                }
                //Send the data to bluetooth here, this is the one with the updated direction + x,y
                Log.d("ImageInfo", imageInfo.toString());
                //>>Bluetooth>>sendIMageInfoToBluetooth(imageInfo)
                //send to the tool

                //Obstacle ID = selectedImageViewTag, X, Y, Col can just follow log.d below to how to get.
                //"Obstacle, ID:, X:,Y:, DIR:"
            }

            // Log the ImageInfo only if it's not null
            if (imageInfo != null) {
                Log.d("ImageInfo", "Image Tag: " + selectedImageViewTag +
                        " Row: " + imageInfo.getRow() +
                        " Column: " + imageInfo.getCol() +
                        " Direction: " + imageInfo.getDirection());
                // Adding the obstacle to obstacle map;
                try {
                    if(!selectedImageViewTag.equals("robot")){
                        addObstacleToMap(selectedImageViewTag, imageInfo);
                    } else{
                        TextView robotPositionTextView = rootView.findViewById(R.id.robotPositionTextView);
                        robotPositionTextView.setText("Robot: X:" + imageInfo.getCol() + " , " +
                                "Y:" + imageInfo.getRow() + " , " +
                                "Direction: " + imageInfo.getDirection());
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public String constructUpdatedImageJson(ImageInfo imageInfo, String draggedImageTag, boolean isRobot) {
        try {
            JSONObject mainObject = new JSONObject();

            mainObject.put("tag", draggedImageTag);
            mainObject.put("x", imageInfo.getCol() - 1); // Adjusting for 0-based index
            mainObject.put("y", imageInfo.getRow() - 1); // Adjusting for 0-based index
            mainObject.put("direction", imageInfo.getDirection());
            mainObject.put("isRobot", isRobot); // Boolean field indicating if it's a robot

            return mainObject.toString();

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public class MyDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Check if this is a valid drop target (20x20 grid)
                    if (v.getId() == R.id.gridLayout) {
                        return true;
                    }
                    return false;
                case DragEvent.ACTION_DRAG_ENTERED:
                    // Handle drag entered event
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    // Handle drag exited event
                    return true;
                case DragEvent.ACTION_DROP:
                    // Handle the drop event
                    ImageView draggedImage = (ImageView) event.getLocalState();
                    GridLayout targetGrid = (GridLayout) v;

                    int column = getEventColumn(targetGrid, event);
                    int row = getEventRow(targetGrid, event);

                    if (row >= 1 && column >= 1) {
                        // Remove the image from its original parent (the selected grid cell)
                        ViewGroup owner = (ViewGroup) draggedImage.getParent();
                        if (owner != null) {
                            owner.removeView(draggedImage);
                        }

                        // Set appropriate layout parameters for the image
                        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                        params.width = 0;
                        params.height = 0;
                        params.rowSpec = GridLayout.spec(row, 1f);
                        params.columnSpec = GridLayout.spec(column, 1f);
                        params.setMargins(5, 5, 5, 5);
                        draggedImage.setLayoutParams(params);

                        // Add the image to the specified position in the grid
                        targetGrid.addView(draggedImage);
                        draggedImage.setPadding(0,5,0,5);
                        draggedImage.setVisibility(View.VISIBLE);

                        // Update the image's row and column in the map
                        String draggedImageTag = (String) draggedImage.getTag();
                        ImageInfo imageInfo = imageInfoMap.get(draggedImageTag);

                        if (imageInfo != null) {
                            imageInfo.setRow(rowArr[row]);
                            imageInfo.setCol(column);
                            Log.d("ImageInfo", "ImageTag: " + draggedImageTag +
                                    " Row: " + imageInfo.getRow() +
                                    " Column: " + imageInfo.getCol() +
                                    " Direction: " + imageInfo.getDirection());
                            boolean isRobotImage = draggedImageTag.equals("robot");
                            String updatedImageJson = constructUpdatedImageJson(imageInfo, draggedImageTag, isRobotImage);
                            if (updatedImageJson != null) {
//                                BluetoothChat.writeMsg(updatedImageJson.getBytes(Charset.defaultCharset()));
                                  Log.d("Updated Image Json", updatedImageJson.toString());
                            }
                            // Adding the obstacle to obstacle map;
                            try {
                                if(!draggedImageTag.equals("robot")){
                                    addObstacleToMap(draggedImageTag, imageInfo);
                                } else{
                                    TextView robotPositionTextView = rootView.findViewById(R.id.robotPositionTextView);
                                    robotPositionTextView.setText("Robot: X:" + imageInfo.getCol() + " , " +
                                            "Y:" + imageInfo.getRow() + " , " +
                                            "Direction: " + imageInfo.getDirection());

                                }
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            /*>>Bluetooth>>can send the bluetooth information over here, but over here the details would
                            be x,y, direction="None", unless direction is set, ie to say the obstacle is
                            already on the map and is just being moved.
                            have to put in this branch as the below one is checking if its outside of the grid.
                            just send the info to the tool for deliverable purpose.
                            */
                        }
                    } else if (v != targetGrid) {
                        // If the drop position is not valid within the grid,
                        // set appropriate layout parameters for the image
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(25, 25);
                        params.setMargins(10, 0, 10, 0);
                        draggedImage.setLayoutParams(params);

                        // Append the image to the end of the LinearLayout
                        LinearLayout horizontalLayout = v.findViewById(R.id.horizontalScrollViewLayout);
                        if (horizontalLayout != null) {
                            horizontalLayout.addView(draggedImage);
                            draggedImage.setVisibility(View.VISIBLE);
                        }
                    }

                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    // Handle drag ended event (e.g., cleanup)
                    return true;
                default:
                    break;
            }
            return false;
        }

        private int getEventColumn(GridLayout gridLayout, DragEvent event) {
            float cellWidth = gridLayout.getWidth() / gridLayout.getColumnCount();
            int column = (int) (event.getX() / cellWidth);
            return Math.max(0, Math.min(column, gridLayout.getColumnCount() - 1));
        }

        private int getEventRow(GridLayout gridLayout, DragEvent event) {
            float cellHeight = gridLayout.getHeight() / gridLayout.getRowCount();
            int row = (int) (event.getY() / cellHeight);
            return Math.max(0, Math.min(row, gridLayout.getRowCount() - 1));
        }
    }

    //The below two function will be called when receiving strings from bluetooth.
    //call this to update the robot's position.
    //>>Bluetooth to call this as well when receiving info from RPI e.g "ROBOT, 3,2,west"
    //>>Bluetooth to call this when sending info to RPI e.g "MOVE, 3,2, west".
    //>>do double check with RPi if move controls are part of their commands

    // NOTE: When receiving from RPi & algo team need to +1 to row and col.
    // Likewise if sending to RPi & algo, -1 to row and col.
    // Double check what you will be receive for the updateRobot portion from RPi side.

    private String constructRobotJson(int newRow, int newCol, String direction) {
        try {
            JSONObject mainObject = new JSONObject();

            mainObject.put("cat", "move");
            mainObject.put("tag", "robot");
            mainObject.put("x", newCol - 1); // Adjusting for 0-based index
            mainObject.put("y", newRow - 1); // Adjusting for 0-based index
            mainObject.put("direction", direction);
            mainObject.put("isRobot", true);

            return mainObject.toString();

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateRobot(int newRow, int newCol, int dirs, String action) {
        // Find the robot's ImageView by its tag, robot's tag is hardcoded to be obstacle_0
        ImageView robotImageView = rootView.findViewWithTag("robot");
        selectedImageView = robotImageView;
        selectedGridCell = robotImageView;
        String direction = "None";
        switch(dirs) {
            case 90:
                direction = "north";
                break;
            case -90:
                direction = "south";
                break;
            case 180:
                direction = "west";
                break;
            case 0:
                direction = "east";
                break;
            default:
                break;
        }
        if (robotImageView != null && newRow < 21 && newCol < 21 && newRow > 0 && newCol > 0) {
            // Update the robot's position in the ImageInfo map
            ImageInfo robotInfo = imageInfoMap.get("robot");
            robotInfo.setRow(newRow);
            robotInfo.setCol(newCol);
            robotInfo.setDirection(direction);
            setGridCellBorderColor(direction);

            // Calculate the position in the GridLayout
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) robotImageView.getLayoutParams();
            params.rowSpec = GridLayout.spec(rowArr[newRow], 1f);
            params.columnSpec = GridLayout.spec(newCol, 1f);

            // Update the ImageView's layout parameters
            robotImageView.setLayoutParams(params);

            Log.d("Robot coors", "Row:" + robotInfo.getRow() + "Col:" + robotInfo.getCol());
        }
        // If action is by button click.
        if(action.equals("MOVE")){
            String robotJson = constructRobotJson(newRow, newCol, direction);
            if (robotJson != null) {
//                BluetoothChat.writeMsg(robotJson.getBytes(Charset.defaultCharset()));
                  Log.d("Robot moving", "Robot moved");
            }
            //>> bluetooth Send the details to RPi for robot to move;
            //need to -1 from row and col when sending to RPI;
        }
    }



    // Call this to update the target's image + cell background simultaneously.
    //>>Bluetooth to call this to update the target's background

    private void updateTarget(String target, String obstacleTag) {
        String targetTag = "obstacle_" + target;
        ImageView obstacleImageView = rootView.findViewWithTag(obstacleTag);
        ImageView targetImageView = new ImageView(requireContext());

        // Set the image resource based on the drawable name
        int drawableResource;
        drawableResource = getResources().getIdentifier(targetTag, "drawable", requireContext().getPackageName());
        targetImageView.setImageResource(drawableResource);


        // Target is found, update the obstacle with the target image view
        if (targetImageView != null && obstacleImageView != null) {
            ImageInfo obstacleInfo = imageInfoMap.get(obstacleTag);
            ImageInfo targetInfo = imageInfoMap.get(targetTag);
            int obstacleRow = obstacleInfo.getRow();
            int obstacleCol = obstacleInfo.getCol();

            // Set the target row and col
            targetInfo.setRow(obstacleRow);
            targetInfo.setCol(obstacleCol);
            targetInfo.setDirection(obstacleInfo.getDirection());

            // Reset obstacleImageView first
            selectedImageView = obstacleImageView;
            selectedGridCell = obstacleImageView;
            selectedGridCell.setBackgroundColor(Color.parseColor("#D3D3D3"));

            // Change the target grid cell's background to black to denote found.
            //have to update selectedImageView and selectedGridCell
            selectedImageView = targetImageView;
            selectedGridCell = targetImageView;
            //found colour
            selectedGridCell.setBackgroundColor(Color.parseColor("#39FF14"));

            // Remove the targetImageView from its current parent
            ViewGroup targetParent = (ViewGroup) targetImageView.getParent();
            if (targetParent != null) {
                targetParent.removeView(targetImageView);
            }

            // Set the position of the targetImageView to the new row and column
            GridLayout.LayoutParams targetParams = new GridLayout.LayoutParams();
            targetParams.width = 0;
            targetParams.height = 0;
            targetParams.rowSpec = GridLayout.spec(rowArr[targetInfo.getRow()], 1f);
            targetParams.columnSpec = GridLayout.spec(targetInfo.getCol(), 1f);
            targetParams.setMargins(5, 5, 5, 5);
            targetImageView.setPadding(0, 5, 0, 5);
            targetImageView.setLayoutParams(targetParams);

            GridLayout gridLayout = rootView.findViewById(R.id.gridLayout);
            gridLayout.addView(targetImageView);


            // Move the obstacle image back to the linear layout of the horizontal scroll view
            // Set new LayoutParams for the obstacleImageView
            ViewGroup obstacleParent = (ViewGroup) obstacleImageView.getParent();
            if (obstacleParent != null) {
                obstacleParent.removeView(obstacleImageView);
            }
            LinearLayout.LayoutParams obstacleParams = new LinearLayout.LayoutParams(25, 25);
            obstacleParams.setMargins(10, 0, 10, 0);
            obstacleImageView.setLayoutParams(obstacleParams);
            obstacleInfo.setDirection("None");
            obstacleInfo.setRow(-1);
            obstacleInfo.setCol(-1);
            obstacleImageView.setPadding(0, 0, 0, 0);

            // Add the obstacle image to the horizontal layout
            LinearLayout horizontalLayout = rootView.findViewById(R.id.horizontalScrollViewLayout);
            if (horizontalLayout != null) {
                horizontalLayout.addView(obstacleImageView);
            }
        }
    }

    // Add obstacles to map: To display the obstacles info that are in the grid
    private void addObstacleToMap(String imageInfoTag, ImageInfo imageInfo) throws JSONException {
        int dirs;
        switch(imageInfo.getDirection().toLowerCase()) {
            case "north":
                dirs = 90;
                break;
            case "south":
                dirs = -90;
                break;
            case "east":
                dirs = 0;
                break;
            case "west":
                dirs = 180;
                break;
            default:
                dirs = -1;
                break;
        }
        JSONObject item = new JSONObject();
        item.put("x",imageInfo.getCol()-1);
        item.put("y", imageInfo.getRow()-1);
        item.put("d", dirs);

        obstacleMap.put(imageInfoTag, item);

        Log.d("Added obstacle to obstacle Map",obstacleMap.toString() + "x value" + imageInfo.getCol() + "y value" + imageInfo.getRow());
        TextView obstacleContent = rootView.findViewById(R.id.obstacleContent);

        StringBuilder resultString = new StringBuilder();

        // Iterate through the entries in the obstacleMap and extract key info
        for (HashMap.Entry<String, JSONObject> entry : obstacleMap.entrySet()) {
            String key = entry.getKey();
            JSONObject obstacleData = entry.getValue();

            int x = obstacleData.optInt("x", -1)+1;
            int y = obstacleData.optInt("y", -1)+1;
            int dir = obstacleData.optInt("d", -1);
            String dirStr = "";
            switch(dir) {
                case 90:
                    dirStr = "N";
                    break;
                case -90:
                    dirStr = "S";
                    break;
                case 180:
                    dirStr = "W";
                    break;
                case 0:
                    dirStr = "E";
                    break;
                default:
                    dirStr = "None";
                    break;
            }

            // Check if all required values are present
            if (x != -1 && y != -1) {
                // Append the formatted string to the result
                resultString.append(key).append(": x: ").append(x).append(", y: ").append(y)
                        .append(", dir: ").append(dirStr).append("\n");
            }
        }
        // Convert the StringBuilder to a final string
        String formattedObstacleData = resultString.toString();

        obstacleContent.setText(formattedObstacleData);
    }

    //>>call this function to send the obstacle to RPi, triggered on button click
    private void sendObstacleToRpi() throws JSONException {
        Log.d("Obstacle map", obstacleMap.toString());
        ArrayList<JSONObject> list = new ArrayList<>();
        int id = 1;
        for (HashMap.Entry<String, JSONObject> entry : obstacleMap.entrySet()) {
            JSONObject obstacleData = entry.getValue();
            obstacleData.put("id", id);
            id++;
            list.add(obstacleData);
        }
        JSONArray jsonArray = new JSONArray(list);
        JSONObject json = new JSONObject();
        json.put("obstacles", jsonArray);
        JSONObject finalJson = new JSONObject();
        finalJson.put("cat", "obstacles");
        finalJson.put("value", json);

        //>>Bluetooth this is the object to send to RPi;
        Log.d("Obstacle json", finalJson.toString());
        BluetoothChat.writeMsg(finalJson.toString().getBytes(Charset.defaultCharset()));
    }


    //this is for the dialog button portion
    @Override
    public void onObstacleDataSubmitted(String obstacleId, String row, String col, String direction) throws JSONException {
        try {
                // Convert row and col to integers
                int rowNum = Integer.parseInt(row);
                int colNum = Integer.parseInt(col);


                if (rowNum >= 21 || colNum >= 21 || rowNum <= 0 || colNum <= 0) {
                    // Row or col values are out of range, display an error message or handle it as needed
                    // You can show a toast message or any other UI feedback to indicate the invalid input
                    Toast.makeText(requireContext(), "Row and col values must be between 1 and 20", Toast.LENGTH_SHORT).show();
                } else if (Integer.parseInt(obstacleId) < 1 || Integer.parseInt(obstacleId) > 8) {
                    Toast.makeText(requireContext(), "Obstacle id values must be between 1 and 8 inclusive", Toast.LENGTH_SHORT).show();
                }// Check if row and col are within valid range (1 to 20)
                else if (rowNum >= 1 && rowNum <= 20 && colNum >= 1 && colNum <= 20 && Integer.parseInt(obstacleId) >= 1 && Integer.parseInt(obstacleId) <= 8) {
                    // Row and col are valid, you can proceed to add the ImageView to the grid layout

                    // Find the corresponding ImageView from the horizontal layout
                    ImageView obstacleImageView = rootView.findViewWithTag("obstacle_" + obstacleId);
                    ViewGroup owner = (ViewGroup) obstacleImageView.getParent();
                    if (owner != null) {
                        owner.removeView(obstacleImageView);
                    }

                    String currentObstacle = removeObstacle(rowNum, colNum);
                    //if there is a current obstacle in this grid cell.
                    Log.d("Return value of curr obstacle", currentObstacle);
                    if (!currentObstacle.equals("")) {
                        //remove the currentObstacle here;
                        ImageView currentObstacleImageView = rootView.findViewWithTag(currentObstacle);
                        ViewGroup ownerCurrObstacle = (ViewGroup) currentObstacleImageView.getParent();
                        if (ownerCurrObstacle != null) {
                            ownerCurrObstacle.removeView(currentObstacleImageView);
                        }

                        // Retrieve the tag to identify the dropped drawable
                        String drawableTag = (String) currentObstacleImageView.getTag();

                        // Reset the imageInfo back to the default values.
                        ImageInfo imageInfo = imageInfoMap.get(drawableTag);
                        if (imageInfo != null) {
                            imageInfo.setDirection("None");
                            imageInfo.setRow(-1);
                            imageInfo.setCol(-1);
                        }

                        setGridCellBorderColor("None");
                        LinearLayout linearLayout = rootView.findViewById(R.id.horizontalScrollViewLayout);

                        // Set appropriate layout parameters for the image
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(25, 25);
                        params.setMargins(10, 0, 10, 0);
                        currentObstacleImageView.setLayoutParams(params);
                        currentObstacleImageView.setPadding(0, 0, 0, 0);

                        // Add ImageView to the HorizontalScrollView
                        linearLayout.addView(currentObstacleImageView);
                        currentObstacleImageView.setVisibility(View.VISIBLE);

                        // Remove item from obstacleMap
                        obstacleMap.remove(drawableTag);
                    }

                    // Set its row and column in the grid layout
                    ImageInfo obstacleInfo = imageInfoMap.get("obstacle_" + obstacleId);
                    obstacleInfo.setRow(rowNum);
                    obstacleInfo.setCol(colNum);
                    obstacleInfo.setDirection(direction.toLowerCase());

                    addObstacleToMap("obstacle_" + obstacleId, obstacleInfo);
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = 0;
                    params.height = 0;
                    params.rowSpec = GridLayout.spec(rowArr[rowNum], 1f);
                    params.columnSpec = GridLayout.spec(colNum, 1f);
                    params.setMargins(5, 5, 5, 5);
                    obstacleImageView.setLayoutParams(params);

                    GridLayout gridLayout = rootView.findViewById(R.id.gridLayout);
                    if (gridLayout != null) {
                        gridLayout.addView(obstacleImageView);
                    }

                    obstacleImageView.setPadding(0, 5, 0, 5);
                    obstacleImageView.setVisibility(View.VISIBLE);

                    selectedImageView = obstacleImageView;
                    selectedGridCell = obstacleImageView;

                    // Set the color of the grid based on direction (you can implement this logic)
                    setGridCellBorderColor(direction);
                }
        } catch(NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter all fields.", Toast.LENGTH_SHORT).show();
        }
    }

    //this is to return currObstacle if there is one in the current grid cell
    public String removeObstacle(int row, int col){
        String isOccupiedKey = "";
        for (HashMap.Entry<String, JSONObject> entry : obstacleMap.entrySet()) {
            String key = entry.getKey();
            JSONObject obstacleData = entry.getValue();
            // Extract values from the JSONObject
            int x = obstacleData.optInt("x", -1)+1;
            int y = obstacleData.optInt("y", -1)+1;
            if (y == row && x == col) {
                isOccupiedKey = key;
                break;
            }
        }
        return isOccupiedKey;
    }
    BroadcastReceiver incomingMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("receivingMsg");

            // Update the UI
//            incomingText.setText(msg);

            // Process the JSON message
            processJsonMessage(msg);
        }
    };


    //BROADCAST RECEIVER FOR BLUETOOTH CONNECTION STATUS
    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Receiving btConnectionStatus Msg!!!");

            String connectionStatus = intent.getStringExtra("ConnectionStatus");
            myBTConnectionDevice = intent.getParcelableExtra("Device");
            if (myBTConnectionDevice == null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String lastDeviceAddress = prefs.getString("lastConnectedDeviceAddress", null);

                if (lastDeviceAddress != null) {
                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    myBTConnectionDevice = adapter.getRemoteDevice(lastDeviceAddress);
                }
            }
            //myBTConnectionDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //DISCONNECTED FROM BLUETOOTH CHAT
            if (connectionStatus.equals("disconnect")) {
//                BluetoothSocket socket = null;
//                try{
//                    socket = myBTConnectionDevice.createRfcommSocketToServiceRecord(myUUID);
//
//                } catch (IOException e){
//                    Log.d("Socket reconnectino", "PLeaser recoonect");
//                }
//                if (socket != null) {
//                    try {
//                        socket.close();
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }

                Intent connectIntent = new Intent(getActivity(), BluetoothConnectionService.class);



                Log.d("SettingsFragment:", "Device Disconnected");
                connectedDevice = null;
                connectedState = false;
                connectionStatusBox.setText(R.string.btStatusOffline);

                Toast.makeText(requireContext(),"I am disconnected from RPI", Toast.LENGTH_SHORT).show();


                //START BT CONNECTION SERVICE
                connectIntent.putExtra("serviceType", "connect");
                connectIntent.putExtra("device", myBTConnectionDevice);
                connectIntent.putExtra("id", myUUID);

                Connect myConnection = new Connect();

                myConnection.startBTConnection(myBTConnectionDevice, myUUID, requireContext());
                connectedDevice = myBTConnectionDevice.getName();
                connectedState = true;
                connectionStatusBox.setText(connectedDevice);

//                Toast.makeText(requireActivity(), "Connection Established: " + myBTConnectionDevice.getName(), Toast.LENGTH_SHORT).show();


            }
            //SUCCESSFULLY CONNECTED TO BLUETOOTH DEVICE
            else if (connectionStatus.equals("connect")) {

                if (myBTConnectionDevice != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    prefs.edit().putString("lastConnectedDeviceAddress", myBTConnectionDevice.getAddress()).apply();
                    connectedDevice = myBTConnectionDevice.getName();
                    connectedState = true;
                    Log.d("MainActivity:", "Device Connected " + connectedState);
                    connectionStatusBox.setText(connectedDevice);
                    Toast.makeText(requireActivity(), "Connection Established: " + myBTConnectionDevice.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("MainActivity:", "myBTConnectionDevice is null");
                }
            }


            //BLUETOOTH CONNECTION FAILED
            else if (connectionStatus.equals("connectionFail")) {
                Toast.makeText(requireActivity(), "Connection Failed: " + myBTConnectionDevice.getName(),
                        Toast.LENGTH_LONG).show();
            }

        }
    };

    private String[] delimiterMsg(String msg, String delimiter) {

        return (msg.toLowerCase()).split(delimiter);
    }

    public void processJsonMessage(String jsonMsg) {
        try {
            JSONObject mainObject = new JSONObject(jsonMsg);
            String category = mainObject.getString("cat");

            switch (category) {
                case "location":
                    JSONObject valueObj = mainObject.getJSONObject("value");
                    int x = valueObj.getInt("x");
                    int y = valueObj.getInt("y");
                    int d = valueObj.getInt("d");

                    // I'm assuming you want to use the 'x' and 'y' values as newCol and newRow respectively.
                    // Adjust if needed.
                    updateRobot(y, x, d, "123"); // Hardcoding "MOVE" as action, change as needed.
                    break;

                case "image-rec":
                    valueObj = mainObject.getJSONObject("value");
                    String image_id = valueObj.getString("image_id");
                    String obstacle_id = valueObj.getString("obstacle_id");
                    Log.d("Scanning images", "Obstacle_id value:" + obstacle_id);
                    String obstacle_string = obstacleMap.keySet().toArray()[Integer.parseInt(obstacle_id)-1].toString();

                    Log.d("Scanning images", obstacle_string + "this is the obstacle ID of images");
                    updateTarget(image_id, obstacle_string);
                    break;


                // Add other cases as needed

                case "info":
                    String statusMessage = mainObject.getString("value");
                    Log.d("Status message", statusMessage);
                    statusContent.setText(statusMessage);
                    break;

                default:
                    Log.w("ProcessJson", "Unknown category: " + category);
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("ProcessJson", "Error processing JSON message", e);
        }
    }

    //>>BLuetooth receive from RPi
    /*
    public void receiveFromRPi(JSONObject payload){
        cat = jsonobject.get('cat');
        switch(cat){
            'info'
                textview statusmessage = r.
             '
    }

     */
}

/*

//>>Data to be sent as requested by ALGO team
{x: "", y: "", direction: "", obstacle_id:""}
where directions:
    90: North
    180: West
    0: East
    -90: South
 */

//From RPi team
//start command
//robot start at 1,1
//One obstacle list one short.

//ky send uuid to JH
//when send to algo team minus 1 from row and col, theirs is 0 index based
