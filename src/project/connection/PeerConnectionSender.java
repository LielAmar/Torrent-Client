package project.connection;

import project.PeerProcess;
import project.packet.packets.BitFieldPacket;
import project.packet.packets.HandshakePacket;
import project.peer.Peer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PeerConnectionSender extends PeerConnection {

    private DataOutputStream out;

    public PeerConnectionSender(Socket connection, Peer peer) {
        super(connection, peer);
    }

    public void run() {
        try {
            this.out = new DataOutputStream(this.connection.getOutputStream());

            sendHandshake();

            sendBitField();
            // Send my bitfield

            this.peer.getLatch().await();

            // Send interest / uninterest
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                System.out.println("[SENDER] Closing connection with peer " + this.peer.getPeerId());

                this.out.close();
                this.connection.close();
            } catch(IOException ioException){
                System.out.println("[SENDER] Failed to close connection with peer " + this.peer.getPeerId());
            }
        }
    }

    private void sendHandshake() {
        System.out.println("[SENDER] Sending Handshake Packet");

        HandshakePacket packet = new HandshakePacket();

        packet.setPeerId(PeerProcess.config.getProcessPeerId());

        this.sendBytes(packet.build());

        System.out.println("[SENDER] Sent Handshake Packet");
    }

    private void sendBitField() {
        System.out.println("[SENDER] Sending Bitfield Packet");

        BitFieldPacket packet = new BitFieldPacket();

        packet.setPayload(PeerProcess.config.getLocalBitSet());

        this.sendBytes(packet.build());

        System.out.println("[SENDER] Sent Bitfield Packet");
    }

    private void sendBytes(byte[] payload) {
        try {
            this.out.write(payload);
            this.out.flush();

            System.out.println("[SENDER] Sent message: " + payload + " to peer " + this.peer.getPeerId());
        } catch(IOException exception){
            exception.printStackTrace();
        }
    }
}
