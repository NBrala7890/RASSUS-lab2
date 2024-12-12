package hr.fer.tel.rassus.config;

import hr.fer.tel.rassus.message.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class PacketUtils {

    public static byte[] dataFromDatagramPacket(DatagramPacket packet) {
        return Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getLength());
    }

    public static DatagramPacket createSendPacket(Message m, InetAddress address, int port) throws IOException {
        byte[] sendBuf = Message.serialize(m);
        return new DatagramPacket(sendBuf, sendBuf.length, address, port);
    }

}
