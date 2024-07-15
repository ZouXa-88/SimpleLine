package com.example.line;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class NameActivity extends AppCompatActivity {

    private EditText username;
    private TextView warning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);

        username = findViewById(R.id.editTextPersonName);
        warning = findViewById(R.id.empty_name_warning);

        warning.setVisibility(TextView.INVISIBLE);
    }

    @Override
    public void onBackPressed() {
        returnBack("");
    }

    public void submitButtonClick(View view){
        String name = username.getText().toString().trim();

        if(name.isEmpty()){
            warning.setText(getText(R.string.empty_name));
            warning.setVisibility(TextView.VISIBLE);
            return;
        }
        if(name.length() > 10){
            warning.setText(getText(R.string.over_length));
            warning.setVisibility(TextView.VISIBLE);
            return;
        }

        warning.setVisibility(TextView.INVISIBLE);
        returnBack(name);
    }

    private void returnBack(String value) {
        Intent data = new Intent();
        data.putExtra("username", value);
        setResult(RESULT_OK, data);
        finish();
    }
}