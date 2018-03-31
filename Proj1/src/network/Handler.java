package network;

import filesystem.Chunk;
import filesystem.ChunkInfo;
import filesystem.Database;
import protocols.*;
import protocols.initiators.helpers.RemovedChunkHelper;
import service.Peer;
import utils.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

public class Handler implements Runnable {
    private Peer parentPeer;
    private BlockingQueue<Message> msgQueue;
    private ScheduledExecutorService executor;
    private Map<String, Map<Integer, Future>> backUpHandlers;

    private Random random;

    public Handler(Peer parentPeer) {
        this.parentPeer = parentPeer;
        this.msgQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newScheduledThreadPool(5);

        this.backUpHandlers = new HashMap<>();
        this.random = new Random();
    }

    @Override
    public void run() {
        Message msg;

        while (true) {
            try {
                msg = msgQueue.take();
                dispatchMessage(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatchMessage(Message msg) {
        if (msg == null) {
            Log.logError("Null Message Received");
            return;
        }

        if (msg.getSenderID() == parentPeer.getID())
            return;

        Log.logWarning("R: " + msg.getType() + msg.getChunkNo());
        switch (msg.getType()) {
            case PUTCHUNK:
                handlePUTCHUNK(msg);
                break;
            case STORED:
                handleSTORED(msg);
                break;
            case GETCHUNK:
                Restore restore = new Restore(parentPeer, msg);
                executor.execute(restore);
                break;
            case CHUNK:
                handleCHUNK(msg);
                break;
            case REMOVED:
                handleREMOVED(msg);
                break;
            case DELETE:
                Delete delete = new Delete(parentPeer, msg);
                executor.execute(delete);
                break;
            default:
                return;
        }
    }

    private void handleCHUNK(Message msg) {
        PeerData peerData = parentPeer.getPeerData();

        // Notify restore observers of new message
        peerData.notifyChunkObservers(msg);

        if (!peerData.getFlagRestored(msg.getFileID())) { // Restoring File ?
            Log.logWarning("Discarded Chunk");
            return;
        }

        peerData.addChunkToRestore(new Chunk(msg.getFileID(), msg.getChunkNo(), msg.getBody()));
    }

    private void handlePUTCHUNK(Message msg) {
        if (parentPeer.getDatabase().hasChunk(msg.getFileID(), msg.getChunkNo())) {
            Map<Integer, Future> fileBackUpHandlers = backUpHandlers.get(msg.getFileID());
            if (fileBackUpHandlers == null) return;

            final Future handler = fileBackUpHandlers.get(msg.getChunkNo());
            if (handler == null) return;
            handler.cancel(true);
            Log.log("Stopping chunk back up, due to received PUTCHUNK");
        } else {
            Backup backup = new Backup(parentPeer, msg);
            executor.execute(backup);
        }
    }

    private void handleSTORED(Message msg) {
        Database database = parentPeer.getDatabase();
        if (database.hasChunk(msg.getFileID(), msg.getChunkNo())) {
            database.addChunkMirror(msg.getFileID(), msg.getChunkNo(), msg.getSenderID());
        } else if (database.hasBackedUpFileById(msg.getFileID())) {
            parentPeer.getPeerData().addChunkReplication(msg.getFileID(), msg.getChunkNo());
        }
    }

    private void handleREMOVED(Message msg) {
        Database database = parentPeer.getDatabase();
        String fileID = msg.getFileID();
        int chunkNo = msg.getChunkNo();

        if (database.removeChunkMirror(fileID, chunkNo, msg.getSenderID()) == null) {
            Log.logWarning("Ignoring REMOVED of non-local Chunk");
            return;
        }

        ChunkInfo chunkInfo = database.getChunkInfo(fileID, chunkNo);

        int perceivedReplication = database.getChunkPerceivedReplication(fileID, chunkNo);
        int desiredReplication = chunkInfo.getReplicationDegree();

        if (perceivedReplication < desiredReplication) {
            byte[] chunkData = parentPeer.loadChunk(fileID, chunkNo);

            Future handler = executor.schedule(
                    new RemovedChunkHelper(parentPeer, chunkInfo, chunkData),
                    this.random.nextInt(ProtocolSettings.MAX_DELAY + 1),
                    TimeUnit.MILLISECONDS
            );

            backUpHandlers.putIfAbsent(msg.getFileID(), new HashMap<>());
            backUpHandlers.get(msg.getFileID()).put(msg.getChunkNo(), handler);
        }
    }

    public void pushMessage(byte[] data, int length) {
        Message msgParsed; // create and parse the message
        try {
            msgParsed = new Message(data, length);
        } catch (Exception e) {
            Log.logError(e.getMessage());
            return;
        }
        msgQueue.add(msgParsed);
    }
}