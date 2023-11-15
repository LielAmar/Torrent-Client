package project.connection;

import project.packet.Packet;
import project.packet.PacketType;
import project.packet.packets.*;
// import project.packet.packets.HandshakePacket;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class PeerConnectionListener extends PeerConnection {

    private DataInputStream in;
    private byte[] lengthHeaderBytes;
    private int lengthHeader;
    private byte[] payload;
    private BlockingQueue<Packet> messageQueue;


    public PeerConnectionListener(Socket connection, ConnectionState state, BlockingQueue<Packet> incomingMessageQueue) {
        super(connection, state);
        messageQueue = incomingMessageQueue;
    }

    public void run() {
        try {
            // Get the input stream
            this.in = new DataInputStream(this.connection.getInputStream());

            // Listen to the first incoming message, make sure it's a Handshake Packet
            Packet handshakePacket = listenToHandshake();

            // System.out.println("[LISTENER] Putting handshake in the queue");
            messageQueue.put(handshakePacket);
            this.state.waitForHandshake();
            // System.out.println("[LISTENER] Woke back up, listening for messages");
            // Start listening to incoming messages
            while (state.getConnectionActive()) {
                listenToMessage();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                System.out.println("[LISTENER] Closing connection with peer " + this.state.getPeerId());

                this.in.close();
                this.connection.close();
            } catch(IOException ioException){
                System.out.println("[LISTENER] Failed to close connection with peer " + this.state.getPeerId());
            }
        }
    }

    // TODO: make sure it can read one packet at a time, and doesn't read multiple
    private byte[] readBytes(int length) {
        try {

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
        this.lengthHeaderBytes = this.readBytes(4);
        ByteBuffer lengthHeaderBuffer = ByteBuffer.allocate(4).put(lengthHeaderBytes,  0 , 4);
        lengthHeaderBuffer.rewind();
        this.lengthHeader = lengthHeaderBuffer.getInt();
        if (lengthHeader < 1)
        {
            return new UnknownPacket();
        }
        this.payload = this.readBytes(lengthHeader);


        Packet packet = Packet.PacketFromBytes(payload);
        
        return packet;
    }

    private Packet listenToHandshake() {
        System.out.println("[LISTENER] Listening to Handshake Packet");
        this.payload = this.readBytes(HandshakePacket.GetHandshakeLength());

        HandshakePacket packet = new HandshakePacket(payload);

        if(!packet.getValid()) {
            System.out.println("[LISTENER] The received Handshake Packet was malformed");
            return null;
        }

        System.out.println("[LISTENER] Received Handshake Packet from peer id " + packet.getPeerId());

        // If the expected peer id and the received peer id are different, then failed
        if(this.state.getPeerId() != -1 && this.state.getPeerId() != packet.getPeerId()) {
            System.out.println("[LISTENER] Expected Handshake Packet from peer " + this.state.getPeerId() + " but got from " + packet.getPeerId() + " instead!");
            return null;
        }

        this.state.setPeerId(packet.getPeerId());

        return packet;
    }
}
