package project.connection;

import project.exceptions.NetworkException;
import project.packet.Packet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class PeerConnectionSender extends PeerConnection {

    private final BlockingQueue<Packet> messageQueue;

    private DataOutputStream out;

    public PeerConnectionSender(Socket connection, ConnectionState state, BlockingQueue<Packet> outgoingMessageQueue) {
        super(connection, state);

        this.messageQueue = outgoingMessageQueue;
    }

    public void run() {
        try {
            // Get the output stream
            this.out = new DataOutputStream(this.connection.getOutputStream());
            Packet packet;

            // Send the first outgoing message (handshake)
            packet = this.messageQueue.take();
            sendMessage(packet);

            // Wait for the handshake to be finished
            // TODO: might be able to remove this wait
            this.state.waitForHandshake();
            this.state.unlockHandshake();

            // Send the second outgoing message (bitfield)
            packet = this.messageQueue.take();
            sendMessage(packet);

            // Start sending outgoing messages
            while (super.state.getConnectionActive()) {
                packet = this.messageQueue.take();
                sendMessage(packet);
            }

            // Send interest / uninterested
        } catch (IOException | InterruptedException | NetworkException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                System.out.println("[SENDER] Closing connection with peer " + this.state.getPeerId());

                this.out.close();
                this.connection.close();
            } catch (IOException ioException) {
                System.out.println("[SENDER] Failed to close connection with peer " + this.state.getPeerId());
            }
        }
    }

    private void sendMessage(Packet message) throws NetworkException {
        try {
            byte[] messageBytes = message.build();

            this.out.write(messageBytes);
            this.out.flush();

            // System.out.println("[SENDER] Sent " + message.getTypeString() + " message:\nBytes: " + Arrays.toString(messageBytes) + "\nString:"+(new String(messageBytes)) + "\nto peer " + this.state.getPeerId());
            System.out.println("[SENDER] Sent " + message.getTypeString() + " message: " + /*Arrays.toString(messageBytes) */"" + " to peer " + this.state.getPeerId());
        } catch(IOException exception){
            exception.printStackTrace();
        }
    }
}
