package project.packet;

import project.exceptions.NetworkException;
import project.packet.packets.*;

public abstract class Packet {

    protected final PacketType type;

    protected Packet(PacketType type) {
        this.type = type;
    }


    public PacketType getType() {
        return this.type;
    }

    public String getTypeString() {
        return type.name();
    }


    /***
     * Build the byte array that will be sent over the network connection
     *
     * @return byte[] that can be sent over the connection, includes the length headers
     */
    public abstract byte[] build() throws NetworkException;

    /**
     * Parse the given payload and set all necessary fields accordingly
     *
     * @param payload   Payload to parse
     * @return          Whether successfully parsed the payload
     */
    public abstract boolean parse(byte[] payload);


    /**
     * Given the payload of the message (not including the length header), construct a packet of the appropriate type
     *
     * @param messagePayload   Payload to use in order to construct the packet
     * @return                 Built packet from payload
     */
    public static Packet PacketFromBytes(byte[] messagePayload) {
        Packet packet = switch (PacketType.fromPayload(messagePayload)) {
            case CHOKE -> new ChokePacket();
            case UNCHOKE -> new UnchokePacket();
            case INTERESTED -> new InterestedPacket();
            case NOT_INTERESTED -> new NotInterestedPacket();
            case HAVE -> new HavePacket();
            case BITFIELD -> new BitFieldPacket();
            case REQUEST -> new RequestPacket();
            case PIECE -> new PiecePacket();
            case HANDSHAKE -> new HandshakePacket(messagePayload);
            default -> null;
        };

        if(packet == null || !packet.parse(messagePayload)) {
            System.out.println("[PACKET] Attempt to parse a packet from a given payload has failed! Creating Unknown packet instead");
            packet = new UnknownPacket();
        } else {
            System.out.println("[PACKET] Attempt to parse a packet from a given payload was successful! Parsed packet of type " + packet.getTypeString());
        }

        return packet;
    }
}