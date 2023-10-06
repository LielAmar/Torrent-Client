package project.packet.packets;

import project.packet.Packet;
import project.packet.PacketType;

import java.nio.ByteBuffer;

public class HandshakePacket extends Packet {

    protected static final int HANDSHAKE_LENGTH = 32;
    protected static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
    protected static final int HANDSHAKE_ZERO_BITS_LENGTH = 10;
    protected static final int PEER_ID_LENGTH = 4;

    private int peerId;

    public HandshakePacket() {
        super(PacketType.HANDSHAKE);
    }

    public void setPeerId(int peerId) {
        this.peerId = peerId;
    }

    public int getPeerId() {
        return this.peerId;
    }

    public byte[] build() {
        byte[] payload = new byte[HANDSHAKE_LENGTH];

        int i;

        // Set the handshake header
        for (i = 0; i < HANDSHAKE_HEADER.length(); i++) {
            payload[i] = (byte) HANDSHAKE_HEADER.charAt(i);
        }

        // Set the handshake zero bits (10 zero bytes)
        i += HANDSHAKE_ZERO_BITS_LENGTH;

        // Set the process' peer id
        System.arraycopy(ByteBuffer.allocate(PEER_ID_LENGTH).putInt(this.peerId).array(), 0,
                payload, i, PEER_ID_LENGTH);

        return payload;
    }

    public boolean parse(byte[] payload) {
        if(payload.length != HANDSHAKE_LENGTH) {
            return false;
        }

        String header = new String(payload, 0, HANDSHAKE_HEADER.length());

        if(!header.equals(HANDSHAKE_HEADER)) {
            return false;
        }

        for(int i = HANDSHAKE_HEADER.length(); i < HANDSHAKE_HEADER.length() + HANDSHAKE_ZERO_BITS_LENGTH; i++) {
            if(payload[i] != 0) {
                return false;
            }
        }

        ByteBuffer peerIdBuffer = ByteBuffer.allocate(4).put(payload,  HANDSHAKE_HEADER.length() + HANDSHAKE_ZERO_BITS_LENGTH, PEER_ID_LENGTH);
        peerIdBuffer.rewind();
        this.peerId = peerIdBuffer.getInt();
        return true;
    }
}
