package project.utils;

public enum Tag {
    DEBUG("DEBUG", false),
    DUMP("DUMP", false),
    PACKET("PACKET", false),
    REQUESTS("REQUESTS", false),
    EXITING("EXITING", true),
    CONFIGURATION("CONFIGURATION", true),
    CONNECTIONS("CONNECTIONS", true),
    CLIENT("CLIENT", true),
    SERVER("SERVER", true),
    LOCAL_PEER_MANAGER("LOCAL_PEER_MANAGER", false),
    EXECUTOR("EXECUTOR", false),

    PEER_CONNECTION_MANAGER("PEER_CONNECTION_MANAGER", false),
    HANDLER("HANDLER", true),
    LISTENER("LISTENER", false),
    SENDER("SENDER", false);


    private String tag;
    private boolean logsEnabled;

    Tag(String tag, boolean logsEnabled) {
        this.tag = tag;
        this.logsEnabled = logsEnabled;
    }

    public boolean isLogsEnabled() {
        return this.logsEnabled;
    }

    @Override
    public String toString() {
        return this.name().toUpperCase();
    }
}
