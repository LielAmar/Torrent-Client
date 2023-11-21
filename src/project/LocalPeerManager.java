package project;

import project.connection.ConnectionState;
import project.connection.PeerConnectionManager;
import project.connection.piece.Piece;
import project.connection.piece.PieceStatus;
import project.utils.Logger;
import project.utils.Tag;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class LocalPeerManager {

    private static final Random random = new Random();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final int localPeerId;

    private final Configuration config;

    private final Piece[] localPieces;
    private final Lock choosePieceLock;
    private final ArrayList<PeerConnectionManager> connectedPeers;

    /*
     * I noticed some potential problems in how the bitmap packet is generated and sent,
     * and how the have packets are communicated internally between threads.
     * EX: Communication with 2 peers, A and B
     * IF the connection with A is set up well before the connection to B, it is possible that we are exchanging data
     * with peer A before we have sent/recieved the handshake with B. If this happened, A might recieve a piece and
     * when anouncing it, adds the have message to connection B's queue before the handshake.
     * This would mean that the handshake isn't the first thing sent to B and the connection would fail.
     * Also, if B has already gotten the bitmap from the LocalPeerManager
     * but hasn't yet enabled listining to other connections "have" messages, then it could end up with
     * Peer B having a incorrect view of what pieces we have.
     *
     * Solution: this ReadWriteLock 'bitMapLock'
     * When a connetion is preparing to send its bitmap, we want to stop all other threads from submiting
     * the pieces they have recieved until the new connection is in a stable state.
     * We are actually somewhat misusing this lock.
     * When a thread recieves a piece it aquires the reader lock before marking it as had in the LocalPeerManager
     * and anouncing the change in state to other threads.
     * The connection reading to generate its bitmap to send actually aquires the writer lock,
     * so that no other threads can mark and piece as had until this thread is done setting up its bitmap.
     *
     */
    private final ReadWriteLock bitmapLock;

    private PeerConnectionManager optimisticallyUnchokedPeer;

    public LocalPeerManager(int localPeerId, Configuration config) {
        this.localPeerId = localPeerId;

        this.config = config;

        this.localPieces = new Piece[this.config.getNumberOfPieces()];

        this.connectedPeers = new ArrayList<>();

        this.bitmapLock = new ReentrantReadWriteLock();
        this.choosePieceLock = new ReentrantLock();

        this.optimisticallyUnchokedPeer = null;

        // Start the scheduler for reevaluating unchoked peers & optimistic unchoked peer
        executor.scheduleAtFixedRate(this::reevaluateUnchokedPeers, 0,
                this.config.getUnchokingInterval(), TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::reevaluateOptimisticPeer, 0,
                this.config.getOptimisticUnchokingInterval(), TimeUnit.SECONDS);
    }


    public int getLocalPeerId() {
        return this.localPeerId;
    }

    public Configuration getConfig() {
        return this.config;
    }


    public Piece[] getLocalPieces() {
        return this.localPieces;
    }

    public void setLocalPiece(int pieceId, PieceStatus status, byte[] content) {
        Logger.print(Tag.LOCAL_PEER_MANAGER, "Updating piece " + pieceId + " status to " + status.name());

        this.bitmapLock.readLock().lock();

        // If the local piece with the given id does not exist, create it. Otherwise, update the existing piece
        if(this.localPieces[pieceId] == null) {
            this.localPieces[pieceId] = new Piece(status, content);
        } else {
            this.localPieces[pieceId].setStatus(status);
            this.localPieces[pieceId].setContent(content);
        }

        // If the piece status is set to HAVE and announce is true, send an announcement to all connections
        if(status == PieceStatus.HAVE) {
            announce(pieceId);
        }

        // If all pieces were transferred, dump all pieces into the file
        if(this.completedTransfer()) {
            Logger.print(Tag.LOCAL_PEER_MANAGER, "Dumped file with piece: " + pieceId);

            this.dumpFile();
        }

        this.bitmapLock.readLock().unlock();
    }

    private void announce(int pieceIndex) {
        // Create a copy to avoid modifying a list we're looping over
        List<PeerConnectionManager> connectedPeersCopy = new ArrayList<>(this.connectedPeers);

        for (PeerConnectionManager peerConnection : connectedPeersCopy) {
            peerConnection.getHandler().sendHave(pieceIndex);
        }
    }


    public void acquireBitmapLock() {
        this.bitmapLock.writeLock().lock();
    }

    public void releaseBitmapLock() {
        this.bitmapLock.writeLock().unlock();
    }


    public PeerConnectionManager connectToPeer(Socket socket) {
        return this.connectToPeer(-1, socket);
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
            for(int i = 0; i < this.config.getNumberOfPieces(); i++) {
                this.localPieces[i] = new Piece(PieceStatus.NOT_HAVE, null);
            }

            return;
        }

        // Otherwise, read the target file and set all local pieces to HAVE with the matching content
        byte[] localFileBytes = this.getLocalFileBytes();

        for(int i = 0; i < this.config.getNumberOfPieces(); i++) {
            // The piece size is either the specified piece size, or smaller if it's an incomplete piece
            int pieceSize = Math.min(this.config.getPieceSize(), this.config.getFileSize() - i * this.config.getPieceSize());
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
        String filePath = "peer_" + this.localPeerId + File.separator + this.config.getFileName();
        File file = new File((new File(filePath)).getAbsolutePath());

        byte[] data = new byte[(int) file.length()];

        try(FileInputStream fis = new FileInputStream(file)) {
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
        // TODO: change this lock to write lock/different lock because 2 connections might choose the same piece
        // this.bitmapLock.readLock().lock();
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

        // this.bitmapLock.readLock().unlock();
        this.choosePieceLock.unlock();

        return randomIndex;
    }


    /**
     * Re-evaluates the unchoked remote peers based on the download speed with each peer.
     * After re-evaluation, the download speed of all peers is being reset.
     */
    private void reevaluateUnchokedPeers() {
        // Go over all connected peers, sort them based on the download speed, filter based on interest and
        // keep the Configuration#getNumberOfPreferredNeighbors first peers. These are the peers to be unchoke.
        List<PeerConnectionManager> unchoked = this.connectedPeers.stream()
                .sorted((pA, pB) -> {
                    if(pA.getConnectionState().getDownloadSpeed() < pB.getConnectionState().getDownloadSpeed()) {
                        return 1;
                    } else if(pA.getConnectionState().getDownloadSpeed() > pB.getConnectionState().getDownloadSpeed()) {
                        return -1;
                    } else {
                        return random.nextInt(2) == 0 ? 1 : -1;
                    }
                })
                .filter(peer -> peer.getConnectionState().isInterested())
                .limit(this.config.getNumberOfPreferredNeighbors())
                .toList();

        // The rest of the connected peers (except for the optimistically unchoke peer) are to be choked.
        List<PeerConnectionManager> choked = this.connectedPeers.stream()
                .filter(peer -> peer != this.optimisticallyUnchokedPeer && !unchoked.contains(peer))
                .toList();

        Logger.print(Tag.EXECUTOR, "Re-evaluated unchoked remote peers. Unchoking peers: " +
                unchoked.stream().map(peer -> peer.getConnectionState().getRemotePeerId() + "")
                        .collect(Collectors.joining(",")));

        unchoked.forEach(peer -> {
            if(peer.getConnectionState().isLocalChoked()) {
                peer.getConnectionState().setLocalChoked(false);
                peer.getHandler().sendUnchoke();
            }
        });

        Logger.print(Tag.EXECUTOR, "Re-evaluated choked remote peers. Choking peers: " +
                choked.stream().map(peer -> peer.getConnectionState().getRemotePeerId() + "")
                        .collect(Collectors.joining(",")));

        choked.forEach(peer -> {
            if(!peer.getConnectionState().isLocalChoked()) {
                peer.getConnectionState().setLocalChoked(true);
                peer.getHandler().sendChoke();
            }
        });

        this.connectedPeers.forEach(peer -> peer.getConnectionState().resetDownloadSpeed());
    }

    /**
     * Re-evaluates the optimistically unchoked remote peer randomly.
     */
    private void reevaluateOptimisticPeer() {
        List<PeerConnectionManager> choked = this.connectedPeers.stream()
                .filter(peer -> !peer.getConnectionState().isLocalChoked() && peer.getConnectionState().isInterested())
                .toList();

        this.optimisticallyUnchokedPeer = choked.get(random.nextInt(choked.size()));

        Logger.print(Tag.EXECUTOR, "Re-evaluated optimistically unchoked remote peer. Unchoking peer " +
                this.optimisticallyUnchokedPeer.getConnectionState().getRemotePeerId());

        if(this.optimisticallyUnchokedPeer.getConnectionState().isLocalChoked()) {
            this.optimisticallyUnchokedPeer.getConnectionState().setLocalChoked(false);
            this.optimisticallyUnchokedPeer.getHandler().sendUnchoke();
        }
    }


    /**
     * Checks if all pieces were transferred successfully
     *
     * @return   Whether the local peer has all file pieces
     */
    private boolean completedTransfer() {
        for(Piece piece : this.localPieces) {
            if(piece == null || piece.getStatus() != PieceStatus.HAVE) {
                return false;
            }
        }

        return true;
    }

    /**
     * Dumps all the file pieces into a local file by looping over every piece and writing its content
     * through a byte buffer
     */
    private void dumpFile() {
        String filePath =  "peer_" + this.localPeerId + File.separator + this.config.getFileName();

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
        } catch (IOException e) {
            System.err.println("An error occurred when trying to dump the content into the file");
        }
    }


    public void attemptTerminate() {
        for(int i = 0; i < this.getLocalPieces().length; i++) {
            if(this.getLocalPieces()[i].getStatus() == PieceStatus.NOT_HAVE) {
                return;
            }
        }

        List<PeerConnectionManager> connectedPeersCopy = new ArrayList<>(this.connectedPeers);

        connectedPeersCopy.stream()
            .filter(peerConnectionManager -> {
                if(peerConnectionManager == null) {
                    return false;
                }
                
                for(int i = 0; i < peerConnectionManager.getConnectionState().getPieces().length; i++) {
                    if(peerConnectionManager.getConnectionState().getPieces()[i] == PieceStatus.NOT_HAVE) {
                        return false;
                    }
                }

                return true;
            })
            .forEach(this::terminateConnection);
    }

    /**
     * Attempts to close the connection between the local peer and the given remote peer if both have all pieces.
     *
     * @param peerConnectionManager   Remote peer
     */
    public void attemptTerminate(PeerConnectionManager peerConnectionManager) {
        for(int i = 0; i < peerConnectionManager.getConnectionState().getPieces().length; i++) {
            if(peerConnectionManager.getConnectionState().getPieces()[i] == PieceStatus.NOT_HAVE) {
                return;
            }
        }

        for(int i = 0; i < this.getLocalPieces().length; i++) {
            if(this.getLocalPieces()[i].getStatus() == PieceStatus.NOT_HAVE) {
                return;
            }
        }

        this.terminateConnection(peerConnectionManager);
    }

    private void terminateConnection(PeerConnectionManager peerConnectionManager) {
        peerConnectionManager.terminate();

        this.connectedPeers.remove(peerConnectionManager);

        if(this.connectedPeers.isEmpty()) {
            executor.close();

            try {
                Thread.sleep(10000);
            } catch(InterruptedException exception) {
                exception.printStackTrace();
            }

            // System.exit(0);
        }
    }
}
