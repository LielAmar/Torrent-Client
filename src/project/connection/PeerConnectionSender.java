package project.connection;

import project.exceptions.NetworkException;
import project.packet.Packet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class PeerConnectionSender extends PeerConnection {

    private DataOutputStream out;
    private BlockingQueue<Packet> messageQueue;

    public PeerConnectionSender(Socket connection, ConnectionState state, BlockingQueue<Packet> outgoingMessageQueue) {
        super(connection, state);
        messageQueue = outgoingMessageQueue;
    }

    public void run() {
        try {
            this.out = new DataOutputStream(this.connection.getOutputStream());
            Packet packet;
            // sendHandshake();

            // sendBitField();
            // // Send my bitfield

            // this.peer.getLatch().await();

            while (state.getConnectionActive()) {
                packet = messageQueue.take();
                sendMessage(packet);
            }

            // Send interest / uninterest
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

    private void sendMessage(Packet message) throws NetworkException
    {
        try {
            byte[] messageBytes = message.build();
            this.out.write(messageBytes);
            this.out.flush();
            // System.out.println("[SENDER] Sent " + message.GetTypeSring() + " message:\nBytes: " + Arrays.toString(messageBytes) + "\nString:"+(new String(messageBytes)) + "\nto peer " + this.state.getPeerId());
            System.out.println("[SENDER] Sent " + message.GetTypeSring() + " message: " + /*Arrays.toString(messageBytes) */"" + " to peer " + this.state.getPeerId());
        } catch(IOException exception){
            exception.printStackTrace();
        }
    }
    
    // private void sendHandshake() {
    //     System.out.println("[SENDER] Sending Handshake Packet");

    //     HandshakePacket packet = new HandshakePacket();

    //     packet.setPeerId(PeerProcess.config.getProcessPeerId());

    //     this.sendBytes(packet.build());

    //     System.out.println("[SENDER] Sent Handshake Packet");
    // }

    // private void sendBitField() {
    //     System.out.println("[SENDER] Sending Bitfield Packet");

    //     BitFieldPacket packet = new BitFieldPacket();

    //     packet.setPayload(PeerProcess.config.getLocalBitSet());

    //     this.sendBytes(packet.build());

    //     System.out.println("[SENDER] Sent Bitfield Packet");
    // }

    // private void sendBytes(byte[] payload) {
    //     try {
    //         this.out.write(payload);
    //         this.out.flush();

    //         System.out.println("[SENDER] Sent message: " + payload + " to peer " + this.state.getPeerId());
    //     } catch(IOException exception){
    //         exception.printStackTrace();
    //     }
    // }
}
