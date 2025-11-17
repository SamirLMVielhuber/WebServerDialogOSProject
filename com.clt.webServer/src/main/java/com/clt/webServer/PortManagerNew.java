package com.clt.webServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.clt.Config;

public class PortManagerNew {
    private static final PortManagerNew INSTANCE = new PortManagerNew();

    private static final int BASE_PORT = 9000;
    private static final int MAX_PORT = 10000;

    private static final int MAX_USERS_PER_PORT = Config.PERPORTCAPACITY();

    private final ConcurrentHashMap<Integer, Group> portGroups = new ConcurrentHashMap<>();

    private PortManagerNew() {}

    public static PortManagerNew getInstance() {
        return INSTANCE;
    }

    public synchronized int assignPort(String userId, int requestedPort) {
        if (requestedPort > 0 && requestedPort >= BASE_PORT && requestedPort < MAX_PORT) {
            //Use Requested port if is valid
            Group g = this.portGroups.get(requestedPort);

            if (g != null) {
                if (!g.isFull()) {
                    g.addUser(userId);
                    System.out.println("PortManager: Added " + userId + " to port group " + requestedPort);
                    System.out.flush();
                    return requestedPort;
                } else {
                    System.out.println("PortManager: Requested port " + requestedPort + " is full.");
                    System.out.flush();
                }
            }
            else {
                if (isPortAvailable(requestedPort)) {
                    Group newGroup = new Group(PortManagerNew.MAX_USERS_PER_PORT);
                    newGroup.addUser(userId);
                    this.portGroups.put(requestedPort, newGroup);

                    System.out.println("PortManager: Created new group on port " + requestedPort);
                    System.out.flush();
                    return requestedPort;
                }
                else {
                    System.out.println("PortManager: Port " + requestedPort + " not available for a new group.");
                    System.out.flush();
                }
            }
        }

        for (Map.Entry<Integer, Group> entry : this.portGroups.entrySet()) {
            //Check, if ther is an already not full existing group
            Group g = entry.getValue();
            if (!g.isFull()) {
                g.addUser(userId);
                System.out.println("PortManager: Assigned " + userId + " to existing group on " + entry.getKey());
                System.out.flush();
                return entry.getKey();
            }
        }

        for (int port = BASE_PORT; port < MAX_PORT; port++) {
            //If nothing works, create a new Group with first possible port
            if (!this.portGroups.containsKey(port) && isPortAvailable(port)) {
                Group g = new Group(MAX_USERS_PER_PORT);
                g.addUser(userId);
                this.portGroups.put(port, g);

                System.out.println("PortManager: Created new group on new port " + port);
                System.out.flush();
                return port;
            }
        }
        throw new RuntimeException("No available ports or group capacity!");
    }

    public synchronized void releasePorts(String userId) {
        for (Map.Entry<Integer, Group> entry : this.portGroups.entrySet()) {
            int port = entry.getKey();
            Group group = entry.getValue();
            group.removeUser(userId);

            if (group.isEmpty()) {
                this.portGroups.remove(port);
                System.out.println("PortManager: Removed empty group on port " + port);
            }
        }
        System.out.println("Released ports for " + userId);
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Set<String> getUsersForPort(int port) {
        Group g = this.portGroups.get(port);
        return g == null ? Collections.emptySet() : g.snapshotUsers();
    }
        private class Group{
        private final int MAX_USERS_PER_PORT;
        private int current_users;
        private final Set<String> users = Collections.newSetFromMap(new ConcurrentHashMap<>());;

        public Group(int maxUsers){
            this.MAX_USERS_PER_PORT = maxUsers;
        }

        public synchronized boolean addUser(String userId){
            if(this.isFull())
                return false;
            this.current_users++;
            this.users.add(userId);
            return true;
        }

        public boolean isFull(){
            return this.current_users >= this.MAX_USERS_PER_PORT;
        }
        
        public synchronized void removeUser(String userId) {
            if (users.remove(userId))
                this.current_users--;
        }
        
        public synchronized Set<String> snapshotUsers() {
            return new HashSet<>(users);
        }

        public synchronized boolean isEmpty() {
            return this.current_users == 0;
        }
    }
}