package project.packet;

public abstract class Packet {

    protected final PacketType type;

    protected byte[] payload;

    protected Packet(PacketType type) {
        this.type = type;

        this.payload = null;
    }

//    /**
//     * Validates that the payload is of the correct length
//     *
//     * @return   Whether the payload is the correct length
//     */
//    protected boolean validateDataLength() {
//        if(this.type.getPayloadSize() < 0) {
//            return true;
//        }
//
//        // TODO: might wanna throw an exception
//        return this.type.getPayloadSize() == this.payload.length;
//    }
//
//    /**
//     * Prints out the raw bytes of an array for debugging purposes
//     *
//     * @param array   Array to print out
//     */
//    private static void printByteArray(byte[] array) {
//        for (byte b : array) {
//            System.out.format("0x%x ", b);
//        }
//
//        System.out.print("\n");
//    }
//
//    /**
//     * Converts a message object into an array of bytes
//     *
//     * @return                    Current message object as a byte array
//     */
//    public final byte[] getFullMessage() throws NetworkException {
//        if (!validateDataLength()) {
//            throw new NetworkException("Data is not a valid length");
//        }
//
//        int payloadLength = this.payload.length + 1;
//
//        int length = payloadLength + 4; // Adding length field (4 bytes)
//
//        byte[] message = new byte[length];
//
//        // Set the length header
//        ByteBuffer lengthBuffer = ByteBuffer.allocate(4).putInt(payloadLength);
//        System.arraycopy(lengthBuffer.array(), 0, message, 0, 4);
//
//        // Set the type header
//        message[4] = (byte) this.type.getTypeId();
//
//        // Set the payload
//        System.arraycopy(payload, 0, message, 5, length);
//
//        printByteArray(message);
//        return message;
//    }
//
//
//    /**
//     * TODO: finish this function
//     * Creates a message object from a given packet
//     *
//     * @param packet   Received packet
//     * @return         Message object parsed from the given packet
//     */
//    public static Packet fromPacket(byte[] packet) {
//        int messageType = (int) (packet[0]);
//        int messageType = (int) (packet[0]);
//
//        switch(messageType) {
//            default -> { return null; }
//        }
//    }
//
//
//    // TODO is this the way to go?
//    public abstract void buildMessage(byte[] array);

    public abstract byte[] build();

    public abstract boolean parse(byte[] payload);
//    public abstract boolean validate(byte[] payload);

    public static Packet fromType(PacketType type) {
        switch(type) {
//            case 0: return new ChokePacket();
//            case 1: return new UnchokePacket();
//            ...
//            case 7: return new PiecePacket();
        }

        return null;
    }
}