package com.example.line;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Mail {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.TAIWAN);

    //genre
    public static final String TEXT = "1";
    public static final String STICKER = "2";
    public static final String IMAGE = "3";
    public static final String JOIN = "4";
    public static final String LEAVE = "5";
    public static final String BECOME_SERVER = "6";
    public static final String CHANGE_SERVER = "7";

    private static final String SEP = "\t";

    private String sender;
    private String senderId;
    private String genre;
    private String data;
    private String time;

    Mail() {}

    Mail(Mail mail) {
        this.sender = mail.sender;
        this.senderId = mail.senderId;
        this.genre = mail.genre;
        this.data = mail.data;
        this.time = mail.time;
    }

    public static String encode(String sender, String senderId, String genre, String data) {
        return sender + SEP + senderId + SEP + genre + SEP + data + SEP + getCurrentTime();
    }

    public static String encode(Mail mail) {
        return mail.sender + SEP + mail.senderId + SEP + mail.genre + SEP + mail.data + SEP + mail.time;
    }

    public static Mail decode(String encoded) {
        String[] decoded = encoded.split(SEP, 5);

        Mail mail = new Mail();
        mail.sender = decoded[0];
        mail.senderId = decoded[1];
        mail.genre = decoded[2];
        mail.data = decoded[3];
        mail.time = decoded[4];

        return mail;
    }

    public static Mail pack(String sender, String senderId, String genre, String data) {
        return decode(encode(sender, senderId, genre, data));
    }

    public String getSender() {
        return sender;
    }

    public String getSenderId() {
        return senderId;
    }

    public boolean isText() {
        return genre.equals(TEXT);
    }

    public boolean isImage() {
        return genre.equals(IMAGE);
    }

    public boolean isMemberNotification() {
        return genre.equals(JOIN) || genre.equals(LEAVE);
    }

    public String getData() {
        return data;
    }

    public String getTime() {
        return time;
    }

    public String getGenre() {
        return genre;
    }

    private static String getCurrentTime() {
        return dateFormat.format(new Date());
    }
}