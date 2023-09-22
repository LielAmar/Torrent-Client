import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Arrays;
import java.util.BitSet;

import javax.swing.text.html.parser.Element;

public class PeerConnection extends Thread {

    private static void PrintByteArray(byte[] array)
    {
        // print out the raw bytes of an array, just left in case needed for debugging
        for (byte b : array) {
            System.out.format("0x%x ", b);
        }
        System.out.print("\n");
    }
    private enum MessageType {
        choke(0, 0),
        unchoke(1, 0),
        interested(2, 0),
        notinterested(3, 0),
        have(4, 4),
        bitfield(5, -1),
        request(6, 4),
        piece(7, -1),
        unknown(10, -1);

        public final int typeNum;
        public final int payloadSize;

        private MessageType(int typeNum, int payloadSize) {
            this.typeNum = typeNum;
            this.payloadSize = payloadSize;
        }

        public static MessageType typeFromValue(int value)
        {
            for (MessageType m : MessageType.values()) 
            {
                if(m.typeNum == value)
                {
                    return m;
                }
            }
            return MessageType.unknown;
        } 
    }


    private class Message {
        private MessageType type;
        private byte[] rawData;

        public Message(MessageType type)
        {
            this.type = type;
        }

        public Message(int typeNum)
        {
            this.type = MessageType.typeFromValue(typeNum);
        }

        // this constructs a message object from the data recieved over the network
        // this should recieve full message minus the initial 4 byte message length field
        public Message(byte[] recievedData)
        {
            this.type = MessageType.typeFromValue((int) (recievedData[0]));
            System.out.println("Message Type: " + this.type.name());
            if (recievedData.length != 1) {
                this.rawData = Arrays.copyOfRange(recievedData, 1, recievedData.length);
            } else {
                this.rawData = new byte[0];
            }
        }
        
        public void SetIntData(int data) throws UnsupportedOperationException
        {
            if (!(type == MessageType.have || type == MessageType.request)) {
                throw new UnsupportedOperationException(
                        "Ints can only be written for \'have\' and \'request\' messages");
            }
            rawData = ByteBuffer.allocate(4).putInt(data).array();
        }
        
        public int GetIntData() throws UnsupportedOperationException
        {
            if (!(type == MessageType.have || type == MessageType.request)) {
                throw new UnsupportedOperationException(
                        "Ints can only be read for \'have\' and \'request\' messages");
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(4).put(rawData);
            byteBuffer.rewind();
            return byteBuffer.getInt();
        }

        public BitSet GetBitSet() throws UnsupportedOperationException
        {
            if (!(type == MessageType.have || type == MessageType.request)) {
                throw new UnsupportedOperationException(
                        "Bitsets can only be written for \'have\' and \'request\' messages");
            }
            
            return new BitSet(0);
        }

        private boolean ValidateDataLen()
        {
            if (this.type.payloadSize < 0) {
                return true;
            } else {
                if (this.type.payloadSize == this.rawData.length) {
                    return true;
                } else {
                    System.out.println("Message has an invalid length");
                    return false;
                }
            }
        }
        
        // returns a byte array containing the full message, ready to send
        public byte[] GetFullMessage() throws NetworkException
        {
            // the length value that will be used in the length field of the message
            // doesnt include the size of the length field itself
            System.out.println("Raw data length: " + rawData.length);

            int length = rawData.length + 1;

            // the length including the length field
            int realLength = length + 4; // 

            byte[] fullMessage = new byte[realLength];


            // write the len and type fields
            // length
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4).putInt(length);
            for(int i = 0; i < 4; i++)
            {
                fullMessage[i] = lengthBuffer.array()[i];
            }

            // type
            fullMessage[4] = (byte) (this.type.typeNum);

            if (!ValidateDataLen())
            {
                throw new NetworkException("Data is not a valid length");
            }
            // copy the raw data to the output
            for (int i = 5, j = 0; i < realLength; i++, j++)
            {
                fullMessage[i] = rawData[j];
            }


            System.out.println("Full message bytes");
            PrintByteArray(fullMessage);
            return fullMessage;
        }
    }

    static final String handshakeHeader = "P2PFILESHARINGPROJ";
    

    private Peer peer;
    private int localPeerId;
    private Socket connection;
    private InputStream in;
    private OutputStream out;
    private LinkedList<Message> incomingMessageQueue;

    public PeerConnection(Socket connection, Peer peer, int localPeerId)
    {
        this.connection = connection;
        this.peer = peer;
        this.localPeerId = localPeerId;
        incomingMessageQueue = new LinkedList<Message>();
    }

    public void run()
    {
        int peerId;
        Message test;
        byte[] testBytes;
        
        try
        {
            
            in = connection.getInputStream();
            out = connection.getOutputStream();
            System.out.println("Connection established with peer " + peer.getPeerId());
            SendHandshake();
            peerId = ListenHandshake();
            if (peerId <= 0)
            {
                System.out.println("Handshake failed, closing connection");
                return;
            }
            peer.setPeerId(peerId);
            System.out.println("Handshake received from peer " + peer.getPeerId());

            test = new Message(4);

            test.SetIntData(543);

            System.out.println("Test data: " + test.GetIntData());

            test.GetFullMessage();

            if (1 == 1)
                return;
            
            while (true) 
            {
                ListenMessage();
                while(!incomingMessageQueue.isEmpty())
                {
                    System.out.println("Processing message");
                    incomingMessageQueue.pop();
                }

                // determine if we need to send any messages
            }
        }
        catch (IOException|NetworkException e)
        {
            System.out.println("Disconnect with Client " + peer.getPeerId());
        }
        finally{
            //Close connections
            try{
                in.close();
                out.close();
                connection.close();
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + peer.getPeerId() + " failed");
            }
        }
    }

    private void sendMessage(Message message) throws NetworkException
    {
        // TODO: add logging here
        byte[] buff = message.GetFullMessage();
        try
        {
            out.write(buff);
        }
        catch(IOException e)
        {
            System.out.println("Falied to send message.");
        }
    }

    private void ListenMessage()
    {
        
    }
    
    private void SendHandshake()
    {
        byte[] buff = new byte[32];
        
        int i;
        for (i = 0; i < handshakeHeader.length(); i++) 
        {
            buff[i] = (byte) handshakeHeader.charAt(i);
        }
        // leave 10 zero bytes
        i += 10;

        //System.out.println("Local Peer ID" + localPeerId + "\nHex: " + Integer.toHexString(localPeerId));
        ByteBuffer localPeerIdByteBuffer = ByteBuffer.allocate(4).putInt(localPeerId);
        for(int j = 0; j < 4; j++, i++)
        {
            buff[i] = localPeerIdByteBuffer.array()[j];
        }
        
        try
        {
            out.write(buff);
        }
        catch(IOException e)
        {
            System.out.println("Falied to send handshake.");
        }
    }

    private int ListenHandshake()
    {
        final int handshakeLen = 32;
        byte[] buff = new byte[32];
        int i = 0;
        try
        {
            while(i < 32)
            {
                i += in.read(buff, i, handshakeLen - i);
                //System.out.println("read " + j + "bytes of handshake");
            }
        }
        catch (IOException e)
        {
            System.out.println("Falied to recieve handshake.");
            return -1;
        }

        


        // Decode Handshake

        // verify header    
        String verifyHeader = new String(buff, 0, handshakeHeader.length());
        if (!(verifyHeader.equals(handshakeHeader)))
        {
            System.out.println("Handshake header not valid");
            return -1;
        }
        // verify 0 bytes
        for (int j = handshakeHeader.length(); j < handshakeHeader.length() + 10; j++)
        {
            if (buff[j] != 0) {
                System.out.println("Handshake zero bytes missing");
                return -1;
            }
        }
        //decode peerId
        int peerId = 0;
        ByteBuffer peerIdByteBuffer = ByteBuffer.allocate(4).put(buff, 28, 4);
        peerIdByteBuffer.rewind();
        peerId = peerIdByteBuffer.getInt();

        return peerId;
    }
}
