package com.example.line;

import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientCommunicator extends Communicator implements Runnable {

    private boolean hasConnected = false;
    private ClientDealer clientDealer;
    private Thread becomeServerDetect = new Thread(this);
    private final long SOCKET_TIMEOUT = 5000;


    private static class ClientDealer extends Dealer implements Runnable {

        private Socket socket;
        private String aimIpAddress;
        private boolean connectedFinish = false;
        private Thread connectThread;


        ClientDealer(){
            super();
        }

        public void start() {
            connectThread = new Thread(this);
            connectThread.start();
        }

        @Override
        public void run() {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(aimIpAddress, PORT_NUMBER));
            } catch (Exception e) {}
            connectedFinish = true;
        }

        public void setAimIpAddress(String aimIpAddress) {
            this.aimIpAddress = aimIpAddress;
        }

        public boolean isConnectedSuccessfully() {
            return socket.isConnected();
        }

        public boolean isConnectedFinish() {
            return connectedFinish;
        }

        public Socket getSocket() {
            return socket;
        }

        public void closeSocket() {
            try{
                socket.close();
            } catch(Exception e) {}
        }
    }

    ClientCommunicator(String username) {
        super(username);
    }

    @Override
    public void run() {
        while(!super.stoppedReceiving){
            if(super.changeServer){
                do{
                    connect(super.changeIp);
                }while(!clientDealer.isConnectedSuccessfully());
                super.changeServer = false;
            }
        }
    }

    public void connect(String ip) {
        clientDealer = new ClientDealer();
        if(clientDealer.isHasIp()) {
            clientDealer.setAimIpAddress(ip);
            clientDealer.start();

            long start = System.currentTimeMillis();
            while (!clientDealer.isConnectedFinish() &&
                    System.currentTimeMillis() - start < SOCKET_TIMEOUT) {
                try{
                    Thread.sleep(10);
                } catch(Exception e) {}
            }

            if (clientDealer.isConnectedSuccessfully()) {
                super.set(clientDealer.getSocket());
                hasConnected = true;
            }
            else {
                clientDealer.closeSocket();
            }
        }
    }

    public boolean isConnected() {
        return hasConnected;
    }

    @Override
    public void go() {
        becomeServerDetect.start();
        super.go();
    }
}
