public class PeerConnection extends Thread {

    private Peer peer;
    private Socket connection;

    public PeerConnection(Socket connection, Peer peer)
    {
        this.connection = connection;
        this.peer = peer;
    }

    public void run()
    {

    }
}
