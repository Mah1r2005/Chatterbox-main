package com.chatterbox.lan.network;

import com.chatterbox.lan.models.Event;

public class ClientThread implements Runnable {

  private final SocketWrapper socketWrapper;
  private final Client client;

    public ClientThread(SocketWrapper socketWrapper, Client client) {

        this.socketWrapper = socketWrapper;
        this.client = client;
    }

    public void run() {
        try {
            while (true) {
                Object obj = socketWrapper.read();

                if (obj instanceof Event) {
                    Event event = (Event) obj;

                    // forward to client handler
                    client.handleIncomingEvent(event);
                }
            }
        } catch (Exception e) {
            System.out.println("[CLIENT] Read thread stopped");
        }
    }
}