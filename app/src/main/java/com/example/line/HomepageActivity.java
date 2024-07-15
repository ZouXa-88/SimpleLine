package com.example.line;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

public class HomepageActivity extends AppCompatActivity {

    private String username;
    private boolean leaveCheck = false;
    private boolean initialized = false;
    private final int NAME_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        Intent nameActivity = new Intent(this, NameActivity.class);
        startActivityForResult(nameActivity, NAME_CODE);
    }

    @Override
    public void onBackPressed() {
        if(!leaveCheck){
            findViewById(R.id.leave_check_layout).setVisibility(View.VISIBLE);

            findViewById(R.id.create_button).setClickable(false);
            findViewById(R.id.join_button).setClickable(false);
            findViewById(R.id.setting_button).setClickable(false);

            leaveCheck = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case NAME_CODE:
                if (resultCode == RESULT_OK) {
                    username = data.getStringExtra("username");

                    if(!username.isEmpty()) {
                        ((TextView)findViewById(R.id.hello_message)).setText(getString(R.string.hello));
                        ((TextView)findViewById(R.id.hello_message)).append(username);
                        initialized = true;
                    }
                    else if(!initialized)
                        finish();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void createButtonClick(View view) {
        Intent chattingActivity = new Intent(this, ChattingActivity.class);
        chattingActivity.putExtra("username", username);
        chattingActivity.putExtra("is server", true);
        startActivity(chattingActivity);
    }

    public void joinButtonClick(View view) {
        Intent joinActivity = new Intent(this, JoinActivity.class);
        joinActivity.putExtra("username", username);
        startActivity(joinActivity);
    }

    public void settingButtonClick(View view) {
        Intent nameActivity = new Intent(this, NameActivity.class);
        startActivityForResult(nameActivity, NAME_CODE);
    }

    public void leaveYes(View view) {
        finish();
    }

    public void leaveNo(View view) {
        findViewById(R.id.leave_check_layout).setVisibility(View.INVISIBLE);

        findViewById(R.id.create_button).setClickable(true);
        findViewById(R.id.join_button).setClickable(true);
        findViewById(R.id.setting_button).setClickable(true);

        leaveCheck = false;
    }
}