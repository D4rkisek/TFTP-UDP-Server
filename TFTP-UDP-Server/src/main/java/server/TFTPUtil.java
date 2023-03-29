package server;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class TFTPUtil {
    // Constants for TFTP opcodes
    public static final short OP_RRQ = 1; // Sending a file
    public static final short OP_WRQ = 2; // Retrieving a file
    public static final short OP_DATA = 3; // Data packet
    public static final short OP_ACK = 4; // Acknowledgements

    public static DatagramPacket createDataPacket(InetAddress serverAddress, int port, short blockNumber, byte[] data, int dataLength) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        try {
            dataOutputStream.writeShort(OP_DATA);
            dataOutputStream.writeShort(blockNumber);
            dataOutputStream.write(data, 0, dataLength);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] dataPacketData = byteArrayOutputStream.toByteArray();
        return new DatagramPacket(dataPacketData, dataPacketData.length, serverAddress, port);
    }

    public static DatagramPacket createAckPacket(InetAddress serverAddress, int port, short blockNumber) {
        byte[] ackData = new byte[4];
        ByteBuffer.wrap(ackData).putShort(OP_ACK).putShort(blockNumber);
        return new DatagramPacket(ackData, ackData.length, serverAddress, port);
    }


    public static void sendFile(DatagramSocket socket, InetAddress serverAddress, int port, String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + fileName);
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            short blockNumber = 0;
            byte[] buffer = new byte[512];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                DatagramPacket dataPacket = createDataPacket(serverAddress, port, blockNumber, buffer, bytesRead);
                socket.send(dataPacket);
                System.out.println("Sent data packet with block number: " + blockNumber);

                DatagramPacket ackPacket = new DatagramPacket(new byte[4], 4);
                socket.receive(ackPacket);
                short ackBlockNumber = ByteBuffer.wrap(ackPacket.getData()).getShort(2);
                System.out.println("Received ACK packet with block number: " + ackBlockNumber);

                if (ackBlockNumber != blockNumber) {
                    throw new IOException("Invalid ACK received");
                }

                blockNumber++;
            }
        }
    }
    public static void receiveFile(DatagramSocket socket, InetAddress serverAddress, int port, String fileName) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
            short expectedBlockNumber = 0;

            // Send the initial ACK packet with block number 0
            DatagramPacket initialAckPacket = createAckPacket(serverAddress, port, (short) 0);
            socket.send(initialAckPacket);
            System.out.println("Sent initial ACK packet with block number: 0");

            while (true) {
                DatagramPacket dataPacket = new DatagramPacket(new byte[516], 516);
                socket.receive(dataPacket);
                System.out.println("Received data packet with block number: " + expectedBlockNumber);

                short opcode = ByteBuffer.wrap(dataPacket.getData()).getShort();
                short blockNumber = ByteBuffer.wrap(dataPacket.getData()).getShort(2);

                if (opcode != OP_DATA || blockNumber != expectedBlockNumber) {
                    throw new IOException("Invalid data packet received");
                }

                int dataLength = dataPacket.getLength() - 4;
                fileOutputStream.write(dataPacket.getData(), 4, dataLength);

                DatagramPacket ackPacket = createAckPacket(serverAddress, port, blockNumber);
                socket.send(ackPacket);
                System.out.println("Sent ACK packet with block number: " + blockNumber);

                expectedBlockNumber++;

                if (dataLength < 512) {
                    break;
                }
            }
        }
    }

    public static short getOpcode(DatagramPacket packet) {
        byte[] data = packet.getData();
        return ByteBuffer.wrap(data).getShort();
    }

    public static String getFileName(DatagramPacket packet) throws IOException {
        byte[] data = packet.getData();
        int length = packet.getLength();
        StringBuilder fileName = new StringBuilder();
        for (int i = 2; i < length; i++) {
            char c = (char) data[i];
            if (c == 0) {
                break;
            }
            fileName.append(c);
        }
        return fileName.toString();
    }
}