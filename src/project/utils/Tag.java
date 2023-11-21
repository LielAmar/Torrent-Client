package project.utils;

public enum Tag {

    CONFIGURATION("CONFIGURATION", true),
    CONNECTIONS("CONNECTIONS", true),
    CLIENT("CLIENT", true),
    SERVER("SERVER", true),
    LOCAL_PEER_MANAGER("LOCAL_PEER_MANAGER", true),
    EXECUTOR("EXECUTOR", true),

    PEER_CONNECTION_MANAGER("PEER_CONNECTION_MANAGER", true),
    HANDLER("HANDLER", true),
    LISTENER("LISTENER", true),
    SENDER("SENDER", true);


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
