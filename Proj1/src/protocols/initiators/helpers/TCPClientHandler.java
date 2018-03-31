package protocols.initiators.helpers;

import filesystem.Chunk;
import network.Message;
import protocols.PeerData;
import service.Peer;
import utils.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class TCPClientHandler implements Runnable {
    private Peer parentPeer;
    private Socket clientSocket;

    public TCPClientHandler(Peer parentPeer, Socket clientSocket) {
        this.parentPeer = parentPeer;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        //Receive the CHUNK
        Message msg = null;

        try {
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            msg = (Message) ois.readObject();
            ois.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println(msg.toString());

        if (msg == null) {
            Log.logError("Invalid CHUNK from TCP. Aborting!");
            return;

        }
        //Handle the CHUNK
        PeerData peerData = parentPeer.getPeerData();

        if (!peerData.getFlagRestored(msg.getFileID())) { // Restoring File ?
            Log.logWarning("Discarded Chunk");
            return;
        }

        peerData.addChunkToRestore(new Chunk(msg.getFileID(), msg.getChunkNo(), msg.getBody()));
    }
}