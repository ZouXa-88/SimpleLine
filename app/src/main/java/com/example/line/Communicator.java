package com.example.line;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.TreeSet;

public class Communicator {

    public String username;
    protected String personalId;
    protected String connectId;

    private Socket socket;
    private DataInputStream stringInput;
    private DataOutputStream stringOutput;

    private HandlerThread receiveThread;
    protected MailBox mailBox = new MailBox();

    private HandlerThread sendThread;
    private Handler sendHandler;

    protected boolean stoppedReceiving = false;
    private boolean gotLeaveNotification = false;
    protected boolean changeServer = false;
    protected String changeIp;
    private boolean becomeServer = false;

    private int fileNameCount = 1;
    private static String fileDir = "";
    protected static Context context;


    protected static class MailBox {

        TreeSet<Mail> mailQueue = new TreeSet<>((Mail mail1, Mail mail2) -> mail1.getTime().compareTo(mail2.getTime()));

        public Mail nextMail() {
            Mail temp = new Mail(mailQueue.first());
            mailQueue.remove(mailQueue.first());
            return temp;
        }

        public TreeSet<Mail> getAllMails() {
            TreeSet<Mail> temp = new TreeSet<>(mailQueue);
            mailQueue.clear();
            return temp;
        }

        public boolean hasNext() {
            return mailQueue.size() >= 1;
        }

        public void add(Mail mail) {
            mailQueue.add(mail);
        }

        public void add(TreeSet<Mail> mails) {
            for(Mail mail : mails)
                add(mail);
        }
    }

    protected static class Dealer {

        public static final int PORT_NUMBER = 9105;
        protected String ipAddress;
        private boolean hasIp;

        Dealer() {
            Thread setUpThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

                        while(networkInterfaces.hasMoreElements()){
                            Enumeration<InetAddress> inetAddresses = networkInterfaces.nextElement().getInetAddresses();

                            while(inetAddresses.hasMoreElements()){
                                InetAddress inetAddress = inetAddresses.nextElement();

                                if(!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
                                    ipAddress =  inetAddress.getHostAddress();
                            }
                        }
                    } catch (Exception e) {
                        ipAddress = null;
                    }
                }
            });
            setUpThread.start();

            try{ setUpThread.join(); } catch(Exception e) {}
            hasIp = (ipAddress != null);
        }

        public boolean isHasIp() {
            return hasIp;
        }

        public String getIpAddress() {
            return ipAddress;
        }
    }

    Communicator(String username) {
        this.username = username;
    }

    public static void setFileDir(String fileDir) {
        Communicator.fileDir = fileDir;
    }

    public static void setContext(Context context) {
        Communicator.context = context;
    }

    protected void set(Socket socket) {
        if(this.socket != null){
            //close the origin connection
            closeStream();
        }

        try {
            stringInput = new DataInputStream(socket.getInputStream());
            stringOutput = new DataOutputStream(socket.getOutputStream());
            personalId = filterAddress(socket.getLocalSocketAddress().toString());
            connectId = filterAddress(socket.getRemoteSocketAddress().toString());
        }catch(Exception e) {}

        this.socket = socket;
    }

    private String filterAddress(String str) {
        return str.substring(1).split(":")[0];
    }

    public void go() {
        sendThread = new HandlerThread("Send Thread");
        sendThread.start();
        sendHandler = new Handler(sendThread.getLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                try{
                    stringOutput.writeUTF((String)msg.obj);
                } catch(Exception e) {}
            }
        };

        receiveThread = new HandlerThread("Receive Thread");
        receiveThread.start();
        Handler receiveHandler = new Handler(receiveThread.getLooper());
        receiveHandler.post(() -> {
            while(!stoppedReceiving){
                if(!changeServer){
                    try {
                        Mail newMail = Mail.decode(stringInput.readUTF());

                        switch (newMail.getGenre()) {
                            case Mail.CHANGE_SERVER:
                                changeIp = newMail.getData();
                                changeServer = true;
                                closeStream();
                                break;
                            case Mail.BECOME_SERVER:
                                becomeServer = true;
                                stoppedReceiving = true;
                                break;
                            case Mail.IMAGE:
                                Mail convertedMail = receiveImage(newMail.getData());
                                mailBox.add(convertedMail);
                                break;
                            case Mail.LEAVE:
                                gotLeaveNotification = true;
                            default:
                                mailBox.add(newMail);
                        }
                    } catch (Exception e) {}
                }
            }
        });
    }

    public Mail sendText(String text) {
        return send(Mail.TEXT, text);
    }

    public Mail sendSticker(String sticker) {
        return send(Mail.STICKER, sticker);
    }

    public Mail sendImage(Context context, Uri uri) {
        send(Mail.IMAGE, "");

        HandlerThread sendImageThread = new HandlerThread("send image thread");
        sendImageThread.start();
        Handler handler = new Handler(sendImageThread.getLooper());
        handler.post(() -> {
            try {
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                InputStream in = context.getContentResolver().openInputStream(uri);
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }

                out.flush();
                out.close();
                in.close();
            } catch (Exception e) {
            }
        });
        sendImageThread.quit();

        return Mail.pack(username, personalId, Mail.IMAGE, uri.toString());
    }

    public void sendMail(String mail) {
        Message message = sendHandler.obtainMessage();
        message.obj = mail;
        sendHandler.sendMessage(message);
    }

    public void joinNotification() {
        send(Mail.JOIN, "");
    }

    public void leaveNotification() {
        send(Mail.LEAVE, "");
    }

    public boolean hasMail() {
        return mailBox.hasNext();
    }

    public Mail getMail() {
        return mailBox.nextMail();
    }

    public TreeSet<Mail> getAllMails() {
        return mailBox.getAllMails();
    }

    public boolean isOff() {
        return stoppedReceiving && !mailBox.hasNext();
    }

    public String getPersonalId() {
        return personalId;
    }

    public String getConnectId() {
        return connectId;
    }

    public String getGroupId() {
        return connectId;
    }

    public boolean receiveLeaveNotification() {
        return gotLeaveNotification;
    }

    public boolean toBecomeServer() {
        return becomeServer;
    }

    public void close() {
        leaveNotification();
        stopConnecting();
    }

    public void closeStream() {
        try{
            stringInput.close();
            stringOutput.close();
            socket.close();
        } catch (Exception e) {}
    }

    public void sendChangingServerNotification(String newServerIp) {
        send(Mail.CHANGE_SERVER, newServerIp);
    }

    public void sendBecomingServerNotification() {
        send(Mail.BECOME_SERVER, "");
    }

    protected void stopConnecting() {
        stoppedReceiving = true;
        sendThread.quitSafely();
        receiveThread.quitSafely();
    }

    private Mail send(String genre, String data) {
        String mail = Mail.encode(username, personalId, genre, data);
        sendMail(mail);
        return Mail.decode(mail);
    }

    private Mail receiveImage(String fileName) {
        File file = new File(generateFileName());

        try {
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            FileOutputStream fileOut = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) > 0) {
                fileOut.write(buffer, 0, bytesRead);
            }

            in.close();
            fileOut.flush();
            fileOut.close();
        }catch(Exception e){
            return Mail.pack(username, personalId, Mail.TEXT, e.toString());
        }

        return Mail.pack(username, personalId, Mail.IMAGE, Uri.fromFile(file).toString());
    }

    private String generateFileName() {
        return fileDir +
                personalId.replace(".", "-") +
                "_" +
                (fileNameCount++) +
                ".jpg";
    }
}