package com.example.mdp_android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

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

    public void setDirection(String direction) {
        this.direction = direction;
    }
}

public class MapFragment extends Fragment {

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


        // Find the buttons by their IDs
        Button buttonLeft = rootView.findViewById(R.id.buttonLeft);
        Button buttonRight = rootView.findViewById(R.id.buttonRight);
        Button buttonUp = rootView.findViewById(R.id.buttonUp);
        Button buttonDown = rootView.findViewById(R.id.buttonDown);
        Button buttonSendToRpi = rootView.findViewById(R.id.buttonSendObstacle);
        // This two are part of testing (can be removed afterwards)
        Button buttonRotate = rootView.findViewById(R.id.buttonRotate);
        Button buttonChange = rootView.findViewById(R.id.buttonChange);

        // Set click listeners for the buttons
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call updateRobot function to move left
                ImageInfo robotInfo = imageInfoMap.get("robot");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow(), robotInfo.getCol() - 1, robotInfo.getDirection(), "MOVE");
                }
            }
        });

        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  Call updateRobot function to move right
                ImageInfo robotInfo = imageInfoMap.get("robot");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow(), robotInfo.getCol() + 1, robotInfo.getDirection(), "MOVE");
                }
            }
        });

        buttonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call updateRobot function to move up
                ImageInfo robotInfo = imageInfoMap.get("robot");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow() + 1, robotInfo.getCol(), robotInfo.getDirection(), "MOVE");
                }
            }
        });

        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call updateRobot function to move down
                ImageInfo robotInfo = imageInfoMap.get("robot");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow() - 1, robotInfo.getCol(), robotInfo.getDirection(), "MOVE");
                }
            }
        });

        buttonSendToRpi.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // Call sendObstacleToRpi here
                sendObstacleToRpi();
            }
        });

        buttonRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For testing purpose, hardcoded to show that robot direction will change
                ImageInfo robotInfo = imageInfoMap.get("robot");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow(), robotInfo.getCol(), "North", "ROBOT");
                }
            }
        });

        buttonChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hardcoded to show that target & obstacle will change if mismatch & background will be changed
                updateTarget("11", "12");
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
                    rowLabelTextView.setTextSize(10);

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
                    colLabelTextView.setTextSize(10);

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
                params.setMargins(10,10,10,10);

                // Set background color for the cellImageView
                cellImageView.setBackgroundColor(Color.parseColor("#D3D3D3"));

                // Add the ImageView to the GridLayout
                cellImageView.setLayoutParams(params);
                gridLayout.addView(cellImageView);
                }
            }
        }

        // Load and add drawables dynamically to HorizontalScrollView
        for (int i = 0; i <= 30; i++) {
            // Create an ImageView
            ImageView imageView = new ImageView(requireContext());

            // Set the image resource based on the drawable name
            int drawableResource;
            // If its 0, i will load the robot icon
            if(i == 0){
                drawableResource = R.drawable.robot_icon;

                imageView.setTag("robot");
            } else{ // Loading other obstacles
                int obstacleId = i + 10;
                String resourceName = "obstacle_" + obstacleId;
                drawableResource = getResources().getIdentifier(resourceName, "drawable", requireContext().getPackageName());

                // Assign a tag to the ImageView to represent the drawable
                imageView.setTag("obstacle_" + obstacleId); // Use a unique identifier for each drawable to add into hash maps
            }
            imageView.setImageResource(drawableResource);

            // Set layout parameters for the ImageView
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40, 40);
            params.setMargins(20, 0, 20, 0);
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
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40, 40);
                            params.setMargins(20, 0, 20, 0);
                            draggedImage.setLayoutParams(params);
                            draggedImage.setPadding(0,0,0,0);

                            // Add ImageView to the HorizontalScrollView
                            linearLayout.addView(draggedImage);
                            draggedImage.setVisibility(View.VISIBLE);

                            // Remove item from obstacleMap
                            obstacleMap.remove(drawableTag);
                            Log.d("Updated Obstacle Map", obstacleMap.toString());
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
    // Function to show a dialog for selecting a direction
    private void showDirectionSelectionDialog() {
        // Create an array of direction options
        final String[] directions = {"North", "South", "East", "West", "None"};

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

    // Function to set the border color of the selected grid cell
    private void setGridCellBorderColor(String direction) {
        if (selectedImageView != null && selectedGridCell != null) {
            @ColorInt int color;
            switch (direction) {
                case "North":
                    color = Color.parseColor("#FF6961");
                    break;
                case "South":
                    color = Color.parseColor("#77DD77");
                    break;
                case "East":
                    color = Color.parseColor("#A7C7E7");
                    break;
                case "West":
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

                //Send the data to bluetooth here, this is the one with the updated direction + x,y
                Log.d("ImageInfo", imageInfo.toString());
                //Bluetooth>>sendIMageInfoToBluetooth(imageInfo)
                //maybe consider sending the tool for deliverable purpose, in actual test maybe just
                //send the entire JSON as per request by RPi team.
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
                        params.setMargins(10, 10, 10, 10);
                        draggedImage.setLayoutParams(params);

                        // Add the image to the specified position in the grid
                        targetGrid.addView(draggedImage);
                        draggedImage.setPadding(0,10,0,10);
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
                            //Bluetooth>>can send the bluetooth information over here, but over here the details would
                            //be x,y, direction="None", unless direction is set, ie to say the obstacle is
                            //already on the map and is just being moved.
                            //have to put in this branch as the below one is checking if its outside of the grid.
                            //Again maybe just send the info to the tool for deliverable purpose.
                            //Actual test send the way JSON as a whole as requested by RPi.
                        }
                    } else if (v != targetGrid) {
                        // If the drop position is not valid within the grid,
                        // set appropriate layout parameters for the image
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40, 40);
                        params.setMargins(20, 0, 20, 0);
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
    private void updateRobot(int newRow, int newCol, String direction, String action) {
        // Find the robot's ImageView by its tag, robot's tag is hardcoded to be obstacle_0
        ImageView robotImageView = rootView.findViewWithTag("robot");
        selectedImageView = robotImageView;
        selectedGridCell = robotImageView;

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
            //>> bluetooth Send the details to RPi for robot to move;
            //need to -1 from row and col when sending to RPI;
        }
    }



    // Call this to update the target's image + cell background simultaneously.
    //>>Bluetooth to call this to update the target's background
    private void updateTarget(String target, String obstacle_id) {
        String targetTag = "obstacle_" + target;
        String obstacleTag = "obstacle_" + obstacle_id;
        ImageView obstacleImageView = rootView.findViewWithTag(obstacleTag);
        ImageView targetImageView = new ImageView(requireContext());

        // Set the image resource based on the drawable name
        int drawableResource;
        drawableResource = getResources().getIdentifier(targetTag, "drawable", requireContext().getPackageName());
        targetImageView.setImageResource(drawableResource);

        // First case if target != obstacle, observed target is not the obstacle.
        if (targetImageView != null && obstacleImageView != null && !obstacleTag.equals(targetTag)) {
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
            targetParams.setMargins(10, 10, 10, 10);
            targetImageView.setPadding(0, 10, 0, 10);
            targetImageView.setLayoutParams(targetParams);

            GridLayout gridLayout = rootView.findViewById(R.id.gridLayout);
            if (gridLayout != null) {
                gridLayout.addView(targetImageView);
            }

            // Move the obstacle image back to the linear layout of the horizontal scroll view
            // Set new LayoutParams for the obstacleImageView
            ViewGroup obstacleParent = (ViewGroup) obstacleImageView.getParent();
            if(obstacleParent != null){
                obstacleParent.removeView(obstacleImageView);
            }
            LinearLayout.LayoutParams obstacleParams = new LinearLayout.LayoutParams(40, 40);
            obstacleParams.setMargins(20, 0, 20, 0);
            obstacleImageView.setLayoutParams(obstacleParams);
            obstacleInfo.setDirection("None");
            obstacleInfo.setRow(-1);
            obstacleInfo.setCol(-1);
            obstacleImageView.setPadding(0,0,0,0);

            // Add the obstacle image to the horizontal layout
            LinearLayout horizontalLayout = rootView.findViewById(R.id.horizontalScrollViewLayout);
            if (horizontalLayout != null) {
                horizontalLayout.addView(obstacleImageView);
            }
        } else if(targetImageView != null && obstacleImageView != null && obstacleTag.equals(targetTag)){
            // At this point obstacle image is the correct one so can just change this obstacle image's background
            selectedImageView = obstacleImageView;
            selectedGridCell = obstacleImageView;
            // Change cell to found color
            selectedGridCell.setBackgroundColor(Color.parseColor("#39FF14"));
        }
    }

    // Convert imageInfo to obstacleID
    private void addObstacleToMap(String imageInfoTag, ImageInfo imageInfo) throws JSONException {
        int obstacle_id = obstacleMap.size()+1;
        int dirs;
        switch(imageInfo.getDirection()) {
            case "North":
                dirs = 90;
                break;
            case "South":
                dirs = -90;
                break;
            case "West":
                dirs = 180;
                break;
            case "East":
                dirs = 0;
                break;
            default:
                dirs = -1;
                break;
        }
        JSONObject item = new JSONObject();
        item.put("obstacle_id", obstacle_id);
        item.put("x",imageInfo.getCol()-1);
        item.put("y", imageInfo.getRow()-1);
        item.put("dir", dirs);
        // Add obstacleInfo to hashmap
        obstacleMap.put(imageInfoTag, item);
    }

    /*
        >>Bluetooth
    this is the json format to RPi (Check with RPi team again), JSON object's key might need to be changed.
    current format is
    {imageTag:
        {
            "obstacle_id": 2,
            "x": 1,
            "y":1,
            "dir": 180
         }
     }
    */
    private void sendObstacleToRpi(){
        JSONObject json = new JSONObject(obstacleMap);
        Log.d("Obstacle json", json.toString());
    }
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
