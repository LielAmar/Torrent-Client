package project.connection;

import java.net.Socket;

import project.peer.Peer;
import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;
import project.packet.packets.HandshakePacket;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PeerConnectionManager extends PeerConnection {
    private final PeerConnectionSender sender;
    private final PeerConnectionListener listener;
    
    BlockingQueue<Packet> incomingMessageQueue;
    BlockingQueue<Packet> outgoingMessageQueue;

    public PeerConnectionManager(Socket connection, ConnectionState state)
    {
        super(connection, state);
        incomingMessageQueue = new LinkedBlockingQueue<Packet>();
        outgoingMessageQueue = new LinkedBlockingQueue<Packet>();
        sender = new PeerConnectionSender(connection, state, outgoingMessageQueue);
        listener = new PeerConnectionListener(connection, state, incomingMessageQueue);
    }

    public void run()
    {
        state.lockHandshake();
        sender.start();
        listener.start();
        try
        {
            outgoingMessageQueue.put(new HandshakePacket(state.getLocalId()));
            Packet receivedPacket = incomingMessageQueue.take();
            if(!(receivedPacket.GetType() == PacketType.HANDSHAKE))
            {
                System.out.println("Handshake not recieved as first message");
                throw new NetworkException("Handshake not recieved as first message");
            }

            // the if statement above ensures that this works
            HandshakePacket handshake = (HandshakePacket) receivedPacket;
            
            if(!handshake.getValid())
            {
                System.out.println("recieved invalid handshake");
                throw new NetworkException("Recieved an invalid handshake");
            }

            System.out.println("Handshake recieved, starting listening");
            state.unlockHandshake();
        }
        catch (InterruptedException | NetworkException e)
        {            
            throw new RuntimeException(e);
        }
        
    }
}
