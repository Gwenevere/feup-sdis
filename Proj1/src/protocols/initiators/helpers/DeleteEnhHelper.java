package protocols.initiators.helpers;

import channels.Channel;
import filesystem.Database;
import network.Message;
import service.Peer;

import java.io.IOException;
import java.util.Set;

public class DeleteEnhHelper implements Runnable {

    private final Message request;
    private Peer parentPeer;

    public DeleteEnhHelper(Message request, Peer parentPeer) {
        this.request = request;
        this.parentPeer = parentPeer;
    }

    @Override
    public void run() {
        Database database = parentPeer.getDatabase();

        Set<String> filesToDelete = database.getFilesToDelete(request.getSenderID());

        if(filesToDelete.isEmpty())
            return;

        for(String fileID : filesToDelete){
            sendDELETE(fileID);
        }

    }

    private void sendDELETE(String fileID){
        String[] args = {
                parentPeer.getVersion(),
                Integer.toString(parentPeer.getID()),
                fileID
        };

        Message msg = new Message(Message.MessageType.DELETE, args);

        try {
            parentPeer.sendMessage(Channel.ChannelType.MC, msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
