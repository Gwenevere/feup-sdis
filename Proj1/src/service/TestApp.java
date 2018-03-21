package service;

import filesystem.FileManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

    public TestApp() {}

    public static void main(String[] args) {
        if(args.length < 2 || args.length > 4) {
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }
        String peer_ap = args[0]; //name of the remote object
        String sub_protocol = args[1]; //protocol to execute
        String pathname = args[2];
        int replicationDegree = Integer.parseInt(args[3]);
        File file = new File(pathname);


        try {
            Registry registry = LocateRegistry.getRegistry(null);
            IService stub = (IService) registry.lookup(peer_ap);
            String response = stub.backup(file, replicationDegree);
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
