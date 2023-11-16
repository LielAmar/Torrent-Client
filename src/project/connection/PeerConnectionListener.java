package project.connection;

import project.packet.Packet;
import project.packet.PacketType;
import project.packet.packets.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class PeerConnectionListener extends PeerConnection {

    private final BlockingQueue<Packet> messageQueue;

    private DataInputStream in;

    public PeerConnectionListener(Socket connection, ConnectionState state, BlockingQueue<Packet> incomingMessageQueue) {
        super(connection, state);

        this.messageQueue = incomingMessageQueue;
    }

    public void run() {
        try {
            // Get the input stream
            this.in = new DataInputStream(this.connection.getInputStream());

            // Listen to the first incoming message (handshake)
            this.messageQueue.put(listenToHandshake());

            // Wait for the handshake to be finished
            // TODO: might be able to remove this wait
            this.state.waitForHandshake();
            this.state.unlockHandshake();

            // Start listening to incoming messages
            while (this.state.getConnectionActive()) {
                this.messageQueue.put(listenToMessage());
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

    private byte[] readBytes(int length) {
        try {
            if(length > 0) {
                System.out.println("[DEBUG] Trying to read " + length + " bytes");
                byte[] message = new byte[length];
                this.in.readFully(message, 0, message.length);
                return message;
            }
        } catch(IOException exception) {
            System.out.println("[DEBUG] Tried to read and failed");
//            exception.printStackTrace();
        }

        return null;
    }

    private Packet listenToMessage() {
        // TODO: check if there's a message in the input stream

        // Read the length of the incoming packet
        byte[] lengthHeaderBytes = this.readBytes(4);
        int lengthHeader = lengthHeaderBytes == null ? 0 : ByteBuffer.wrap(lengthHeaderBytes).getInt();

        if(lengthHeader < 1) {
            return new UnknownPacket();
        }

        // Read 'lengthHeader' bytes, which is the content of the packet
        byte[] payload = this.readBytes(lengthHeader);

        // Create packet from the read payload
        return Packet.PacketFromBytes(payload);
    }

    private Packet listenToHandshake() {
        System.out.println("[LISTENER] Listening to Handshake Packet");
        byte[] message = this.readBytes(HandshakePacket.HANDSHAKE_LENGTH);
        return new HandshakePacket(message);
    }
}
