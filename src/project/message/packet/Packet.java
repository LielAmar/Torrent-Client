package project.message.packet;

import project.exceptions.NetworkException;
import project.message.Message;
import project.message.packet.packets.*;

public abstract class Packet extends Message{

    protected final PacketType type;

    protected Packet(PacketType type) {
        super(true);
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
        Packet packet;
        switch (PacketType.fromPayload(messagePayload)) {
        case CHOKE:
            packet =  new ChokePacket();
            break;
        case UNCHOKE:
            packet =  new UnchokePacket();
            break;
        case INTERESTED:
            packet =  new InterestedPacket();
            break;
        case NOT_INTERESTED:
            packet =  new NotInterestedPacket();
            break;
        case HAVE:
            packet =  new HavePacket();
            break;
        case BITFIELD:
            packet =  new BitFieldPacket();
            break;
        case REQUEST:
            packet =  new RequestPacket();
            break;
        case PIECE:
            packet =  new PiecePacket();
            break;
        case HANDSHAKE:
            packet =  new HandshakePacket(messagePayload);
            break;
        default:
            packet = null;
            break;
        };

        if(packet == null || !packet.parse(messagePayload)) {
            packet = new UnknownPacket();
        }

        return packet;
    }
}