import java.util.BitSet;

public class Peer {

    private final int peerId;


    private boolean choked;
    private BitSet bitfield;

    public Peer(int peerId) {
        this.peerId = peerId;


        this.choked = false;
        this.bitfield = null;
    }
}
