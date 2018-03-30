package protocols.initiators;

import channels.Channel;
import filesystem.Database;
import filesystem.FileInfo;
import network.Message;
import service.Peer;
import utils.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DeleteInitiator implements Runnable {
    private String version;
    private String pathName;
    private Peer parentPeer;

    public DeleteInitiator(String version, String pathName, Peer parentPeer) {
        this.version = version;
        this.pathName = pathName;
        this.parentPeer = parentPeer;

        Log.logWarning("Starting deleteInitiator!");
    }

    @Override
    public void run() {
        Database database = parentPeer.getDatabase();
        //Obtain info of the file from Database
        FileInfo fileInfo = database.getFileInfoByPath(pathName);
        if (fileInfo == null) {
            Log.logError("File didn't exist! Aborting Delete!");
            return;
        }

        //Send Delete message to MC channel
        sendMessageToMC(fileInfo);

        //Delete the file from fileSystem
        try {
            //TODO: Send delete messages 3/5 times with delay?
            Files.delete(Paths.get(pathName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Delete file from database
        database.removeRestorableFileByPath(pathName);
        Log.logWarning("Finished deleteInitiator!");
    }

    private boolean sendMessageToMC(FileInfo fileInfo) {
        String[] args = {
                version,
                Integer.toString(parentPeer.getID()),
                fileInfo.getFileID()
        };

        Message msg = new Message(Message.MessageType.DELETE, args);

        try {
            parentPeer.sendMessage(Channel.ChannelType.MC, msg);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
