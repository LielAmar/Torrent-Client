package message.type;

import message.Message;
import message.MessageType;

public class ChokeMessage extends Message {

    public ChokeMessage() {
        super(MessageType.CHOKE);

//        buildMessage();
    }

    @Override
    public void buildMessage(byte[] array) {

    }
}
