package project.connection;

import java.io.IOException;
import java.net.Socket;

import project.LocalPeerManager;
import project.message.Message;
import project.message.InternalMessage.InternalMessage;
import project.message.packet.Packet;
import project.message.packet.PacketType;
import project.message.packet.packets.*;
import project.utils.Logger;
import project.utils.Tag;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class PeerConnectionManager extends PeerConnection {

    private final PriorityBlockingQueue<Message> incomingMessageQueue;
    private final PriorityBlockingQueue<Packet> outgoingMessageQueue;

    private final PeerConnectionSender sender;
    private final PeerConnectionListener listener;

    private final PeerConnectionHandler handler;

    public PeerConnectionManager(Socket connection, LocalPeerManager localPeerManager, ConnectionState state) {
        super(connection, localPeerManager, state);

        this.incomingMessageQueue = new PriorityBlockingQueue<>();
        this.outgoingMessageQueue = new PriorityBlockingQueue<>();

        this.sender = new PeerConnectionSender(connection, localPeerManager, state, this.outgoingMessageQueue);
        this.listener = new PeerConnectionListener(connection, localPeerManager, state, this.incomingMessageQueue);

        this.handler = new PeerConnectionHandler(this);
    }


    public PeerConnectionHandler getHandler() {
        return this.handler;
    }


    public void run() {
        super.state.lockHandshake();

        this.sender.start();
        this.listener.start();

        try {
            // Create first packet to send: handshake packet
            this.outgoingMessageQueue.put(new HandshakePacket(super.localPeerManager.getLocalPeerId()));

            // Listen to the first received packet: handshake packet
            Message receivedMessage = this.incomingMessageQueue.take();

            if (!receivedMessage.GetIsPacket()) {
                System.err.println("Recieved A control message before the handshake");

                this.terminate();
                return;
            }
            
            Packet receivedPacket = (Packet) receivedMessage;

            if(receivedPacket.getType() != PacketType.HANDSHAKE) {
                System.err.println("An error occurred when establishing connection with remote peer: " +
                        "FIRST_PACKET_NOT_HANDSHAKE");

                this.terminate();
                return;
            }

            HandshakePacket handshake = (HandshakePacket) receivedPacket;
            
            if(!handshake.isValid()) {
                System.err.println("An error occurred when establishing connection with remote peer: " +
                        "INVALID_HANDSHAKE");

                this.terminate();
                return;
            }

            super.state.setRemotePeerId(handshake.getPeerId());
            Logger.print(Tag.PEER_CONNECTION_MANAGER, "Handshake received and parsed successfully");
            super.state.unlockHandshake();

            // Log the connection according to who connected to who
            if(this.state.isLocalConnectedToRemote()) {
                this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() + " makes a connection to Peer " + this.state.getRemotePeerId() + ".");
            } else {
                this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() + " is connected from Peer " + this.state.getRemotePeerId() + ".");
            }

            // Create and send the second packet: bitfield packet
            this.localPeerManager.acquireBitmapLock();
            this.handler.sendBitfield();
            this.localPeerManager.releaseBitmapLock();

            // Handle incoming packets and prepare replies
            while(super.state.isConnectionActive()) {
                Message incomingMessage = this.incomingMessageQueue.take();

                if(incomingMessage.GetIsPacket())
                {
                    this.handler.handle((Packet) incomingMessage);
                }
                else
                {

                }
            }
        } catch (InterruptedException exception) {
            // TODO: figure out what to do here
//            throw new RuntimeException(e);
        } finally {
            // TODO: same as 3 lines above
//            PeerProcess.localPeerManager.checkTerminateConnection(this);
        }
    }


    /**
     * Prepares a packet to be sent
     *
     * @param packet                  Packet to send
     * @throws InterruptedException   Throws an exception if queuing the received packet fails
     */
    public void preparePacket(Packet packet) throws InterruptedException {
        this.outgoingMessageQueue.put(packet);
    }


    /**
     * Terminates a connection
     */
    public void terminate() {
        // Already terminated
        if (!this.state.isConnectionActive()) {
            return;
        }

        Logger.print(Tag.PEER_CONNECTION_MANAGER, "Terminating the connection with peer " +
                this.state.getRemotePeerId());

        this.state.setConnectionActive(false);

        try {
            this.connection.close();
            this.incomingMessageQueue.add(new UnknownPacket());
            this.outgoingMessageQueue.add(new UnknownPacket());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
    
    public void SendControlMessage(InternalMessage message)
    {
        this.incomingMessageQueue.put(message);
    }
}