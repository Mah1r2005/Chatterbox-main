package com.chatterbox.lan.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketWrapper {
    private Socket socket;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;
    private final Object writeLock = new Object();

    public SocketWrapper(String s, int port) throws IOException { // used by the client
        this.socket = new Socket(s, port);
        // Create output stream first to prevent deadlock
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public SocketWrapper(Socket s) throws IOException { // used by the server
        this.socket = s;
        // Create output stream first to prevent deadlock
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public Object read() throws IOException, ClassNotFoundException {
        synchronized(ois) {
            try {
                return ois.readObject();
            } catch (java.io.StreamCorruptedException e) {
                ois.skip(ois.available());
                throw e;
            }
        }
    }

    public void write(Object o) throws IOException {
        synchronized(writeLock) {
            try {
                oos.writeObject(o);
                oos.flush(); // Always flush after write
                oos.reset(); // Reset handle table to prevent memory leaks
            } catch (IOException e) {
                closeConnection();
                throw e;
            }
        }
    }

    public void closeConnection() throws IOException {
        synchronized(writeLock) {
            try {
                if (ois != null) {
                    ois.close();
                }
            } finally {
                try {
                    if (oos != null) {
                        oos.close();
                    }
                } finally {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                }
            }
        }
    }

    public String getRemoteAddress() {
        return socket.getRemoteSocketAddress().toString();
    }





}