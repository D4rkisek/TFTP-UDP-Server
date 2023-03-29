package server;

import java.io.*;
import java.net.*;

class TFTPUDPServerThread extends Thread {
    private final DatagramSocket socket;
    private final DatagramPacket packet;

    public TFTPUDPServerThread(DatagramSocket socket, DatagramPacket packet) {
        this.socket = socket;
        this.packet = packet;
    }

    @Override
    public void run() {
        // Create a new DatagramSocket to handle incoming packets
        try (DatagramSocket threadSocket = new DatagramSocket()) {
            // Extraction of client's IP address and port number that sent the packet
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();
            // Read the opcode from the incoming client's packet
            short opcode = TFTPUtil.getOpcode(packet);
            // (Read request)
            if (opcode == TFTPUtil.OP_RRQ) {
                String fileName = TFTPUtil.getFileName(packet);
                System.out.println("Received Read request for file: " + fileName + " from " + clientAddress + ":" + clientPort);
                // Using TFTPUtil's sendFile method
                TFTPUtil.sendFile(threadSocket, clientAddress, clientPort, fileName);
            // (Write request)
            } else if (opcode == TFTPUtil.OP_WRQ) {
                String fileName = TFTPUtil.getFileName(packet);
                System.out.println("Received Write request for file: " + fileName + " from " + clientAddress + ":" + clientPort);
                // Using TFTPUtil's receiveFile method
                TFTPUtil.receiveFile(threadSocket, clientAddress, clientPort, fileName);
            }
        } catch (IOException e) { // Exception handler
            System.out.println("Error handling request: " + e.getMessage());
        }
    }
}