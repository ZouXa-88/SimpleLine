package com.example.line;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TreeSet;

public class ServerCommunicator extends Communicator implements Runnable {

    private ArrayList<Communicator> parallelCommunicators = new ArrayList<>(4);

    private boolean setUpSuccessfully = false;
    private boolean left = false;
    private ServerDealer serverDealer;
    private Thread centralThread = new Thread(this);


    private static class ServerDealer extends Dealer implements Runnable {

        private ServerSocket serverSocket;
        private ArrayList<Communicator> newCommunicator = new ArrayList<>();
        private boolean close = false;
        private Thread acceptThread = new Thread(this);
        private String username;// for set up new communicator


        ServerDealer(String username) {
            super();
            this.username = username;

            try{
                serverSocket = new ServerSocket(PORT_NUMBER);
            } catch(Exception e) {}
        }

        public void start() {
            acceptThread.start();
        }

        @Override
        public void run() {
            while(!close){
                try {
                    Socket acceptedSocket = serverSocket.accept();

                    Communicator communicator = new Communicator(username);
                    communicator.set(acceptedSocket);
                    communicator.go();
                    newCommunicator.add(communicator);
                } catch(Exception e) {}
            }
        }

        public ArrayList<Communicator> getNewCommunicators() {
            ArrayList<Communicator> temp = new ArrayList<>(newCommunicator);
            newCommunicator.clear();
            return temp;
        }

        public void setClose() {
            close = true;
            try { serverSocket.close(); } catch(Exception e) {}
        }
    }

    public ServerCommunicator(String username) {
        super(username);

        serverDealer = new ServerDealer(username);
        if(serverDealer.isHasIp()){
            personalId = serverDealer.getIpAddress();
            setUpSuccessfully = true;
        }
    }

    public boolean isSetUpSuccessfully() {
        return setUpSuccessfully;
    }

    @Override
    public void run() {
        ArrayList<Communicator> removeList = new ArrayList<>();
        TreeSet<Mail> receiveBuf = new TreeSet<>((Mail mail1, Mail mail2) -> mail1.getTime().compareTo(mail2.getTime()));

        while(!left){
            for(Communicator communicator : parallelCommunicators){
                if(communicator.hasMail())
                    receiveBuf.addAll(communicator.getAllMails());
            }

            sendMails(receiveBuf);
            mailBox.add(receiveBuf);
            receiveBuf.clear();

            parallelCommunicators.addAll(serverDealer.getNewCommunicators());
            checkLeaveNotification();

            try{ Thread.sleep(50); } catch(Exception e) {}
        }
    }

    @Override
    public void go() {
        centralThread.setDaemon(true);
        centralThread.start();
        serverDealer.start();
    }

    @Override
    public Mail sendText(String text) {
        String sendMail = Mail.encode(username, personalId, Mail.TEXT, text);

        for(Communicator communicator : parallelCommunicators){
            communicator.sendMail(sendMail);
        }

        return Mail.decode(sendMail);
    }

    @Override
    public Mail sendSticker(String sticker) {
        String sendMail = Mail.encode(username, personalId, Mail.STICKER, sticker);

        for(Communicator communicator : parallelCommunicators){
            communicator.sendMail(sendMail);
        }

        return Mail.decode(sendMail);
    }

    @Override
    public Mail sendImage(Context context, Uri uri) {
        String sendMail = Mail.encode(username, personalId, Mail.IMAGE, uri.toString());

        for(Communicator communicator : parallelCommunicators){
            communicator.sendImage(context, uri);
        }

        return Mail.decode(sendMail);
    }

    @Override
    public void close() {
        serverDealer.setClose();
        left = true;

        if(parallelCommunicators.size() > 0){
            for(Communicator communicator : parallelCommunicators)
                communicator.leaveNotification();

            String newServerId = parallelCommunicators.get(0).connectId;
            parallelCommunicators.get(0).sendBecomingServerNotification();
            for(int i = 1; i < parallelCommunicators.size(); i++){
                parallelCommunicators.get(i).sendChangingServerNotification(newServerId);
            }

            for(Communicator communicator : parallelCommunicators)
                communicator.stopConnecting();
        }
    }

    @Override
    public String getGroupId() {
        return super.getPersonalId();
    }

    //help distribute
    private void sendMails(TreeSet<Mail> mails) {
        for(Mail mail : mails){
            for(Communicator communicator : parallelCommunicators){
                //to avoid resending to original sender
                if(mail.getSenderId().equals(communicator.getConnectId()))
                    continue;

                if(mail.isImage()){

                }
                else
                    communicator.sendMail(Mail.encode(mail));
            }
        }
    }

    private void checkLeaveNotification() {
        ArrayList<Communicator> removeList = new ArrayList<>();

        for(Communicator communicator : parallelCommunicators){
            if(communicator.receiveLeaveNotification()) {
                removeList.add(communicator);
                communicator.stopConnecting();
                communicator.closeStream();
            }
        }

        parallelCommunicators.removeAll(removeList);
    }
}
