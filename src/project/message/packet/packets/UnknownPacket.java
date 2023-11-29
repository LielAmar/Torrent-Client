package project.message.packet.packets;

import project.exceptions.NetworkException;
import project.message.packet.Packet;
import project.message.packet.PacketType;

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
