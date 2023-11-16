package project.packet.packets;

import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;

public class UnknownPacket extends Packet {

    public UnknownPacket() {
        super(PacketType.UNKNOWN);
    }

    @Override
    public byte[] build() throws NetworkException {
        return new byte[0];
    }

    @Override
    public boolean parse(byte[] payload) {
        return false;
    }
}
