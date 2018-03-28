package filesystem;

import java.util.concurrent.ConcurrentHashMap;

public class Database {
    private ConcurrentHashMap<String, FileInfo> restorableFiles; // pathname, fileinfo
    private ConcurrentHashMap<String, ChunkInfo> chunksBackedUp;// chunkid (fileID/ChunkNo), chunkinfo

    // TODO _inner_ String to Int, and _inner_ ConcurrentHashMap to ConcurrentSkipListMap
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Chunk>> chunksRestored;
    // fileID(sha256) -> (ChunkNo -> Chunk)


    public Database() {
        restorableFiles = new ConcurrentHashMap<>();
        chunksBackedUp = new ConcurrentHashMap<>();
        chunksRestored = new ConcurrentHashMap<>();
        initializeDatabase();
    }

    private void initializeDatabase() {
        //TODO: Load from metadata from files the previous backups and chunks
        //maybe usar json
    }

    private void saveDatabase() {
        //TODO: Save metadata to files
        //TODO: or serialize self?
    }


    /*
        Restore Initiator functions
     */
    public void setFlagRestored(boolean flag, String fileID) {
        if (flag) {
            chunksRestored.put(fileID, new ConcurrentHashMap<>());
        } else {
            chunksRestored.remove(fileID);
        }
    }

    public boolean getFlagRestored(String fileID) {
        return chunksRestored.containsKey(fileID);
    }

    public void addChunksRestored(Chunk chunk) {
        if (chunksRestored.get(chunk.getFileID()).containsKey(Integer.toString(chunk.getChunkNo()))) {
            System.out.println("Chunk already exist");
        } else {
            System.out.println("Adding chunk to merge");
            chunksRestored.get(chunk.getFileID()).put(Integer.toString(chunk.getChunkNo()), chunk);
        }

    }

    public Integer getChunksRestoredSize(String fileID) {
        return chunksRestored.get(fileID).size();
    }

    public ConcurrentHashMap<String, Chunk> getChunksToRestore(String fileID) {
        return chunksRestored.get(fileID);
    }

    public boolean hasRestoreFinished(String pathName, String fileID) {
        int numChunks = restorableFiles.get(pathName).getNumChunks();
        int chunksRestored = getChunksRestoredSize(fileID);

        return numChunks == chunksRestored;
    }

    /*
     *
     */
    //Backup
    public void addRestorableFile(String pathName, FileInfo fileInfo) {
        if (!hasChunk(pathName)) {
            restorableFiles.put(pathName, fileInfo);

            saveDatabase();
        }
    }

    //delete
    public void removeRestorableFile(String pathName) {
        restorableFiles.remove(pathName);

        saveDatabase();
    }

    public boolean hasFile(String pathName) {
        return restorableFiles.containsKey(pathName);
    }

    public FileInfo getFileInfo(String pathName) {
        return restorableFiles.get(pathName);
    }

    /*
     *
     */
    public boolean hasChunk(String chunkID) {
        return chunksBackedUp.containsKey(chunkID);
    }

    public void addChunk(String chunkID, ChunkInfo chunkInfo) {
        if (!hasChunk(chunkID)) {
            chunksBackedUp.put(chunkID, chunkInfo);

            saveDatabase();
        }
    }

    public void removeChunk(String chunkID) {
        chunksBackedUp.remove(chunkID);

        saveDatabase();
    }

    public void addChunkMirror(String chunkID, String peerID) {
        if (hasChunk(chunkID)) {
            if (!chunksBackedUp.get(chunkID).getMirrors().contains(peerID)) {
                chunksBackedUp.get(chunkID).getMirrors().add(peerID);

                saveDatabase();
            }
        }
    }

    public void removeChunkMirror(String chunkID, String peerID) {
        chunksBackedUp.get(chunkID).removeMirror(peerID);
    }

    public int getChunkReplicationDegree(String chunkID) {
        return chunksBackedUp.get(chunkID).getReplicationDegree();
    }

    public int getChunkMirrorsSize(String chunkID) {
        return chunksBackedUp.get(chunkID).getMirrors().size();
    }

}
