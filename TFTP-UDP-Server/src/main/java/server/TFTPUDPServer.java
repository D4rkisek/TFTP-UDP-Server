package server;

import java.io.*;
import java.net.*;

public class TFTPUDPServer {
    private static final int TFTP_PORT = 9222; // Default TFTP port

    public static void main(String[] args) throws IOException {
        int portNumber = TFTP_PORT; //setting the port number to 69

        if (args.length != 1) {
        //    System.out.println("Using the default port " + TFTP_PORT);
        } else {
            portNumber = Integer.parseInt(args[0]);
        }

        try (DatagramSocket serverSocket = new DatagramSocket(portNumber)) {
            byte[] buf = new byte[512];

            System.out.println("Server started on port " + portNumber + "...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received packet 0 from " + packet.getAddress() + ", Port Num:" + packet.getPort()); // Receiver receives pkt0
                new TFTPUDPServerThread(serverSocket, packet).start(); // Starting up a new thread for each client, allowing a multi-client connection to the server
            }
        }
    }
}