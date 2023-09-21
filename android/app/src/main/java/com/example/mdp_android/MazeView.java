package com.example.mdp_android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.nio.charset.Charset;

/**
 * Created by darks on 24-Feb-18.
 */

public class MazeView extends View {

    private static final String TAG = "MazeView";
    private static Cell[][] cells;
    private static final int COLS = 15, ROWS = 20;
    private static final float WallThickness = 2;
    private static float cellSize, hMargin, vMargin;
    private static Paint wallPaint, robotPaint, waypointPaint, directionPaint,  emptyPaint, virtualWallPaint, obstaclePaint, unexploredPaint, ftpPaint, endPointPaint, gridNumberPaint;
    private static int robotRow = 18, robotCols = 1, wayPointRow =-1, wayPointCols=-1;
    private static String robotDirection = "east";
    private static boolean setRobotPostition = false, setWayPointPosition = false;
    private static boolean createCellStatus = false;

    public MazeView(Context context) {
        super(context);
        init(null);

    }

    //CONSTRUCTOR
    public MazeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);

        //PAINT FOR END POINT
        endPointPaint = new Paint();
        endPointPaint.setColor(Color.RED);

        //PAINT THE THICKNESS OF THE WALL
        wallPaint = new Paint();
        wallPaint.setColor(Color.BLACK);
        wallPaint.setStrokeWidth(WallThickness);

        //COLOR FOR ROBOT
        robotPaint = new Paint();
        robotPaint.setColor(Color.GREEN);

        //COLOR FOR ROBOT DIRECTION
        directionPaint = new Paint();
        directionPaint.setColor(Color.BLACK);

        //COLOR FOR WAY POINT
        waypointPaint = new Paint();
        waypointPaint.setColor(Color.YELLOW);

        //COLOR FOR EXPLORED BUT EMPTY
        emptyPaint = new Paint();
        emptyPaint.setColor(Color.WHITE);

        //COLOR FOR VIRTUAL WALL
        virtualWallPaint = new Paint();
        virtualWallPaint.setColor(Color.parseColor("#FFA500"));

        //COLOR FOR OBSTACLE
        obstaclePaint = new Paint();
        obstaclePaint.setColor(Color.BLACK);

        //COLOR FOR UNEXPLORED PATH
        unexploredPaint = new Paint();
        unexploredPaint.setColor(Color.GRAY);

        gridNumberPaint = new Paint();
        gridNumberPaint.setColor(Color.BLACK);
        gridNumberPaint.setTextSize(18);
        gridNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);

        //COLOR FOR FASTEST PATH
        ftpPaint = new Paint();
        ftpPaint.setColor(Color.parseColor("#FFC0CB"));


    }

    private void init(@Nullable AttributeSet attrs){
        setWillNotDraw(false);
    }

    //CREATE Cell METHOD
    private void createCell() {
        cells = new Cell[COLS][ROWS];

        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {

                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize - (cellSize / 40), (y + 1) * cellSize - (cellSize / 60), unexploredPaint);

            }
        }

    }

    //ON TOUCH METHOD
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int coordinates[];
        float x = event.getX();
        float y = event.getY();

        coordinates = findGridOnTouch(x, y);
        Log.d(TAG, "touch coordiantes after finding grid:" + coordinates[0] + " " + coordinates[1]);


        if (setRobotPostition) {
            //ENSURE ONTOUCH IS WITHIN THE MAZE
            if (coordinates[0] != -1 && coordinates[1] != -1) {

                //ENSURE COORDINATES IS NOT THE FIRST OR LAST ROW/COLS AS THE ROBOT IS PLOT BASED ON THE CENTER COORDINATES
                if ((coordinates[0] != 0 && coordinates[0] != 14) && (coordinates[1] != 0 && coordinates[1] != 19)) {
                    robotCols = coordinates[0];
                    robotRow = coordinates[1];
                    invalidate();

                    //SEND START POINT COORDINATES TO RPI
                    //MainActivity.sendStartPoint(robotCols,inverseCoordinates(coordinates[1]));

                }
            }
        } else if (setWayPointPosition) {

            //ENSURE ONTOUCH IS WITHIN THE MAZE
            if (coordinates[0] != -1 && coordinates[1] != -1 && SettingsFragment.connectedDevice != null) {

                wayPointCols = coordinates[0];
                wayPointRow = coordinates[1];
                invalidate();
                Log.d(TAG,"setwaypoint before !!" + setWayPointPosition);

                int wayPoint[] = getWaypoint();

                //SEND WAY POINT TO RPI
                String wpCoordinates = "Algorithm|Android|SetWayPoint|" + wayPoint[0] + "," + wayPoint[1];
                byte[] bytes = wpCoordinates.getBytes(Charset.defaultCharset());
                BluetoothChat.writeMsg(bytes);

                Toast.makeText(getContext(), "WayPoint Sent!!",
                        Toast.LENGTH_SHORT).show();

                setWayPointPosition = false;
                Log.d(TAG,"setwaypoint after !!" + setWayPointPosition);


            }
            else {
                Toast.makeText(getContext(), "Please Connect to a Device First!!",
                        Toast.LENGTH_SHORT).show();
                setWayPointPosition = false;

            }
        }


        return super.onTouchEvent(event);

    }

    //DRAW SHAPES ON CANVAS
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Log.d(TAG,"Repainting now!!");

        //BACKGROUND COLOR OF CANVAS
        canvas.drawColor(Color.WHITE);

        //WIDTH OF THE CANVAS
        int width = getWidth();
        //HEIGHT OF THE CANVAS
        int height = getHeight();

        //CALCULATE MARGIN SIZE FOR THE CANVAS
        hMargin = (width - COLS * cellSize) / 2;
        vMargin = (height - ROWS * cellSize) / 2;

        //CALCULATE THE CELLSIZE BASED ON THE DIMENSIONS OF THE CANVAS
        if (width / height < COLS / ROWS) {
            cellSize = width / (COLS + 1);
        } else {
            cellSize = height / (ROWS + 1);
        }

        //CREATE CELL ONCE
        if(!createCellStatus) {
            //CREATE CELL COORDINATES
            Log.d(TAG,"CREATE CELL");
            createCell();
            createCellStatus = true;
        }

        //SET THE MARGIN IN PLACE
        canvas.translate(hMargin, vMargin);

        //DRAW BORDER FOR EACH CELL
        drawBorder(canvas);

        //DRAW EACH INDIVIDUAL CELL
        drawCell(canvas);

        //DRAW GRID NUMBER
        drawGridNumber(canvas);

        //DRAW ENDPOINT ON MAZE
        drawEndPoint(canvas);

        //DRAW ROBOT ON MAZE
        drawRobot(canvas);

        setRobotPostition = false;

        //DRAW WAY POINT ON MAZE
        drawWayPoint(canvas);

        Log.d(TAG, "Drawing Way Point!!");



    }

    //DRAW ENDPOINT CELL
    private void drawEndPoint(Canvas canvas) {

        for (int x = 12; x < COLS; x++) {
            for (int y = 0; y < 3; y++) {

                //DRAW EACH INDIVIDUAL CELL
                canvas.drawRect(cells[x][y].startX,cells[x][y].startY,cells[x][y].endX,cells[x][y].endY,endPointPaint);

            }
        }
    }


    //DRAW INDIVIDUAL CELL
    private void drawCell(Canvas canvas){

        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {

                //DRAW EACH INDIVIDUAL CELL
                canvas.drawRect(cells[x][y].startX,cells[x][y].startY,cells[x][y].endX,cells[x][y].endY,cells[x][y].paint);

            }
        }
    }

    //DRAW BORDER FOR EACH CELL
    private void drawBorder(Canvas canvas){

        //DRAW BORDER FOR EACH CELL
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {

                //DRAW LINE FOR TOPWALL OF CELL
                canvas.drawLine(
                        x * cellSize,
                        y * cellSize,
                        (x + 1) * cellSize,
                        y * cellSize, wallPaint);
                //DRAW LINE FOR RIGHTWALL OF CELL
                canvas.drawLine(
                        (x + 1) * cellSize,
                        y * cellSize,
                        (x + 1) * cellSize,
                        (y + 1) * cellSize, wallPaint);
                //DRAW LINE FOR LEFTWALL OF CELL
                canvas.drawLine(
                        x * cellSize,
                        y * cellSize,
                        x * cellSize,
                        (y + 1) * cellSize, wallPaint);
                //DRAW LINE FOR BOTTOMWALL OF CELL
                canvas.drawLine(
                        x * cellSize,
                        (y + 1) * cellSize,
                        (x + 1) * cellSize,
                        (y + 1) * cellSize, wallPaint);
            }
        }
    }

    //DRAW ROBOT ON THE CANVAS
    private void drawRobot(Canvas canvas) {


        float halfWidth = (cells[robotCols][robotRow - 1].endX - cells[robotCols][robotRow - 1].startX) / 2;

        //DRAW COLOR FOR REST OF THE ROBOT
        canvas.drawRect(cells[robotCols][robotRow - 1].startX, cells[robotCols][robotRow - 1].startY, cells[robotCols][robotRow - 1].endX, cells[robotCols][robotRow - 1].endY, robotPaint);
        canvas.drawRect(cells[robotCols][robotRow].startX, cells[robotCols][robotRow].startY, cells[robotCols][robotRow].endX, cells[robotCols][robotRow].endY, robotPaint);
        canvas.drawRect(cells[robotCols + 1][robotRow].startX, cells[robotCols + 1][robotRow].startY, cells[robotCols + 1][robotRow].endX, cells[robotCols + 1][robotRow].endY, robotPaint);
        canvas.drawRect(cells[robotCols - 1][robotRow].startX, cells[robotCols - 1][robotRow].startY, cells[robotCols - 1][robotRow].endX, cells[robotCols - 1][robotRow].endY, robotPaint);
        canvas.drawRect(cells[robotCols + 1][robotRow - 1].startX, cells[robotCols + 1][robotRow - 1].startY, cells[robotCols + 1][robotRow - 1].endX, cells[robotCols + 1][robotRow - 1].endY, robotPaint);
        canvas.drawRect(cells[robotCols - 1][robotRow - 1].startX, cells[robotCols - 1][robotRow - 1].startY, cells[robotCols - 1][robotRow - 1].endX, cells[robotCols - 1][robotRow - 1].endY, robotPaint);
        canvas.drawRect(cells[robotCols][robotRow + 1].startX, cells[robotCols][robotRow + 1].startY, cells[robotCols][robotRow + 1].endX, cells[robotCols][robotRow + 1].endY, robotPaint);
        canvas.drawRect(cells[robotCols + 1][robotRow + 1].startX, cells[robotCols + 1][robotRow + 1].startY, cells[robotCols + 1][robotRow + 1].endX, cells[robotCols + 1][robotRow + 1].endY, robotPaint);
        canvas.drawRect(cells[robotCols - 1][robotRow + 1].startX, cells[robotCols - 1][robotRow + 1].startY, cells[robotCols - 1][robotRow + 1].endX, cells[robotCols - 1][robotRow + 1].endY, robotPaint);

        //TRIANGLE FOR ROBOT DIRECTION
        Path path = new Path();

        switch (robotDirection){
            case "north":
                path.moveTo(cells[robotCols][robotRow - 1].startX + halfWidth, cells[robotCols][robotRow - 1].startY); // Top
                path.lineTo(cells[robotCols][robotRow - 1].startX, cells[robotCols][robotRow - 1].endY); // Bottom left
                path.lineTo(cells[robotCols][robotRow - 1].endX, cells[robotCols][robotRow - 1].endY); // Bottom right
                path.lineTo(cells[robotCols][robotRow - 1].startX + halfWidth, cells[robotCols][robotRow - 1].startY); // Back to Top
                break;

            case "south":
                path.moveTo(cells[robotCols][robotRow + 1].endX - halfWidth, cells[robotCols][robotRow + 1].endY); // Top
                path.lineTo(cells[robotCols][robotRow + 1].startX, cells[robotCols][robotRow + 1].startY); // Bottom left
                path.lineTo(cells[robotCols + 1][robotRow + 1].startX, cells[robotCols +1][robotRow + 1].startY); // Bottom right
                path.lineTo(cells[robotCols][robotRow + 1].endX - halfWidth, cells[robotCols][robotRow + 1].endY); // Back to Top
                break;

            case "east":
                path.moveTo(cells[robotCols+1][robotRow].startX + (2*halfWidth), cells[robotCols][robotRow].startY + halfWidth); // Top
                path.lineTo(cells[robotCols+1][robotRow].startX, cells[robotCols+1][robotRow].startY); // Bottom left
                path.lineTo(cells[robotCols+1][robotRow+1].startX, cells[robotCols+1][robotRow+1].startY); // Bottom right
                path.lineTo(cells[robotCols+1][robotRow].startX + (2*halfWidth) , cells[robotCols][robotRow].startY + halfWidth); // Back to Top
                break;

            case "west":
                path.moveTo(cells[robotCols-1][robotRow].startX, cells[robotCols][robotRow].startY + halfWidth); // Top
                path.lineTo(cells[robotCols][robotRow].startX, cells[robotCols][robotRow].startY); // Bottom left
                path.lineTo(cells[robotCols][robotRow + 1].startX, cells[robotCols][robotRow  +1].startY); // Bottom right
                path.lineTo(cells[robotCols-1][robotRow].startX, cells[robotCols][robotRow].startY + halfWidth); // Back to Top
                break;
        }
        path.close();
        canvas.drawPath(path, directionPaint);
    }

    //DRAW ROBOT ON THE CANVAS
    private void drawWayPoint(Canvas canvas) {

        if(wayPointRow != -1 && wayPointCols != -1) {
            canvas.drawRect(cells[wayPointCols][wayPointRow].startX, cells[wayPointCols][wayPointRow].startY, cells[wayPointCols][wayPointRow].endX, cells[wayPointCols][wayPointRow].endY, waypointPaint);
        }
    }

    //DRAW NUMBERS ON MAP GRID
    private void drawGridNumber(Canvas canvas) {

        //GRID NUMBER FOR ROW
        for (int x = 0; x < 15; x++) {

            if(x >9 && x <15){

                canvas.drawText(Integer.toString(x), cells[x][19].startX + (cellSize / 5), cells[x][19].endY + (cellSize / 1.5f), gridNumberPaint);
            }
            else {
                //GRID NUMBER FOR ROW
                canvas.drawText(Integer.toString(x), cells[x][19].startX + (cellSize / 3), cells[x][19].endY + (cellSize / 1.5f), gridNumberPaint);

            }
        }

        //GRID NUMBER FOR COLUMN
        for (int x = 0; x <20; x++) {

            if(x >9 && x <20){

                canvas.drawText(Integer.toString(19 - x), cells[0][x].startX - (cellSize / 1.5f), cells[0][x].endY - (cellSize / 3.5f), gridNumberPaint);
            }
            else {

                canvas.drawText(Integer.toString(19 - x), cells[0][x].startX - (cellSize / 1.2f), cells[0][x].endY - (cellSize / 3.5f), gridNumberPaint);

            }
        }
    }


    //FIND COORDIANTES OF THE CELLMAZE BASED ON ONTOUCH
    private int[] findGridOnTouch(float x, float y) {

        int row = -1, cols = -1;

        //FIND COLS OF THE MAZE BASED ON ONTOUCH
        for (int i = 0; i < COLS; i++) {

            if (cells[i][0].endX >= (x - hMargin) && cells[i][0].startX <= (x - hMargin)) {
                cols = i;
                Log.d(TAG, "cols = " + cols);

                break;
            }
        }
        //FIND ROW OF THE MAZE BASED ON ONTOUCH
        for (int j = 0; j < ROWS; j++) {

            if (cells[0][j].endY >= (y - vMargin) && cells[0][j].startY <= (y - vMargin)) {
                row = j;
                Log.d(TAG, "row = " + row);

                break;
            }

        }

        return new int[]{cols, row};
    }


    //Cell Class
    private class Cell {

        float startX, startY, endX, endY;
        Paint paint;

        private Cell(float startX, float startY, float endX, float endY, Paint paint) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
        }

        public void setPaint(Paint paint){
            this.paint = paint;
        }
    }

    //ALLOW USER TO SET WAYPOINT POSITION
    public void setWayPoint(boolean status){
        setWayPointPosition = status;
    }

    //ALLOW USER TO SET ROBOT POSITION
    public void setStartPoint(boolean status){
        setRobotPostition = status;
    }

    //RETURN START POINT OF ROBOT FOR BUTTON CLICK
    public int[] getRobotStartPoint(){
        return new int[] {robotCols, inverseCoordinates(robotRow)};
    }

    //RETURN WAYPOINT FOR BUTTON CLICK
    public int[] getWaypoint(){
        return new int[] {wayPointCols, inverseCoordinates(wayPointRow)};
    }

    //INVERT ROWS COORDINATES TO START FROM BOTTOM
    private int inverseCoordinates(int y){

        //ONLY NEED TO INVERSE ROWS
        return (19 - y);
    }

    //UPDATE MAZE WHEN MAZE INFO ARRIVES
    public void updateMaze(String[] mazeInfo,boolean autoUpdate){

        Log.d(TAG, "Stage 2: " + mazeInfo[0] + " " + mazeInfo[1] + " " + mazeInfo[2] + " " + mazeInfo[3]);
        Log.d(TAG, "Stage 3-1: " + mazeInfo[1]);
        Log.d(TAG, "Stage 3-1: " + mazeInfo[2]);
        Log.d(TAG, "Stage 3-1: " + mazeInfo[3]);

        robotDirection = mazeInfo[1];
        robotCols = Integer.parseInt(mazeInfo[2]);
        robotRow = 19 - Integer.parseInt(mazeInfo[3]);




        int counter =0;

        for (int x = 0; x < ROWS; x++) {
            for (int y = 0; y < COLS; y++) {


                switch (mazeInfo[0].charAt(counter)){
                    case '0':
                        cells[y][x].setPaint(emptyPaint);
                        Log.d(TAG, "Stage 3: empty");

                        break;
                    case '1':
                        cells[y][x].setPaint(virtualWallPaint);
                        Log.d(TAG, "Stage 3: vir");

                        break;
                    case '2':
                        cells[y][x].setPaint(obstaclePaint);
                        Log.d(TAG, "Stage 3: obstaclePaint");

                        break;
                    case '3':
                        cells[y][x].setPaint(unexploredPaint);
                        Log.d(TAG, "Stage 3: unexploredPaint");

                        break;
                    case '4':
                        cells[y][x].setPaint(ftpPaint);
                        Log.d(TAG, "Stage 3: ftpPaint");

                        break;
                }
                counter++;

            }

        }

        //ENSURE AUTO UPDATE TOGGLE BUTTON IS ON
        if(autoUpdate) {
            invalidate();
        }
        Log.d(TAG, "Stage 4: ");


    }

    //REFRESH THE MAZE
    public void refreshMap(){
        invalidate();
    }


}
