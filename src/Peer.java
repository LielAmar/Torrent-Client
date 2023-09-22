import java.util.BitSet;

public class Peer {

    private int peerId;

    private boolean choked;
    private BitSet bitfield;

    public Peer(int peerId) {
        this.peerId = peerId;

        this.choked = false;
        this.bitfield = null;
    }
    
    public int getPeerId() {
        return this.peerId;
    }

    public void setPeerId(int peerId)
    {
        this.peerId = peerId;
    }
}
