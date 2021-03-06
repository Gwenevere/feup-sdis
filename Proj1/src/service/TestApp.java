package service;

import utils.Log;

import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

import static utils.Utils.getRegistry;
import static utils.Utils.parseRMI;

public class TestApp implements Runnable {

    private String[] peer_ap;
    private String sub_protocol;
    private String opnd_1;
    private String opnd_2;

    private Map<String, Runnable> handlers;
    private RemoteBackupService stub;

    public TestApp(String[] peer_ap, String sub_protocol, String opnd_1, String opnd_2) {
        this.peer_ap = peer_ap;
        this.sub_protocol = sub_protocol;
        this.opnd_1 = opnd_1;
        this.opnd_2 = opnd_2;

        handlers = new HashMap<>();
        handlers.put("BACKUP", this::handleBackup);
        handlers.put("RESTORE", this::handleRestore);
        handlers.put("DELETE", this::handleDelete);
        handlers.put("RECLAIM", this::handleReclaim);
        handlers.put("STATE", this::handleState);

    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) {
            Log.logWarning("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        //host/name or   //host:port/name
        String[] peer_ap = parseRMI(false, args[0]);
        if (peer_ap == null) {
            return;
        }

        String sub_protocol = args[1];
        String operand1 = args.length > 2 ? args[2] : null;
        String operand2 = args.length > 3 ? args[3] : null;

        TestApp app = new TestApp(peer_ap, sub_protocol, operand1, operand2);
        new Thread(app).start();
    }

    @Override
    public void run() {
        initiateRMIStub();
        handlers.get(sub_protocol).run();
    }

    private void initiateRMIStub() {
        try {
            Registry registry = getRegistry(peer_ap);
            stub = (RemoteBackupService) registry.lookup(peer_ap[2]);
        } catch (Exception e) {
            Log.logError("Error when opening RMI stub");
            e.printStackTrace();
        }
    }

    private void handleBackup() {
        Log.log("BACKING UP file at \"" + Paths.get(this.opnd_1) + "\"");

        try {
            stub.backup(this.opnd_1, Integer.parseInt(this.opnd_2));
        } catch (RemoteException e) {
            Log.logError("Client exception: " + e.toString());
        }
    }

    private void handleDelete() {
        Log.log("DELETING file \"" + opnd_1 + "\"");

        try {
            stub.delete(this.opnd_1);
        } catch (RemoteException e) {
            Log.logError("Client exception: " + e.toString());
        }
    }

    private void handleRestore() {
        Log.log("RESTORING file \"" + opnd_1 + "\"");

        try {
            stub.restore(opnd_1);
        } catch (RemoteException e) {
            Log.logError("Client exception: " + e.toString());
        }
    }

    private void handleReclaim() {
        Log.log("RECLAIMING disk space: \"" + opnd_1 + "\"");

        try {
            stub.reclaim(Integer.parseInt(opnd_1));
        } catch (RemoteException e) {
            Log.logError("Client exception: " + e.toString());
        }
    }

    private void handleState() {
        Log.log("This is my state :D");
        try {
            stub.state();
        } catch (RemoteException e) {
            Log.logError("Client exception: " + e.toString());
        }
    }
}
