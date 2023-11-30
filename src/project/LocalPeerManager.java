package project;

import project.connection.ConnectionState;
import project.connection.PeerConnectionManager;
import project.connection.piece.Piece;
import project.connection.piece.PieceStatus;
import project.message.InternalMessage.InternalMessage;
import project.message.InternalMessage.InternalMessages.ChokeThreadIntMes;
import project.message.InternalMessage.InternalMessages.NewLocalPeiceIntMes;
import project.message.InternalMessage.InternalMessages.ReceivedIntMes;
import project.message.InternalMessage.InternalMessages.TerminateIntMes;
import project.message.InternalMessage.InternalMessages.UnchokeThreadIntMes;
import project.utils.Logger;
import project.utils.Tag;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LocalPeerManager extends Thread {

    private static final Random random = new Random();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


    private static final String DIRECTORY = "peer_%d";

    private final int localPeerId;

    private final Configuration config;
    private final Logger logger;

    private final ArrayList<PeerConnectionManager> connectedPeers;

    private final Piece[] localPieces;

    private final BlockingQueue<InternalMessage> incomingControlMessages;

    private boolean localFileCompleted;
    private AtomicBoolean allPeersConnected;

    /*
     * CHANGE TO the bitmap lock:
     * Since i am swapping to a message passing system for interthread communication, im going to use this lock 
     * in a more standard way. which the LocalPeerManager thread wants to update the local pieces array, it will aquire the writelock, 
     * thus preventing any of the peerConnectionMangers from aquiring the readlock. It will also send a message to each of the peers informing them
     * of changes to the local pieces before it releases the lock and allowing reads again.
     * The only time the peerConnectionMangers should need to use the exposed interface to aquire the readlock is when 
     * they are sending their bitfield packet. Other times, when just reading the bitmap it will be done automatically
     */
    private final ReadWriteLock bitmapLock;
    private final Lock choosePieceLock;

    private PeerConnectionManager optimisticallyUnchokedPeer;

    public LocalPeerManager(int localPeerId, Configuration config) {
        this.incomingControlMessages = new LinkedBlockingQueue<>();
        this.localPeerId = localPeerId;

        this.config = config;
        this.logger = new Logger(Paths.get(String.format(DIRECTORY, this.localPeerId) +
                File.separator + "log_peer_" + this.localPeerId + ".log").toAbsolutePath().toString());

        this.localPieces = new Piece[this.config.getNumberOfPieces()];
        localFileCompleted = false;
        
        //we connect to the peers after we create and start this thread
        allPeersConnected = new AtomicBoolean(false);


        this.connectedPeers = new ArrayList<>();

        this.bitmapLock = new ReentrantReadWriteLock();
        this.choosePieceLock = new ReentrantLock();

        this.optimisticallyUnchokedPeer = null;

        // Start the scheduler for reevaluating unchoked peers & optimistic unchoked peer
        executor.scheduleAtFixedRate(this::reevaluateUnchokedPeers, this.config.getUnchokingInterval(),
                this.config.getUnchokingInterval(), TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::reevaluateOptimisticPeer, this.config.getOptimisticUnchokingInterval(),
                this.config.getOptimisticUnchokingInterval(), TimeUnit.SECONDS);
    }

    public int getLocalPeerId() {
        return this.localPeerId;
    }

    public Configuration getConfig() {
        return this.config;
    }

    public Logger getLogger() {
        return logger;
    }

    public Piece[] getLocalPieces() {
        try {
            this.bitmapLock.readLock().lock();

            return this.localPieces;
        }
        finally {
            this.bitmapLock.readLock().unlock();
        }
    }

    public int getLocalPiecesCount() {
        this.bitmapLock.readLock().lock();
        int count = 0;

        for (int i = 0; i < this.localPieces.length; i++) {
            if (this.localPieces[i] != null && this.localPieces[i].getStatus() == PieceStatus.HAVE) {
                count++;
            }
        }
        this.bitmapLock.readLock().unlock();

        return count;
    }

    private void setLocalPiece(int pieceId, PieceStatus status, byte[] content) {
        Logger.print(Tag.LOCAL_PEER_MANAGER, "Updating piece " + pieceId + " status to " + status.name());

        this.bitmapLock.writeLock().lock();

        // If the local piece with the given id does not exist, create it. Otherwise, update the existing piece
        if (this.localPieces[pieceId] == null) {
            this.localPieces[pieceId] = new Piece(status, content);
        } else {
            this.localPieces[pieceId].setStatus(status);
            this.localPieces[pieceId].setContent(content);
        }

        // If the piece status is set to HAVE and announce is true, send an announcement to all connections
        if (status == PieceStatus.HAVE) {
            announce(new NewLocalPeiceIntMes(pieceId));
        }

        this.bitmapLock.writeLock().unlock();
    }

    // send a message to all peerConnections
    private void announce(InternalMessage message) {
        // Create a copy to avoid modifying a list we're looping over
        List<PeerConnectionManager> connectedPeersCopy = new ArrayList<>(this.connectedPeers);

        for (PeerConnectionManager peerConnection : connectedPeersCopy) {
            peerConnection.SendControlMessage(message);;
        }
    }


    public PeerConnectionManager connectToPeer(Socket socket) {
        PeerConnectionManager manager = this.connectToPeer(-1, socket);

        // This method is being called when remote is connecting to local, in which case we update this value
        // to false in order to remember remote connected to local and not local connected to remote
        manager.getConnectionState().setLocalConnectedToRemote(false);

        return manager;
    }

    /**
     * Connects the local peer to a remote peer through a given socket
     *
     * @param remotePeerId   The ID of the remote peer
     * @param socket         Socket to connect over
     * @return               A PeerConnectionManager object, managing the connection between local and remote peers.
     */
    public PeerConnectionManager connectToPeer(int remotePeerId, Socket socket) {
        Logger.print(Tag.LOCAL_PEER_MANAGER, "Creating a connection between local peer (" + localPeerId +
                ") and remote peer (" + remotePeerId + ")");

        ConnectionState state = new ConnectionState(remotePeerId);
        PeerConnectionManager connectionManager = new PeerConnectionManager(socket, this, state);

        this.connectedPeers.add(connectionManager);

        return connectionManager;
    }

    /**
     * Sets up the initial pieces of the targeted file based on whether the local peer has it or not
     *
     * @param hasFile   Whether the local peer has the target file or trying to receive it
     */
    public void setupInitialPieces(boolean hasFile) throws IOException {
        // If the local peer doesn't have the file, set all local pieces to NOT_HAVE with null content
        if (!hasFile) {
            for (int i = 0; i < this.config.getNumberOfPieces(); i++) {
                this.localPieces[i] = new Piece(PieceStatus.NOT_HAVE, null);
            }

            return;
        }

        // Otherwise, read the target file and set all local pieces to HAVE with the matching content
        byte[] localFileBytes = this.getLocalFileBytes();

        for (int i = 0; i < this.config.getNumberOfPieces(); i++) {
            // The piece size is either the specified piece size, or smaller if it's an incomplete piece
            int pieceSize = Math.min(this.config.getPieceSize(),
                    this.config.getFileSize() - i * this.config.getPieceSize());
            byte[] pieceContent = new byte[pieceSize];

            System.arraycopy(localFileBytes, i * this.config.getPieceSize(), pieceContent, 0, pieceSize);
            this.localPieces[i] = new Piece(PieceStatus.HAVE, pieceContent);
        }
    }

    /**
     * Opens the target file and returns its content as a byte array
     *
     * @return               Target file content as a byte array
     * @throws IOException   Throws an IOException if reading the target file failed
     */
    private byte[] getLocalFileBytes() throws IOException {
        String filePath = String.format(DIRECTORY, this.localPeerId) + File.separator + this.config.getFileName();
        File file = new File((new File(filePath)).getAbsolutePath());

        byte[] data = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(data);
        } catch (IOException exception) {
            throw new IOException(exception);
        }

        return data;
    }

    /**
     * Returns the index of a random piece the remote peer has and the local piece doesn't
     *
     * @param remotePieces   Pieces of the remote peer
     * @return               Chosen piece index, -1 if no such piece
     */
    public int choosePieceToRequest(PieceStatus[] remotePieces) {
        this.choosePieceLock.lock();

        ArrayList<Integer> desired = new ArrayList<>();

        for (int i = 0; i < remotePieces.length; i++) {
            if (remotePieces[i] == PieceStatus.HAVE && this.localPieces[i].getStatus() == PieceStatus.NOT_HAVE) {
                desired.add(i);
            }
        }

        int randomIndex = -1;

        if (!desired.isEmpty()) {
            randomIndex = desired.get(random.nextInt(desired.size()));
            this.localPieces[randomIndex].setStatus(PieceStatus.REQUESTED);
        }

        this.choosePieceLock.unlock();

        return randomIndex;
    }

    public void cancelPieceRequest(int pieceIndex) {
        this.choosePieceLock.lock();
        Piece p = this.localPieces[pieceIndex];
        if (p.getStatus() != PieceStatus.REQUESTED) {
            throw new RuntimeException("Tried to cancel peiece request for a peice that isn't requested");
        }
        this.choosePieceLock.unlock();
    }

    /**
     * Re-evaluates the unchoked remote peers based on the download speed with each peer.
     * After re-evaluation, the download speed of all peers is being reset.
     */
    private void reevaluateUnchokedPeers() {
        // Go over all connected peers, sort them based on the download speed, filter based on interest and
        // keep the Configuration#getNumberOfPreferredNeighbors first peers. These are the peers to be unchoke.
        List<PeerConnectionManager> unchokedTemp;
        List<PeerConnectionManager> unchoked;


        if(!completedTransfer())
        {
            unchokedTemp = this.connectedPeers.stream()
                    .sorted((pA, pB) -> {
                        // if(pA.getConnectionState().getDownloadSpeed() < pB.getConnectionState().getDownloadSpeed()) {
                        //     return 1;
                        // } else if(pA.getConnectionState().getDownloadSpeed() > pB.getConnectionState().getDownloadSpeed()) {
                        //     return -1;
                        // } else {
                        //     return random.nextInt(2) == 0 ? 1 : -1;
                        // }
                        return pA.getConnectionState().getDownloadSpeed() - pB.getConnectionState().getDownloadSpeed();
                    }).filter(peer -> peer.getConnectionState().isInterested()).collect(Collectors.toList());
            // check for ties, and randomize if necessary
            if(unchokedTemp.size() > this.config.getNumberOfPreferredNeighbors())
            {
                // if the peer just before and just after the cutoff both have the same download speed, then there is a tie that needs to be resolved
                if (unchokedTemp.get(this.config.getNumberOfPreferredNeighbors() - 1).getConnectionState()
                        .getDownloadSpeed() == unchokedTemp.get(this.config.getNumberOfPreferredNeighbors())
                                .getConnectionState().getDownloadSpeed()) {
                    int tiedSpeed = unchokedTemp.get(this.config.getNumberOfPreferredNeighbors() - 1)
                            .getConnectionState()
                            .getDownloadSpeed();
                    int peersAboveTied = (int) (unchokedTemp.stream().filter(peer -> {
                        return peer.getConnectionState().getDownloadSpeed() > tiedSpeed;
                    }).count());
                    int slotsForTiedPeers = this.config.getNumberOfPreferredNeighbors() - peersAboveTied;
                    int tiedPeers = (int) (unchokedTemp.stream().filter(peer -> {
                        return peer.getConnectionState().getDownloadSpeed() == tiedSpeed;
                    }).count());

                    List<Integer> l = IntStream.range(0, tiedPeers).boxed().collect(Collectors.toList());
                    Collections.shuffle(l);
                    l = l.stream().limit(slotsForTiedPeers).collect(Collectors.toList());
                    ;
                    List<PeerConnectionManager> unchokedRandomized = new ArrayList<>();
                    for (int i = 0; i < peersAboveTied; i++) {
                        unchokedRandomized.add(unchokedTemp.get(i));
                    }
                    for (int i = 0; i < slotsForTiedPeers; i++) {
                        unchokedRandomized.add(unchokedTemp.get(peersAboveTied + i));
                    }
                    unchokedTemp = unchokedRandomized;
                }
            }
            unchokedTemp = unchokedTemp.stream().limit(this.config.getNumberOfPreferredNeighbors()).collect(Collectors.toList());
        }
        else {
            unchokedTemp = new ArrayList<>(this.connectedPeers);
            Collections.shuffle(unchokedTemp);
            unchokedTemp = unchokedTemp.stream().limit(this.config.getNumberOfPreferredNeighbors()).collect(Collectors.toList());
        }
        // the choked list filter operation (line 376 at time of writing) was complaining about unchoked not being final or final-like and this made it happy
        unchoked = unchokedTemp;

        // The rest of the connected peers (except for the optimistically unchoke peer) are to be choked.
        List<PeerConnectionManager> choked = this.connectedPeers.stream()
                .filter(peer -> peer != this.optimisticallyUnchokedPeer && !unchoked.contains(peer)).collect(Collectors.toList());;

        Logger.print(Tag.EXECUTOR, "Re-evaluated unchoked remote peers. Unchoking peers: " +
                unchoked.stream().map(peer -> peer.getConnectionState().getRemotePeerId() + "")
                        .collect(Collectors.joining(",")));

        unchoked.forEach(peer -> {
            // if(peer.getConnectionState().isLocalChoked()) {
            //     peer.getConnectionState().setLocalChoked(false);
            //     peer.getHandler().sendUnchoke();
            // }
            peer.SendControlMessage(new UnchokeThreadIntMes());
        });

        Logger.print(Tag.EXECUTOR, "Re-evaluated choked remote peers. Choking peers: " +
                choked.stream().map(peer -> peer.getConnectionState().getRemotePeerId() + "")
                        .collect(Collectors.joining(",")));

        choked.forEach(peer -> {
            // if(!peer.getConnectionState().isLocalChoked()) {
            //     peer.getConnectionState().setLocalChoked(true);
            //     peer.getHandler().sendChoke();
            // }
            peer.SendControlMessage(new ChokeThreadIntMes());
        });

        this.connectedPeers.forEach(peer -> peer.getConnectionState().resetDownloadSpeed());

        this.logger.log("Peer " + this.localPeerId + " has the preferred neighbors " +
                unchoked.stream().map(peer -> peer.getConnectionState().getRemotePeerId() + "")
                        .collect(Collectors.joining(","))
                + ".");
    }

    /**
     * Re-evaluates the optimistically unchoked remote peer randomly.
     */
    private void reevaluateOptimisticPeer() {
	Logger.print(Tag.DUMP, "Preparing to dump entire state");
	String missing = "Missing pieces: ";
        for(int i = 0; i< config.getNumberOfPieces();i++)
        {
            if (this.localPieces[i].getStatus() != PieceStatus.HAVE) {
                missing = missing + i + " ";
            }
        }
        Logger.print(Tag.DUMP, missing);
        missing = "Requested pieces: ";
        for(int i = 0; i< config.getNumberOfPieces();i++)
        {
            if (this.localPieces[i].getStatus() == PieceStatus.REQUESTED) {
                missing = missing + i + " ";
            }
        }
        Logger.print(Tag.DUMP, missing);
        String missingPeer;
        for(PeerConnectionManager peer: this.connectedPeers)
        {
            missingPeer = "Missing pieces for peer " + peer.getConnectionState().getRemotePeerId() + ": ";
            for (int i = 0; i < config.getNumberOfPieces(); i++) {
                if (peer.getConnectionState().getPieces()[i] != PieceStatus.HAVE) {
                    missingPeer = missingPeer + i + " ";
                }
            }
            Logger.print(Tag.DUMP, missingPeer);
        }
        for(PeerConnectionManager peer: this.connectedPeers)
        {
            try
            {Logger.print(Tag.DUMP, peer.dumpState());
            }
            catch(Exception e)
            {
                System.err.println(e);
                e.printStackTrace(System.err);
            }
        }
	Logger.print(Tag.DUMP, "Finished dumping entire state");
	System.out.println("Finished Dumping State");
	Logger.FlushDebug();
        Logger.print(Tag.EXECUTOR, "Re-evaluating optimitically unchoked. Currently: " + (this.optimisticallyUnchokedPeer != null ? this.optimisticallyUnchokedPeer.getConnectionState().getRemotePeerId() : "null"));
	List<PeerConnectionManager> choked = this.connectedPeers.stream()
                .filter(peer -> peer.getConnectionState().isLocalChoked() && peer.getConnectionState().isInterested()).collect(Collectors.toList());
        if (choked.size() == 0)
        {
            Logger.print(Tag.EXECUTOR,
                    "All interested peers are already unchoked, there is no optimistically unchoked peer");
            return;
        }
        PeerConnectionManager newOptomisticallyUnchoked = choked.get(random.nextInt(choked.size()));

        Logger.print(Tag.EXECUTOR, "Re-evaluated optimistically unchoked remote peer. Unchoking peer " +
                newOptomisticallyUnchoked.getConnectionState().getRemotePeerId());
        if(this.optimisticallyUnchokedPeer != newOptomisticallyUnchoked)
        {
	    if(this.optimisticallyUnchokedPeer != null)
	    {
	        this.optimisticallyUnchokedPeer.SendControlMessage(new ChokeThreadIntMes());
	    }
            newOptomisticallyUnchoked.SendControlMessage(new UnchokeThreadIntMes());
            this.optimisticallyUnchokedPeer = newOptomisticallyUnchoked;
        }
        else
        {
            Logger.print(Tag.EXECUTOR, "Picked the same optimistically unchoked neighbor");
        }

        this.logger.log("Peer " + this.localPeerId + " has the optimistically unchoked neighbor " +
                this.optimisticallyUnchokedPeer.getConnectionState().getRemotePeerId() + ".");

        
    }

    /**
     * Checks if all pieces were transferred successfully
     *
     * @return   Whether the local peer has all file pieces
     */
    private boolean completedTransfer() {
        // once this returns true once, it will always return true, so we just save the value to avoid recomputing it repeatedly
        if (localFileCompleted)
        {
            return true;
        }
        for (Piece piece : this.localPieces) {
            if (piece == null || piece.getStatus() != PieceStatus.HAVE) {
                return false;
            }
        }
        localFileCompleted = true;
        return true;
    }

    /**
     * Dumps all the file pieces into a local file by looping over every piece and writing its content
     * through a byte buffer
     */
    private void dumpFile() {
        String filePath = String.format(DIRECTORY, this.localPeerId) + File.separator + this.config.getFileName();

        File file = new File((new File(filePath)).getAbsolutePath());

        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (!file.exists()) {
                boolean created = file.createNewFile();
            }

            // Write each byte array to the file
            for (Piece piece : this.localPieces) {
                fos.write(piece.getContent());
            }

            Logger.print(Tag.LOCAL_PEER_MANAGER, "Dumped all content into the file");

            this.logger.log("Peer " + this.localPeerId + " has downloaded the complete file.");
        } catch (IOException e) {
            System.err.println("An error occurred when trying to dump the content into the file");
        }
    }

    // used by PeerConnectionManger Threads to send messages to this localpeermanager
    public void SendControlMessage(InternalMessage message) {
        try {
            this.incomingControlMessages.put(message);
        } catch (InterruptedException e) {
            System.err.println("An error occured while trying to send a control message to LocalPeerManager");
            throw new RuntimeException(e);
        }
    }

    // this is run once when all connections have been established, it just prevents the 
    // for check all peers having the complete file from succeding before all the connections are set up 
    public void SetAllPeersConnected()
    {
        allPeersConnected.set(true);
    }

    private boolean AllTransfersCompleted() {
        if(!completedTransfer())
        {
            return false;
        }
        if (!allPeersConnected.get())
        {
            return false;
        }
        for(PeerConnectionManager peer : this.connectedPeers)
        {
            if (!peer.getConnectionState().isRemoteComplete()) {
                return false;
            }
        }

        return true;
    }

    public void run() {
        try {
            while (!AllTransfersCompleted()) {
                // use poll instead of take, so that we get the timeouts
                // this ensures we check if completion, even if we arent getting that many messages
                // ie in the process for a peer that starts with the file
                InternalMessage message = incomingControlMessages.poll(5, TimeUnit.SECONDS);
                if (message == null)
                {
                    continue;
                }
                HandleControlMessage(message);
            }
        } catch (InterruptedException | UnsupportedOperationException e) {
            System.err.println("An error occured in localPeerManager");
            e.printStackTrace(System.err);
            throw new RuntimeException(e);

        } finally {
            // terminate all peer connections
            Logger.print(Tag.EXITING, "Finished all transfers, exiting");
            announce(new TerminateIntMes());
            dumpFile();
            executor.shutdown();
        }
    }

    private void HandleControlMessage(InternalMessage message) throws UnsupportedOperationException {
        Logger.print(Tag.LOCAL_PEER_MANAGER, "Local Peer Manager is processing control message of type " + message.getTypeString());
        switch (message.getType()) {
            case RECEIVED:
                ReceivedIntMes recMessage = (ReceivedIntMes) message;
                if (recMessage.GetPieceContent() == null) {
                    System.err.println(
                            "LocalPeerManager recieved a recieved control message with no contents which isn't allowed");
                    throw new UnsupportedOperationException("'Recieved' control message with no contents");
                }
                setLocalPiece(recMessage.GetPieceIndex(), PieceStatus.HAVE, recMessage.GetPieceContent());
                break;
            default:
                System.err.println("LocalPeerManager recieved a control message of type " + message.getTypeString()
                        + " which isn't allowed");
                throw new UnsupportedOperationException(
                        "Control Message of invalid type " + message.getTypeString() + " in LocalPeerManager");
        }
    }
}
