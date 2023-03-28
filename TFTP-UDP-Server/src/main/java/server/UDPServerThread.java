package server;

import java.io.*;
import java.net.*;

class UDPServerThread extends Thread {
    private final DatagramSocket socket;
    private final DatagramPacket packet;

    public UDPServerThread(DatagramSocket socket, DatagramPacket packet) {
        this.socket = socket;
        this.packet = packet;
    }

    @Override
    public void run() {
        try (DatagramSocket threadSocket = new DatagramSocket()) {
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            short opcode = TFTPUtil.getOpcode(packet);

            if (opcode == TFTPUtil.OP_RRQ) {
                String fileName = TFTPUtil.getFileName(packet);
                System.out.println("Received Read request for file: " + fileName + " from " + clientAddress + ":" + clientPort);
                TFTPUtil.sendFile(threadSocket, clientAddress, clientPort, fileName);
            } else if (opcode == TFTPUtil.OP_WRQ) {
                String fileName = TFTPUtil.getFileName(packet);
                System.out.println("Received Write request for file: " + fileName + " from " + clientAddress + ":" + clientPort);
                TFTPUtil.receiveFile(threadSocket, clientAddress, clientPort, fileName);
            }
        } catch (IOException e) {
            System.out.println("Error handling request: " + e.getMessage());
            try {
                DatagramPacket errorPacket = TFTPUtil.createErrorPacket(packet.getAddress(), packet.getPort(), TFTPUtil.ERR_FILE_NOT_FOUND, "File not found");
                socket.send(errorPacket);
            } catch (IOException ex) {
                System.out.println("Error sending error packet: " + ex.getMessage());
            }
        }
    }
}