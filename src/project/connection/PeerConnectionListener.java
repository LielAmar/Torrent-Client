package project.connection;

import project.packet.Packet;
import project.packet.PacketType;
import project.packet.packets.HandshakePacket;
import project.peer.Peer;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class PeerConnectionListener extends PeerConnection {

    private DataInputStream in;
    private byte[] payload;

    public PeerConnectionListener(Socket connection, Peer peer) {
        super(connection, peer);
    }

    public void run() {
        try {
            // Get the input stream
            this.in = new DataInputStream(this.connection.getInputStream());

            // Listen to the first incoming message, make sure it's a Handshake Packet
            Packet handshakePacket = listenToHandshake();

            // If the handshake packet is invalid, close the connection
            if(handshakePacket == null) {
                System.out.println("[LISTENER] closing connection");
                return;
            }

            // Start listening to incoming messages
            while (true) {
                listenToMessage();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                System.out.println("[LISTENER] Closing connection with peer " + this.peer.getPeerId());

                this.in.close();
                this.connection.close();
            } catch(IOException ioException){
                System.out.println("[LISTENER] Failed to close connection with peer " + this.peer.getPeerId());
            }
        }
    }

    // TODO: make sure it can read one packet at a time, and doesn't read multiple
    private byte[] readBytes() {
        try {
            int length = this.in.readInt();

            if(length > 0) {
                byte[] message = new byte[length];
                this.in.readFully(message, 0, message.length);
                return message;
            }
        } catch(IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    private Packet listenToMessage() {
        this.payload = this.readBytes();

        if(this.payload == null) {
            return null;
        }

        PacketType type = PacketType.fromPayload(this.payload);
        Packet packet = Packet.fromType(type);

        if(packet == null || !packet.parse(this.payload)) {
            return null;
        }

        // TODO: handle every possible packet type
        return packet;
    }

    private Packet listenToHandshake() {
        System.out.println("[LISTENER] Listening to Handshake Packet");
        this.payload = this.readBytes();

        HandshakePacket packet = new HandshakePacket();

        if(!packet.parse(this.payload)) {
            System.out.println("[LISTENER] The received Handshake Packet was malformed");
            return null;
        }

        System.out.println("[LISTENER] Received Handshake Packet from peer id " + packet.getPeerId());

        // If the expected peer id and the received peer id are different, then failed
        if(this.peer.getPeerId() != -1 && this.peer.getPeerId() != packet.getPeerId()) {
            System.out.println("[LISTENER] Expected Handshake Packet from peer " + this.peer.getPeerId() + " but got from " + packet.getPeerId() + " instead!");
            return null;
        }

        this.peer.setPeerId(packet.getPeerId());

        return packet;
    }
}
