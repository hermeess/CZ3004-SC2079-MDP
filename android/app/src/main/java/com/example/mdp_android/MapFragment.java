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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    private HashMap<String, ImageInfo> imageInfoMap = new HashMap<>(); //used to store my imageInfo for all images.

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
   2.To update the target, call the updateTarget function alongside the targetid & obstacle id

   Data structure that is keeping track of all the image etc:
   A hashmap is used to keep track of all the images.
    */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_map, container, false);

        HorizontalScrollView horizontalScrollView = rootView.findViewById(R.id.horizontalScrollView);
        LinearLayout linearLayout = rootView.findViewById(R.id.horizontalScrollViewLayout);
        GridLayout gridLayout = rootView.findViewById(R.id.gridLayout);


        // Find the buttons by their IDs
        Button buttonLeft = rootView.findViewById(R.id.buttonLeft);
        Button buttonRight = rootView.findViewById(R.id.buttonRight);
        Button buttonUp = rootView.findViewById(R.id.buttonUp);
        Button buttonDown = rootView.findViewById(R.id.buttonDown);
        //this two are part of testing (can be removed afterwards)
        Button buttonRotate = rootView.findViewById(R.id.buttonRotate);
        Button buttonChange = rootView.findViewById(R.id.buttonChange);

        // Set click listeners for the buttons
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call your moveRobot function with the appropriate parameters
                // For example, to move left, you can decrease the column position by 1
                ImageInfo robotInfo = imageInfoMap.get("obstacle_0");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow(), robotInfo.getCol() - 1, robotInfo.getDirection(), "MOVE");
                }
            }
        });

        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For example, to move right, you can increase the column position by 1
                ImageInfo robotInfo = imageInfoMap.get("obstacle_0");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow(), robotInfo.getCol() + 1, robotInfo.getDirection(), "MOVE");
                }
            }
        });

        buttonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For example, to move up, you can decrease the row position by 1
                ImageInfo robotInfo = imageInfoMap.get("obstacle_0");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow() + 1, robotInfo.getCol(), robotInfo.getDirection(), "MOVE");
                }
            }
        });

        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For example, to move down, you can increase the row position by 1
                ImageInfo robotInfo = imageInfoMap.get("obstacle_0");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow() - 1, robotInfo.getCol(), robotInfo.getDirection(), "MOVE");
                }
            }
        });

        buttonRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call your moveRobot function with the appropriate parameters
                // For example, to rotate, you can change the direction
                ImageInfo robotInfo = imageInfoMap.get("obstacle_0");
                if(robotInfo.getCol() != -1 && robotInfo.getRow() != -1) {
                    updateRobot(robotInfo.getRow(), robotInfo.getCol(), "North", "ROBOT");
                }
            }
        });

        buttonChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For example, to change the image for "obstacle_5" from obstacle_6
                updateTarget("5", "6");
            }
        });

        //create my grid view
        for (int row = 0; row < 21; row++) {
            for (int col = 0; col < 21; col++) {
                if(col == 0 && row == 0){
                    continue;
                }
                //row ticks
                if(col == 0){
                    TextView rowLabelTextView = new TextView(requireContext());
                    rowLabelTextView.setText(String.valueOf(row));
                    rowLabelTextView.setTextSize(10);

                    // Set layout parameters for the column label
                    GridLayout.LayoutParams rowLabelParams = new GridLayout.LayoutParams();
                    rowLabelParams.width = 0;
                    rowLabelParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    rowLabelParams.rowSpec = GridLayout.spec(row,1f);
                    rowLabelParams.columnSpec = GridLayout.spec(col, 1f);
                    rowLabelTextView.setLayoutParams(rowLabelParams);

                    rowLabelTextView.setGravity(Gravity.CENTER);

                    // Add the row label to your gridLayout
                    gridLayout.addView(rowLabelTextView);
                } else if(row == 0){ //col ticks
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
                params.rowSpec = GridLayout.spec(row, 1f);
                params.columnSpec = GridLayout.spec(col, 1f);
                params.setMargins(10,10,10,10);

                // Set background for the cellImageView
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
            //if its 0, i will load the robot icon
            if(i == 0){
                drawableResource = getResources().getIdentifier("robot_icon", "drawable", requireContext().getPackageName());
            } else{ //loading other obstacles
                drawableResource = getResources().getIdentifier("obstacle_" + i, "drawable", requireContext().getPackageName());
            }
            imageView.setImageResource(drawableResource);

            // Set layout parameters for the ImageView
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40, 40);
            params.setMargins(20, 0, 20, 0);
            imageView.setLayoutParams(params);

            // Assign a tag to the ImageView to represent the drawable
            imageView.setTag("obstacle_" + i); // Use a unique identifier for each drawable to add into hash

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
                    switch (action) {
                        case DragEvent.ACTION_DROP:
                            // Handle the drop event
                            ImageView draggedImage = (ImageView) event.getLocalState();
                            GridLayout targetGrid = (GridLayout) gridLayout;

                            // Remove the image from its original parent (the GridLayout)
                            ViewGroup owner = (ViewGroup) draggedImage.getParent();
                            if (owner != null) {
                                owner.removeView(draggedImage);
                            }

                            // Retrieve the tag to identify the dropped drawable
                            String drawableTag = (String) draggedImage.getTag();

                            //reset the imageInfo back to the default values.
                            ImageInfo imageInfo = imageInfoMap.get(drawableTag);
                            if (imageInfo != null) {
                                imageInfo.setDirection("None");
                                imageInfo.setRow(-1);
                                imageInfo.setCol(-1);
                                //log to see if it is resets to original values
                                Log.d("ImageInfo", "ImageView: " + draggedImage.toString() +
                                        " Row: " + imageInfo.getRow() +
                                        " Column: " + imageInfo.getCol() +
                                        " Direction: " + imageInfo.getDirection());
                            }

                            setGridCellBorderColor("None");
                            // If the drop position is not valid within the grid,
                            // set appropriate layout parameters for the image
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40, 40);
                            params.setMargins(20, 0, 20, 0);
                            draggedImage.setLayoutParams(params);

                            // Add ImageView to the HorizontalScrollView
                            linearLayout.addView(draggedImage);
                            draggedImage.setVisibility(View.VISIBLE);

                            //>>Bluetooth can send "Remove" e.g to remove the obstacle from map etc
                            return true;
                        default:
                            break;
                    }
                    return true;
                }
            });
            String imageTag = (String) imageView.getTag();

            //initialise the imageInfo for each imageView
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

                // Dismiss the dialog
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
            }

            // Log the ImageInfo only if it's not null
            if (imageInfo != null) {
                Log.d("ImageInfo", "Image Tag: " + selectedImageViewTag.toString() +
                        " Row: " + imageInfo.getRow() +
                        " Column: " + imageInfo.getCol() +
                        " Direction: " + imageInfo.getDirection());
            }
        }
    }


    public class MyDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Check if this is a valid drop target (your 20x20 grid)
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

                    // Update the image's row and column in the map
                    String draggedImageTag = (String) draggedImage.getTag();
                    ImageInfo imageInfo = imageInfoMap.get(draggedImageTag);
                    if (imageInfo != null) {
                        imageInfo.setRow(row);
                        imageInfo.setCol(column);
                        Log.d("ImageInfo", "ImageTag: " + draggedImageTag +
                                " Row: " + imageInfo.getRow() +
                                " Column: " + imageInfo.getCol() +
                                " Direction: " + imageInfo.getDirection());
                    }

                    if (row >= 0 && column >= 0 && v == targetGrid) {
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
                        draggedImage.setVisibility(View.VISIBLE);

                        //Bluetooth>>can send the bluetooth information over here, but over here the details would
                        //be x,y, direction="None", unless direction is set, ie to say the obstacle is
                        //moved on the map.

                        //have to put in this branch as the below one is checking if its outside of the grid.

                    } else if (v != targetGrid) {
                        // If the drop position is not valid within the grid,
                        // set appropriate layout parameters for the image
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40, 40);
                        params.setMargins(20, 0, 20, 0); // Set margins (left, top, right, bottom)
                        draggedImage.setLayoutParams(params);

                        // Append the image to the end of the LinearLayout
                        LinearLayout horizontalLayout = (LinearLayout) v.findViewById(R.id.horizontalScrollViewLayout);
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
    private void updateRobot(int newRow, int newCol, String direction, String action) {
        // Find the robot's ImageView by its tag, robot's tag is hardcoded to be obstacle_0
        ImageView robotImageView = rootView.findViewWithTag("obstacle_0");
        selectedImageView = (ImageView) robotImageView;
        selectedGridCell = (ImageView) robotImageView;

        if (robotImageView != null) {
            // Update the robot's position in the ImageInfo map
            ImageInfo robotInfo = imageInfoMap.get("obstacle_0");
            robotInfo.setRow(newRow);
            robotInfo.setCol(newCol);
            robotInfo.setDirection(direction);
            setGridCellBorderColor(direction);

            // Calculate the position in the GridLayout
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) robotImageView.getLayoutParams();
            params.rowSpec = GridLayout.spec(newRow, 1f);
            params.columnSpec = GridLayout.spec(newCol, 1f);

            // Update the ImageView's layout parameters
            robotImageView.setLayoutParams(params);
        }
        if(action == "MOVE"){
            //>> bluetooth Send the details to RPI for robot to move;
        }
    }



    //Call this to update the target's image + cell background simultaneously.
    //Bluetooth>>Bluetooth to call this to update the target's background
    private void updateTarget(String target, String obstacle_id) {
        String targetTag = "obstacle_" + target;
        String obstacleTag = "obstacle_" + obstacle_id;
        ImageView obstacleImageView = rootView.findViewWithTag(obstacleTag);
        ImageView targetImageView = new ImageView(requireContext());

        // Set the image resource based on the drawable name
        int drawableResource;
        //if its 0, i will load the robot icon
        drawableResource = getResources().getIdentifier(targetTag, "drawable", requireContext().getPackageName());
        targetImageView.setImageResource(drawableResource);

        //First case if target != obstacle, observed target is not the intended one.
        if (targetImageView != null && obstacleImageView != null && !obstacleTag.equals(targetTag)) {
            ImageInfo obstacleInfo = imageInfoMap.get(obstacleTag);
            ImageInfo targetInfo = imageInfoMap.get(targetTag);
            int obstacleRow = obstacleInfo.getRow();
            int obstacleCol = obstacleInfo.getCol();

            // Set the target row and col
            targetInfo.setRow(obstacleRow);
            targetInfo.setCol(obstacleCol);
            targetInfo.setDirection(obstacleInfo.getDirection());

            //reset obstacleImageView first
            selectedImageView = (ImageView) obstacleImageView;
            selectedGridCell = (ImageView) obstacleImageView;
            selectedGridCell.setBackgroundColor(Color.parseColor("#D3D3D3"));

            //change the target grid cell's background to black to denote found.
            //have to update selectedImageView and selectedGridCell
            selectedImageView = (ImageView) targetImageView;
            selectedGridCell = (ImageView) targetImageView;
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
            targetParams.rowSpec = GridLayout.spec(targetInfo.getRow(), 1f);
            targetParams.columnSpec = GridLayout.spec(targetInfo.getCol(), 1f);
            targetParams.setMargins(10, 10, 10, 10);
            targetImageView.setLayoutParams(targetParams); // Set new LayoutParams for the targetImageView

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

            // Add the obstacle image to the horizontal layout
            LinearLayout horizontalLayout = rootView.findViewById(R.id.horizontalScrollViewLayout);
            if (horizontalLayout != null) {
                horizontalLayout.addView(obstacleImageView);
            }
            Log.d("Change Image", "Change Success " + targetInfo.getCol() + targetInfo.getRow());
        } else if(targetImageView != null && obstacleImageView != null && obstacleTag.equals(targetTag)){
            //at this point obstacle image is the correct one so can just change this obstacle image.
            selectedImageView = (ImageView) obstacleImageView;
            selectedGridCell = (ImageView) obstacleImageView;
            //change cell to found color
            selectedGridCell.setBackgroundColor(Color.parseColor("#39FF14"));
        }
    }
}