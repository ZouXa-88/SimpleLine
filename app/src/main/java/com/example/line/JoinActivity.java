package com.example.line;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class JoinActivity extends AppCompatActivity implements Runnable {

    private String username;
    private EditText[] idBLock = new EditText[4];
    private Thread activateButtonDetect = new Thread(this);
    private boolean stopDetecting = false;
    private static ArrayList<String> id_history = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        idBLock[0] = findViewById(R.id.id_1);
        idBLock[1] = findViewById(R.id.id_2);
        idBLock[2] = findViewById(R.id.id_3);
        idBLock[3] = findViewById(R.id.id_4);

        username = getIntent().getStringExtra("username");
        activateButtonDetect.start();

        for(String currentId : id_history){
            Button button = new Button(this);
            button.setId(View.generateViewId());

            button.setText(currentId);
            button.setTextSize(30f);
            button.setTextColor(ContextCompat.getColor(this, R.color.white));
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setInput(((Button)view).getText().toString());
                }
            });

            ((LinearLayout)findViewById(R.id.id_history_linear_layout)).addView(button);
        }
    }

    @Override
    public void onBackPressed() {
        if(findViewById(R.id.id_history_layout).getVisibility() == View.VISIBLE)
            findViewById(R.id.id_history_layout).setVisibility(View.INVISIBLE);
        else {
            stopDetecting = true;
            super.onBackPressed();
        }
    }

    @Override
    public void run() {
        Button idSummitButton = findViewById(R.id.id_summit_button);
        Context context = this;

        while(!stopDetecting){
            boolean toActivate = true;

            for(EditText currentIdBlock : idBLock){
                if(currentIdBlock.getText().toString().isEmpty()){
                    toActivate = false;
                    break;
                }
            }

            Handler handler = new Handler(getMainLooper());
            if(toActivate){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        idSummitButton.setBackground(ContextCompat.getDrawable(context, R.drawable.button_style_peachpuff));
                        idSummitButton.setClickable(true);
                    }
                });
            }
            else{
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        idSummitButton.setBackground(ContextCompat.getDrawable(context, R.drawable.button_style_off));
                        idSummitButton.setClickable(false);
                    }
                });
            }

            try{ Thread.sleep(100); } catch(Exception e) {}
        }
    }

    public void idSummitButtonClick(View view) {
        Intent chattingActivity = new Intent(this, ChattingActivity.class);
        String idInput = getIdInput();

        chattingActivity.putExtra("username", username);
        chattingActivity.putExtra("is server", false);
        chattingActivity.putExtra("connect id", idInput);

        if(!id_history.contains(idInput))
            id_history.add(idInput);

        findViewById(R.id.connecting_text).setVisibility(TextView.VISIBLE);
        stopDetecting = true;
        startActivity(chattingActivity);
        finish();
    }

    public void idHistoryButtonClick(View view) {
        findViewById(R.id.id_history_layout).setVisibility(View.VISIBLE);
    }

    public void backgroundClick(View view) {
        if(findViewById(R.id.id_history_layout).getVisibility() == View.VISIBLE)
            findViewById(R.id.id_history_layout).setVisibility(View.INVISIBLE);
    }

    private String getIdInput() {
        String input_id = "";

        for (EditText currentIdBlock : idBLock)
            input_id += currentIdBlock.getText() + ".";

        return input_id.substring(0, input_id.length() - 1);
    }

    private void setInput(String id) {
        String[] block = id.split("\\.");

        for(int i = 0; i < 4; i++)
            idBLock[i].setText(block[i]);
    }
}