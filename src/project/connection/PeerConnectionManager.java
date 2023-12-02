package project.connection;

import java.io.IOException;
import java.net.Socket;

import project.LocalPeerManager;
import project.message.InternalMessage.InternalMessage;
import project.message.InternalMessage.InternalMessages.NewLocalPeiceIntMes;
import project.message.packet.Packet;
import project.message.packet.PacketType;
import project.message.packet.packets.*;
import project.utils.Logger;
import project.utils.Tag;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PeerConnectionManager extends PeerConnection {

    private final BlockingQueue<Packet> incomingPacketQueue;
    private final BlockingQueue<Packet> outgoingPacketQueue;

    // this is only used until the handshake is recieved, afterwards it is ignored
    private final BlockingQueue<InternalMessage> incomingControlMessageQueue;

    private final PeerConnectionSender sender;
    private final PeerConnectionListener listener;

    private final PeerConnectionHandler handler;

    public PeerConnectionManager(Socket connection, LocalPeerManager localPeerManager, ConnectionState state) {
        super(connection, localPeerManager, state);

        this.incomingPacketQueue = new LinkedBlockingQueue<>();
        this.outgoingPacketQueue = new LinkedBlockingQueue<>();

        this.incomingControlMessageQueue = new LinkedBlockingQueue<>();

        this.sender = new PeerConnectionSender(connection, localPeerManager, state, this.outgoingPacketQueue);
        this.listener = new PeerConnectionListener(connection, localPeerManager, state, this);

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
            this.outgoingPacketQueue.put(new HandshakePacket(super.localPeerManager.getLocalPeerId()));

            // Listen to the first received packet: handshake packet            
            Packet receivedPacket = this.incomingPacketQueue.take();

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
            Logger.print(Tag.PEER_CONNECTION_MANAGER, "Handshake received and parsed successfully from peer " + handshake.getPeerId());
            super.state.unlockHandshake();

	        this.setName("Peer " + handshake.getPeerId() + " Manager");
            sender.setName("Peer " + handshake.getPeerId() + " Sender");
            listener.setName("Peer " + handshake.getPeerId() + " Listener");

            // Log the connection according to who connected to who
            if(this.state.isLocalConnectedToRemote()) {
                this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() + " makes a connection to Peer " + this.state.getRemotePeerId() + ".");
            } else {
                this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() + " is connected from Peer " + this.state.getRemotePeerId() + ".");
            }

            // Create and send the second packet: bitfield packet
            // this.localPeerManager.acquireBitmapLock();
            this.handler.sendBitfield();
            // this.localPeerManager.releaseBitmapLock();

            // while (super.state.isConnectionActive()) {}

            // Handle incoming packets and prepare replies
            while (super.state.isConnectionActive()) {
                
                /*
                 * this is synchronized to avoid a deadlock where where the if evals to true and we start to wait, 
                 * but then before we wait, a new message/packet arives and notifies, but we havent wait()ed yet, so the notify is lost
                 */
                synchronized (this)
                {
                    if(this.incomingControlMessageQueue.isEmpty() && this.incomingPacketQueue.isEmpty())
                    {
                        wait();
                    }
                }
                if(!(this.incomingControlMessageQueue.isEmpty()))
                {
                    HandleControlMessage(this.incomingControlMessageQueue.take());
                } 
                else if (!(this.incomingPacketQueue.isEmpty()))
                {
                    Packet incomingPacket = this.incomingPacketQueue.take();
                    this.handler.handle(incomingPacket);
                }
                else
                {
                    System.err.println(
                            "It should be impossible to get here without having a message or packet to process");
                    
                    // throw new RuntimeException("PeerConnectionManager for peer " + this.state.getRemotePeerId() + " was woken up without a message to process");
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
        this.outgoingPacketQueue.put(packet);
    }


    /**
     * Terminates a connection
     */
    private void terminate() {
        // Already terminated
        if (!this.state.isConnectionActive()) {
            return;
        }

        Logger.print(Tag.PEER_CONNECTION_MANAGER, "Terminating the connection with peer " +
                this.state.getRemotePeerId());

        this.state.setConnectionActive(false);

        try {
            this.connection.close();
            this.incomingPacketQueue.add(new UnknownPacket());
            this.outgoingPacketQueue.add(new UnknownPacket());

            this.listener.join();
            this.sender.join();
            //Logger.print(Tag.EXITING, String.format("PeerManager %d, Listener killed: %b, Sender killed: %b", this.state.getRemotePeerId(), !listener.isAlive(), !sender.isAlive()));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        catch (InterruptedException e)
        {
            System.err.println("Exception while waiting for sender and listener to finish");
            System.err.println(e);
            e.printStackTrace();
        }
    }
    
    // used by other threads to send control messages to this thread
    public synchronized void SendControlMessage(InternalMessage message) 
    {
        try {
            this.incomingControlMessageQueue.put(message);
            notifyAll();
        } catch (InterruptedException e) {
            System.err.println(
                    "An error occured while trying to send a control message to the PeerConnectionManager for peer "
                            + this.state.getRemotePeerId());
            throw new RuntimeException(e);
        }
    }
    
    // used by the listener thread to send packets it to this manager thread 
    public synchronized void SendRecievedPacket(Packet packet)
    {
        try {
            this.incomingPacketQueue.put(packet);
            notifyAll();
        }
        catch (InterruptedException e)
        {
            System.err.println("An error occured while trying to send a packet from listener to PeerConnectionManger for peer  "+ this.state.getRemotePeerId());
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    private void HandleControlMessage(InternalMessage message) throws UnsupportedOperationException
    {
        switch (message.getType()) {
            case TERMINATE:
                terminate();
                break;
            case NEW_LOCAL_PIECE:
                this.handler.sendHave(((NewLocalPeiceIntMes) message).GetPieceIndex());
                this.handler.SetInterestAndRequest();
                break;
            case UNCHOKE_THREAD:
                if(this.state.isLocalChoked()) {
                    this.state.setLocalChoked(false);
                    this.handler.sendUnchoke();
                }
                break;
            case CHOKE_THREAD:
                if(!this.state.isLocalChoked()) {
                    this.state.setLocalChoked(true);
                    this.handler.sendChoke();
                }
                break;
            default:
                System.err.println("PeerConnectionManager recieved a control message of type " + message.getTypeString()
                        + " which isn't allowed");
                throw new UnsupportedOperationException(
                        "Control Message of invalid type " + message.getTypeString() + " in PeerConnectionManager");
        }
    }
    
    
    public String dumpState()
    {
        Logger.print(Tag.DEBUG, "Dumping state for peer " + this.state.getRemotePeerId());
        return String.format("Peer Connection %d%nlocalInterested: %b%nlocalRequested: %b%nlocalRequestedID: %d%nshouldBeInterested: %b%nLocalChoked: %b%nremotechoked%b%n", this.state.getRemotePeerId(), this.state.isLocalInterestedIn(), this.state.getPieceRequested(), this.state.getPieceRequestedID(), this.handler.hasInterest(), this.state.isLocalChoked(), this.state.isRemoteChoked());
    }
}
