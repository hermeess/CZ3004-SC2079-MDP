package com.example.mdp_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;

import org.json.JSONException;


public class ObstacleDialogFragment extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_obstacle, container, false);

        // Initialize UI elements and set up listeners here

        Button cancelButton = view.findViewById(R.id.buttonCancel);
        Button submitButton = view.findViewById(R.id.buttonSubmit);

        // Handle Cancel button click
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(); // Close the dialog
            }
        });

        // Handle Submit button click
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve data from the EditText fields
                EditText obstacleIdEditText = view.findViewById(R.id.editObstacleId);
                EditText rowEditText = view.findViewById(R.id.editRow);
                EditText colEditText = view.findViewById(R.id.editCol);
                EditText directionEditText = view.findViewById(R.id.editDirection);

                String obstacleId = obstacleIdEditText.getText().toString();
                String row = rowEditText.getText().toString();
                String col = colEditText.getText().toString();
                String direction = directionEditText.getText().toString();

                // Pass the data to your listener (MapFragment or another component)
                if (getTargetFragment() instanceof ObstacleDialogListener) {
                    ObstacleDialogListener listener = (ObstacleDialogListener) getTargetFragment();
                    try {
                        listener.onObstacleDataSubmitted(obstacleId, row, col, direction);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

                dismiss(); // Close the dialog
            }
        });

        return view;
    }
}

