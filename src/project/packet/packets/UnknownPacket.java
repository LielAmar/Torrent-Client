package project.packet.packets;

import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;

public class UnknownPacket extends Packet {
    public UnknownPacket()
    {
        super(PacketType.UNKNOWN);
    }
    public byte[] build() throws NetworkException
    {
        return new byte[0];
    }

    public static Packet PacketFromBytes(byte[] messageBytes)
    {
        return new UnknownPacket();
    }


    public boolean parse(byte[] payload)
    {
        return true;
    }
}
