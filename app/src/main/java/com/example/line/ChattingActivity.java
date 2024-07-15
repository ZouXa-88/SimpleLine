package com.example.line;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashMap;

public class ChattingActivity extends AppCompatActivity implements Runnable {

    private Communicator communicator;

    private final int NULL_STICKER = R.id.nullbutton;
    private int clickedSticker = NULL_STICKER;

    private ConstraintLayout chooseStickerLayout;
    private EditText messageEdit;

    private HashMap<String, Drawable> sticker = new HashMap<>();
    private HashMap<Integer, ConstraintLayout> stickerFrame = new HashMap<>();

    private Thread refreshThread = new Thread(this);

    private boolean shownId = false;
    private boolean shownStickers = false;
    private boolean scrollDown = false;
    private boolean leaveCheck = false;
    private long lastBackPressedTime = 0L;

    private final int IMAGE_SELECT_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting);

        chooseStickerLayout = findViewById(R.id.choose_sticker_layout);
        chooseStickerLayout.setVisibility(ConstraintLayout.INVISIBLE);
        messageEdit = findViewById(R.id.message_text_editor);
        putSticker();
        putStickerFrame();

        Communicator.setFileDir(getApplicationContext().getFilesDir().getPath());
        Communicator.setContext(getApplicationContext());

        boolean isServer = getIntent().getBooleanExtra("is server", false);
        if(toConnectAndIsSuccessfully(isServer)){
            refreshThread.setDaemon(true);
            refreshThread.start();
        }
    }

    @Override
    public void run() {
        while(!communicator.isOff()){
            while(communicator.hasMail()){
                Mail newMail = communicator.getMail();
                if(newMail.isMemberNotification())
                    newMemberNotification(newMail);
                else
                    newMessageBlock(newMail, true);
            }

            if(scrollDown){
                Handler handler = new Handler(getMainLooper());
                handler.post(() -> ((ScrollView)findViewById(R.id.message_presentation_scroll)).fullScroll(ScrollView.FOCUS_DOWN));
                scrollDown = false;

                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(100);
            }

            if(leaveCheck && System.currentTimeMillis() - lastBackPressedTime > 3000) {
                findViewById(R.id.leave_check_text).setVisibility(View.INVISIBLE);
                leaveCheck = false;
            }
        }
        communicator.stopConnecting();

        if(communicator.toBecomeServer()){
            toConnectAndIsSuccessfully(true);
            run();
        }
    }

    @Override
    public void onBackPressed() {
        if(shownId || shownStickers)
            closeSubpage();
        else {
            if(leaveCheck) {
                communicator.close();
                super.onBackPressed();
            }

            lastBackPressedTime = System.currentTimeMillis();
            findViewById(R.id.leave_check_text).setVisibility(View.VISIBLE);
            leaveCheck = true;
        }
    }

    public void onClickConnectErrorButton(View view) {
        super.onBackPressed();
    }

    public void onClickStickerChoose(View view) {
        if (!shownStickers) {
            if(shownId)
                onClickShowGroupId(findViewById(R.id.show_id_button));

            changeClickedSticker(NULL_STICKER);
            findViewById(R.id.send_sticker_button).setBackground(ContextCompat.getDrawable(this, R.drawable.sticker_choose_on_click_pattern));
            chooseStickerLayout.setVisibility(ConstraintLayout.VISIBLE);
            shownStickers = true;
        }
    }

    public void onClickChooseStickerBack(View view) {
        findViewById(R.id.send_sticker_button).setBackground(ContextCompat.getDrawable(this, R.drawable.sticker_choose_non_click_pattern));
        chooseStickerLayout.setVisibility(ConstraintLayout.INVISIBLE);
        shownStickers = false;
    }

    public void onClickShowGroupId(View view) {
        if(shownStickers)
            onClickChooseStickerBack(findViewById(R.id.send_sticker_button));

        ConstraintLayout showIdLayout = findViewById(R.id.show_id_layout);
        Button showIdButton = findViewById(R.id.show_id_button);
        TextView showIdNotation = findViewById(R.id.show_id_notation);
        ((TextView)findViewById(R.id.group_id)).setText(communicator.getGroupId());

        if(shownId){
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            showIdLayout.startAnimation(animation);

            showIdNotation.setVisibility(TextView.VISIBLE);
            showIdButton.setRotation(showIdButton.getRotation() + 180);
            showIdLayout.setVisibility(ConstraintLayout.INVISIBLE);
        }
        else{
            showIdNotation.setVisibility(TextView.INVISIBLE);
            showIdLayout.setVisibility(ConstraintLayout.VISIBLE);
            showIdLayout.bringToFront();
            showIdButton.setRotation(showIdButton.getRotation() + 180);

            Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            showIdLayout.startAnimation(animation);
        }

        shownId = !shownId;
    }

    public void onClickMessageSubmit(View view) {
        String message = messageEdit.getText().toString();

        if(!message.equals("")) {
            newMessageBlock(communicator.sendText(message), false);
            messageEdit.getText().clear();
        }
    }

    public void stickerButtonClickAnimation(View view) {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.button_click);
        view.startAnimation(animation);
        animation = AnimationUtils.loadAnimation(this, R.anim.button_release);
        view.startAnimation(animation);
    }

    public void onClickSticker(View view) {
        if(clickedSticker == view.getId()){
            stickerButtonClickAnimation(view);
            newMessageBlock(communicator.sendSticker(Integer.toString(view.getId())), false);
        }
        else{
            changeClickedSticker(view.getId());
        }
    }

    public void onClickRefreshId(View view) {
        ((TextView)findViewById(R.id.group_id)).setText(communicator.getGroupId());
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_full);
        view.startAnimation(animation);
    }

    public void onClickKeyBoard(View view) {
        closeSubpage();
    }

    public void onClickSendImage(View view) {
        closeSubpage();

        if (Build.VERSION.SDK_INT >= 23) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select an Image to Send"), IMAGE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case IMAGE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    newMessageBlock(communicator.sendImage(this, data.getData()), false);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void closeSubpage() {
        if(shownStickers)
            onClickChooseStickerBack(findViewById(R.id.send_sticker_button));

        if(shownId)
            onClickShowGroupId(findViewById(R.id.show_id_button));
    }

    private boolean toConnectAndIsSuccessfully(boolean isServer) {
        try {
            if (isServer) {
                ServerCommunicator serverCommunicator = new ServerCommunicator(getIntent().getStringExtra("username"));
                if (!serverCommunicator.isSetUpSuccessfully()) {
                    throw new Exception();
                }
                serverCommunicator.go(); //enable to send mails and accept others to join
                communicator = serverCommunicator;
            }
            else {
                ClientCommunicator clientCommunicator = new ClientCommunicator(getIntent().getStringExtra("username"));
                clientCommunicator.connect(getIntent().getStringExtra("connect id"));
                if (!clientCommunicator.isConnected()) {
                    throw new Exception();
                }
                clientCommunicator.go(); //enable to send and receive mails
                clientCommunicator.joinNotification();
                communicator = clientCommunicator;
            }
        }
        catch (Exception e) {
            findViewById(R.id.connect_layout).setVisibility(ConstraintLayout.VISIBLE);
            findViewById(R.id.chatting_layout).setVisibility(ConstraintLayout.INVISIBLE);
            return false;
        }

        findViewById(R.id.connect_layout).setVisibility(ConstraintLayout.INVISIBLE);
        findViewById(R.id.chatting_layout).setVisibility(ConstraintLayout.VISIBLE);
        return true;
    }

    private void changeClickedSticker(int changedSticker) {
        stickerFrame.get(clickedSticker).setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        clickedSticker = changedSticker;
        stickerFrame.get(clickedSticker).setBackgroundColor(ContextCompat.getColor(this, R.color.yellow));
    }

    private void putSticker() {
        sticker.put(Integer.toString(R.id.sticker_01), ContextCompat.getDrawable(this, R.drawable.sticker01));
        sticker.put(Integer.toString(R.id.sticker_02), ContextCompat.getDrawable(this, R.drawable.sticker02));
        sticker.put(Integer.toString(R.id.sticker_03), ContextCompat.getDrawable(this, R.drawable.sticker03));
        sticker.put(Integer.toString(R.id.sticker_04), ContextCompat.getDrawable(this, R.drawable.sticker04));
        sticker.put(Integer.toString(R.id.sticker_05), ContextCompat.getDrawable(this, R.drawable.sticker05));
        sticker.put(Integer.toString(R.id.sticker_06), ContextCompat.getDrawable(this, R.drawable.sticker06));
        sticker.put(Integer.toString(R.id.sticker_07), ContextCompat.getDrawable(this, R.drawable.sticker07));
        sticker.put(Integer.toString(R.id.sticker_08), ContextCompat.getDrawable(this, R.drawable.sticker08));
        sticker.put(Integer.toString(R.id.sticker_09), ContextCompat.getDrawable(this, R.drawable.sticker09));
        sticker.put(Integer.toString(R.id.sticker_10), ContextCompat.getDrawable(this, R.drawable.sticker10));
        sticker.put(Integer.toString(R.id.sticker_11), ContextCompat.getDrawable(this, R.drawable.sticker11));
        sticker.put(Integer.toString(R.id.sticker_12), ContextCompat.getDrawable(this, R.drawable.sticker12));
        sticker.put(Integer.toString(R.id.sticker_13), ContextCompat.getDrawable(this, R.drawable.sticker13));
        sticker.put(Integer.toString(R.id.sticker_14), ContextCompat.getDrawable(this, R.drawable.sticker14));
        sticker.put(Integer.toString(R.id.sticker_15), ContextCompat.getDrawable(this, R.drawable.sticker15));
        sticker.put(Integer.toString(R.id.sticker_16), ContextCompat.getDrawable(this, R.drawable.sticker16));
        sticker.put(Integer.toString(R.id.sticker_17), ContextCompat.getDrawable(this, R.drawable.sticker17));
        sticker.put(Integer.toString(R.id.sticker_18), ContextCompat.getDrawable(this, R.drawable.sticker18));
        sticker.put(Integer.toString(R.id.sticker_19), ContextCompat.getDrawable(this, R.drawable.sticker19));
        sticker.put(Integer.toString(R.id.sticker_20), ContextCompat.getDrawable(this, R.drawable.sticker20));
        sticker.put(Integer.toString(R.id.sticker_21), ContextCompat.getDrawable(this, R.drawable.sticker21));
        sticker.put(Integer.toString(R.id.sticker_22), ContextCompat.getDrawable(this, R.drawable.sticker22));
        sticker.put(Integer.toString(R.id.sticker_23), ContextCompat.getDrawable(this, R.drawable.sticker23));
        sticker.put(Integer.toString(R.id.sticker_24), ContextCompat.getDrawable(this, R.drawable.sticker24));
        sticker.put(Integer.toString(R.id.sticker_25), ContextCompat.getDrawable(this, R.drawable.sticker25));
        sticker.put(Integer.toString(R.id.sticker_26), ContextCompat.getDrawable(this, R.drawable.sticker26));
        sticker.put(Integer.toString(R.id.sticker_27), ContextCompat.getDrawable(this, R.drawable.sticker27));
        sticker.put(Integer.toString(R.id.sticker_28), ContextCompat.getDrawable(this, R.drawable.sticker28));
        sticker.put(Integer.toString(R.id.sticker_29), ContextCompat.getDrawable(this, R.drawable.sticker29));
        sticker.put(Integer.toString(R.id.sticker_30), ContextCompat.getDrawable(this, R.drawable.sticker30));
        sticker.put(Integer.toString(R.id.sticker_31), ContextCompat.getDrawable(this, R.drawable.sticker31));
        sticker.put(Integer.toString(R.id.sticker_32), ContextCompat.getDrawable(this, R.drawable.sticker32));
        sticker.put(Integer.toString(R.id.sticker_33), ContextCompat.getDrawable(this, R.drawable.sticker33));
        sticker.put(Integer.toString(R.id.sticker_34), ContextCompat.getDrawable(this, R.drawable.sticker34));
        sticker.put(Integer.toString(R.id.sticker_35), ContextCompat.getDrawable(this, R.drawable.sticker35));
        sticker.put(Integer.toString(R.id.sticker_36), ContextCompat.getDrawable(this, R.drawable.sticker36));
        sticker.put(Integer.toString(R.id.sticker_37), ContextCompat.getDrawable(this, R.drawable.sticker37));
        sticker.put(Integer.toString(R.id.sticker_38), ContextCompat.getDrawable(this, R.drawable.sticker38));
        sticker.put(Integer.toString(R.id.sticker_39), ContextCompat.getDrawable(this, R.drawable.sticker39));
        sticker.put(Integer.toString(R.id.sticker_40), ContextCompat.getDrawable(this, R.drawable.sticker40));
    }

    private void putStickerFrame() {
        stickerFrame.put(R.id.sticker_01, (ConstraintLayout)findViewById(R.id.stickerframe_01));
        stickerFrame.put(R.id.sticker_02, (ConstraintLayout)findViewById(R.id.stickerframe_02));
        stickerFrame.put(R.id.sticker_03, (ConstraintLayout)findViewById(R.id.stickerframe_03));
        stickerFrame.put(R.id.sticker_04, (ConstraintLayout)findViewById(R.id.stickerframe_04));
        stickerFrame.put(R.id.sticker_05, (ConstraintLayout)findViewById(R.id.stickerframe_05));
        stickerFrame.put(R.id.sticker_06, (ConstraintLayout)findViewById(R.id.stickerframe_06));
        stickerFrame.put(R.id.sticker_07, (ConstraintLayout)findViewById(R.id.stickerframe_07));
        stickerFrame.put(R.id.sticker_08, (ConstraintLayout)findViewById(R.id.stickerframe_08));
        stickerFrame.put(R.id.sticker_09, (ConstraintLayout)findViewById(R.id.stickerframe_09));
        stickerFrame.put(R.id.sticker_10, (ConstraintLayout)findViewById(R.id.stickerframe_10));
        stickerFrame.put(R.id.sticker_11, (ConstraintLayout)findViewById(R.id.stickerframe_11));
        stickerFrame.put(R.id.sticker_12, (ConstraintLayout)findViewById(R.id.stickerframe_12));
        stickerFrame.put(R.id.sticker_13, (ConstraintLayout)findViewById(R.id.stickerframe_13));
        stickerFrame.put(R.id.sticker_14, (ConstraintLayout)findViewById(R.id.stickerframe_14));
        stickerFrame.put(R.id.sticker_15, (ConstraintLayout)findViewById(R.id.stickerframe_15));
        stickerFrame.put(R.id.sticker_16, (ConstraintLayout)findViewById(R.id.stickerframe_16));
        stickerFrame.put(R.id.sticker_17, (ConstraintLayout)findViewById(R.id.stickerframe_17));
        stickerFrame.put(R.id.sticker_18, (ConstraintLayout)findViewById(R.id.stickerframe_18));
        stickerFrame.put(R.id.sticker_19, (ConstraintLayout)findViewById(R.id.stickerframe_19));
        stickerFrame.put(R.id.sticker_20, (ConstraintLayout)findViewById(R.id.stickerframe_20));
        stickerFrame.put(R.id.sticker_21, (ConstraintLayout)findViewById(R.id.stickerframe_21));
        stickerFrame.put(R.id.sticker_22, (ConstraintLayout)findViewById(R.id.stickerframe_22));
        stickerFrame.put(R.id.sticker_23, (ConstraintLayout)findViewById(R.id.stickerframe_23));
        stickerFrame.put(R.id.sticker_24, (ConstraintLayout)findViewById(R.id.stickerframe_24));
        stickerFrame.put(R.id.sticker_25, (ConstraintLayout)findViewById(R.id.stickerframe_25));
        stickerFrame.put(R.id.sticker_26, (ConstraintLayout)findViewById(R.id.stickerframe_26));
        stickerFrame.put(R.id.sticker_27, (ConstraintLayout)findViewById(R.id.stickerframe_27));
        stickerFrame.put(R.id.sticker_28, (ConstraintLayout)findViewById(R.id.stickerframe_28));
        stickerFrame.put(R.id.sticker_29, (ConstraintLayout)findViewById(R.id.stickerframe_29));
        stickerFrame.put(R.id.sticker_30, (ConstraintLayout)findViewById(R.id.stickerframe_30));
        stickerFrame.put(R.id.sticker_31, (ConstraintLayout)findViewById(R.id.stickerframe_31));
        stickerFrame.put(R.id.sticker_32, (ConstraintLayout)findViewById(R.id.stickerframe_32));
        stickerFrame.put(R.id.sticker_33, (ConstraintLayout)findViewById(R.id.stickerframe_33));
        stickerFrame.put(R.id.sticker_34, (ConstraintLayout)findViewById(R.id.stickerframe_34));
        stickerFrame.put(R.id.sticker_35, (ConstraintLayout)findViewById(R.id.stickerframe_35));
        stickerFrame.put(R.id.sticker_36, (ConstraintLayout)findViewById(R.id.stickerframe_36));
        stickerFrame.put(R.id.sticker_37, (ConstraintLayout)findViewById(R.id.stickerframe_37));
        stickerFrame.put(R.id.sticker_38, (ConstraintLayout)findViewById(R.id.stickerframe_38));
        stickerFrame.put(R.id.sticker_39, (ConstraintLayout)findViewById(R.id.stickerframe_39));
        stickerFrame.put(R.id.sticker_40, (ConstraintLayout)findViewById(R.id.stickerframe_40));
        stickerFrame.put(R.id.nullbutton, (ConstraintLayout)findViewById(R.id.nullbuttonframe));
    }

    private void newMemberNotification(Mail mail) {
        final Context context = getApplicationContext();

        Handler handler = new Handler(getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                ConstraintLayout newNotification = new ConstraintLayout(context);
                newNotification.setId(View.generateViewId());

                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 50, 0, 0);
                newNotification.setLayoutParams(params);

                //set property
                TextView text = new TextView(context);
                text.setId(View.generateViewId());

                String textValue = mail.getSender();
                if(mail.getGenre().equals(Mail.JOIN))
                    textValue += getString(R.string.join);
                else
                    textValue += getString(R.string.leave);

                text.setText(textValue);
                text.setBackground(ContextCompat.getDrawable(context, R.drawable.member_notification_frame));
                text.setTextColor(ContextCompat.getColor(context, R.color.white));
                text.setPadding(10, 5, 10, 5);

                newNotification.addView(text);
                ((LinearLayout) findViewById(R.id.message_presentation_layout)).addView(newNotification);

                //set constraint
                ConstraintSet set = new ConstraintSet();
                set.clone(newNotification);

                set.connect(text.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                set.connect(text.getId(), ConstraintSet.END,   ConstraintSet.PARENT_ID, ConstraintSet.END);
                set.setHorizontalBias(text.getId(), 0.5f);

                //done
                set.applyTo(newNotification);
                scrollDown = true;
            }
        });
    }

    private void newMessageBlock(Mail mail, boolean fromOther) {
        final Context context = getApplicationContext();

        Handler handler = new Handler(getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                ConstraintLayout newBlock = new ConstraintLayout(context);
                newBlock.setId(View.generateViewId());

                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 50, 0, 0);
                newBlock.setLayoutParams(params);

                //set property

                TextView sender = new TextView(context);
                sender.setId(View.generateViewId());
                sender.setText(mail.getSender());
                sender.setTextSize(18f);
                sender.setPadding(5, 5, 5, 5);
                if(fromOther)
                    sender.setBackground(ContextCompat.getDrawable(context, R.drawable.nameplate2));
                else
                    sender.setBackground(ContextCompat.getDrawable(context, R.drawable.nameplate1));

                View message;
                if(mail.isText()){
                    TextView entity = new TextView(context);
                    entity.setId(View.generateViewId());

                    entity.setText(mail.getData());
                    entity.setTextColor(ContextCompat.getColor(context, R.color.black));
                    entity.setTextSize(25f);
                    entity.setSingleLine(false);
                    entity.setMaxWidth(700);
                    entity.setGravity(TextView.TEXT_ALIGNMENT_CENTER);
                    entity.setPadding(30, 30, 10, 30);

                    if(fromOther)
                        entity.setBackground(ContextCompat.getDrawable(context, R.drawable.chat_frame2));
                    else
                        entity.setBackground(ContextCompat.getDrawable(context, R.drawable.chat_frame1));

                    message = entity;
                }
                else if(mail.isImage()){
                    ImageView entity = new ImageView(context);
                    entity.setId(View.generateViewId());
                    entity.setImageURI(Uri.parse(mail.getData()));

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(500, 500);
                    entity.setLayoutParams(layoutParams);
                    entity.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    message = entity;
                }
                else { //sticker
                    TextView entity = new TextView(context);
                    entity.setId(View.generateViewId());

                    entity.setBackground(sticker.get(mail.getData()));
                    entity.setWidth(400);
                    entity.setHeight(400);

                    message = entity;
                }

                TextView time = new TextView(context);
                time.setId(View.generateViewId());
                time.setText(mail.getTime().substring(0, 5));
                time.setTextColor(ContextCompat.getColor(context, R.color.black));

                newBlock.addView(sender);
                newBlock.addView(message);
                newBlock.addView(time);
                ((LinearLayout) findViewById(R.id.message_presentation_layout)).addView(newBlock);

                //set constraint
                ConstraintSet set = new ConstraintSet();
                set.clone(newBlock);

                set.connect(sender.getId(),  ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                set.connect(message.getId(), ConstraintSet.TOP,    sender.getId(),          ConstraintSet.BOTTOM);
                set.connect(time.getId(),    ConstraintSet.BOTTOM, message.getId(),         ConstraintSet.BOTTOM);

                if(fromOther){
                    set.connect(sender.getId(),  ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                    set.connect(message.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                    set.connect(time.getId(),    ConstraintSet.START, message.getId(),         ConstraintSet.END);
                    set.setMargin(sender.getId(),  ConstraintSet.START, 50);
                    set.setMargin(message.getId(), ConstraintSet.START, 50);
                    set.setMargin(time.getId(),    ConstraintSet.START, 50);
                }
                else{
                    set.connect(sender.getId(),  ConstraintSet.END,   ConstraintSet.PARENT_ID, ConstraintSet.END);
                    set.connect(message.getId(), ConstraintSet.END,   ConstraintSet.PARENT_ID, ConstraintSet.END);
                    set.connect(time.getId(),    ConstraintSet.END,   message.getId(),         ConstraintSet.START);
                    set.setMargin(sender.getId(),  ConstraintSet.END, 50);
                    set.setMargin(message.getId(), ConstraintSet.END, 50);
                    set.setMargin(time.getId(),    ConstraintSet.END, 50);
                }

                //done
                set.applyTo(newBlock);
                scrollDown = true;
            }
        });
    }
}