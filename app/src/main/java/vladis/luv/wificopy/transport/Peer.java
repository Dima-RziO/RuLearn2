package vladis.luv.wificopy.transport;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import vladis.luv.wificopy.Messenger;

public class Peer {

    private InetAddress localIp;
    private volatile Set<Host> discoveredHosts;
    ExecutorService executorService;
    ScheduledExecutorService scheduledExecutorService;
    Future<Boolean> dataTransferEnded;
    Future<Boolean> commTransferEnded;
    ServerSocket commInterfaceSocket;
    ServerSocket dataInterfaceSocket;
    DatagramSocket discoverySocket;
    Messenger messenger;
    InetAddress localhost = InetAddress.getLoopbackAddress();

    public Peer(Messenger messenger) { // Edited by DimarZio, Jan 3, 2026
/*
        if (null == (localIp = getLocalIP())) {
            throw new Exception("Включите Wi Fi");
        } else {
            discoveredHosts = new HashSet<>();
            //executorService = new ThreadPoolExecutor(3, 15, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
            executorService = Executors.newFixedThreadPool(5);
            this.messenger = messenger;
        }

 */
        localIp = getLocalIP();

        discoveredHosts = new HashSet<>();
        //executorService = new ThreadPoolExecutor(3, 15, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
        executorService = Executors.newFixedThreadPool(5);
        this.messenger = messenger;
    }

    public void startPeer() {
        startCommInterface(localIp);
        broadcastPresence();
        startDiscovery(localIp);
        startDataTransferInterface(localIp);
    }

    private void startCommInterface(InetAddress ip) {
        // communication server socket
        Runnable command = new Runnable() {
            @Override
            public void run() {
                try {
                    commInterfaceSocket = new ServerSocket(Prefs.communicationPort, 0, ip);
                    sendMessage("comm interface up");
                    while (true) {
                        Socket socket = commInterfaceSocket.accept();
                        commTransferEnded = executorService.submit(new PeerHandler(socket, getFilesInOutbox()));
                    }
                } catch (Exception e) {
                    sendMessage("In comm interface: " + e.getMessage());
                }
            }
        };
        executorService.execute(command);
    }

    private void startDiscovery(InetAddress ip) {
        Runnable discovery = new Runnable() {
            @Override
            public void run() {
                try {
                    discoverySocket = new DatagramSocket(Prefs.discoveryPort);
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    sendMessage("discovery up");
                    while (true) {
                        discoverySocket.receive(packet);
                        InetAddress address = packet.getAddress();
                        // Edit by DimarZio, Jan 3, 2026
                        if (!address.equals(ip) && !address.equals(localhost) && !address.equals(getLocalIP())) {
                            synchronized (discoveredHosts) {
                                String hostname = new String(packet.getData(), packet.getOffset(), packet.getLength());
                                discoveredHosts.add(new Host(address, hostname));
                            }
                        }
                    }
                } catch (Exception e) {
                    sendMessage("In discovery: " + e.getMessage());
                }
            }
        };
        executorService.execute(discovery);
    }

    private void broadcastPresence() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        sendMessage("broadcasting up");
        Runnable broadcast = new Runnable() {
            @Override
            public void run() {
                try (DatagramSocket socket = new DatagramSocket()) {
                    String message = Prefs.hostname;
                    InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, Prefs.discoveryPort);
                    socket.send(packet);
                } catch (Exception e) {
                    sendMessage("In broadcast: " + e.getMessage());
                }
            }
        };
        scheduledExecutorService.scheduleWithFixedDelay(broadcast, 0, 1, TimeUnit.SECONDS);
    }

    //server
    private void startDataTransferInterface(InetAddress ip) {
        // Listen for incoming connections
        Runnable transfer = new Runnable() {
            @Override
            public void run() {
                try {
                    dataInterfaceSocket = new ServerSocket(Prefs.dataPort, 0, ip);
                    sendMessage("data transfer up");
                    while (true) {
                        Socket socket = dataInterfaceSocket.accept();
                        dataTransferEnded = executorService.submit(new DataTransferHandler(socket));
                    }
                } catch (IOException e) {
                    sendMessage("In data interface: " + e.getMessage());
                }
            }
        };
        executorService.execute(transfer);
    }

    public ArrayList<Host> getHosts() {
        return new ArrayList<>(discoveredHosts);
    }

    public void shutDownPeer() {

        Runnable shutdown = new Runnable() {
            @Override
            public void run() {
                try {
                    scheduledExecutorService.shutdownNow();
                    discoverySocket.close();
                    if (commTransferEnded == null) {
                        commInterfaceSocket.close();
                    } else {
                        commTransferEnded.get();
                        commInterfaceSocket.close();
                    }
                    if (dataTransferEnded == null) {
                        dataInterfaceSocket.close();
                    } else {
                        dataTransferEnded.get();
                        dataInterfaceSocket.close();
                    }
                } catch (Exception e) {
                    sendMessage("in shutdown: " + e.getMessage());
                }
                executorService.shutdownNow();
            }
        };
        executorService.execute(shutdown);
        executorService.shutdown();
    }

    public boolean execServiceIsRunning() {
        return !executorService.isTerminated();
    }

    private ArrayList<FileInfo> getFilesInOutbox() throws IOException {
        ArrayList<FileInfo> filesOut = new ArrayList<>();
        File outBoxDir = Prefs.outboxFolder;
        if (!outBoxDir.exists() || !outBoxDir.isDirectory()) {
            throw new IOException("Missing 'out' directory");
        }

        for (File file : outBoxDir.listFiles()) {
            filesOut.add(new FileInfo(file.getName(), file.length(), file.lastModified()));
        }
        return filesOut;
    }

    private static ArrayList<FileInfo> getFilesInInbox() throws IOException {
        ArrayList<FileInfo> filesIn = new ArrayList<>();
        File inBoxDir = Prefs.inboxFolder;
        if (!inBoxDir.exists() || !inBoxDir.isDirectory()) {
            throw new IOException("Missing 'in' directory");
        }

        for (File file : inBoxDir.listFiles()) {
            filesIn.add(new FileInfo(file.getName(), file.length(), file.lastModified()));
        }
        return filesIn;
    }

    //client
    public void getAllFilesFromServer(Host host) {
        InetAddress ip = host.getIp();
        try {
            Socket socket = new Socket(ip, Prefs.communicationPort);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ArrayList<FileInfo> remoteFiles = (ArrayList<FileInfo>) in.readObject();
            sendMessage("Receiving files from address: " + socket.getInetAddress().getHostAddress()); // + ", from port: " + socket.getPort());
            for (FileInfo file : compareWithLocalFiles(remoteFiles, getFilesInInbox())) {
                sendMessage("client requests " + file.getFileName());
                getFileFromServer(host, file);
            }
        } catch (IOException | ClassNotFoundException e) {
            sendMessage("error " + e.getMessage());
        }
    }

    public void getSelectedFilesFromServer(Host host, ArrayList<FileInfo> files) {
        if(files == null || files.isEmpty())
            return;
        InetAddress ip = host.getIp();
        try {
            Socket socket = new Socket(ip, Prefs.communicationPort);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ArrayList<FileInfo> remoteFiles = (ArrayList<FileInfo>) in.readObject();
            sendMessage("Receiving selected files from address: " + socket.getInetAddress().getHostAddress());
            for (FileInfo file : remoteFiles) {
                if (files.contains(file)) {
                    sendMessage("client requests " + file.getFileName());
                    getFileFromServer(host, file);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            sendMessage("error " + e.getMessage());
        }
    }

    public void getSelectedFileFromServer(Host host, FileInfo file, File to) { // By DimarZio Jan 1, 2026
        InetAddress ip = host.getIp();
        try {
            Socket socket = new Socket(ip, Prefs.communicationPort);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ArrayList<FileInfo> remoteFiles = (ArrayList<FileInfo>) in.readObject();
            sendMessage("Receiving selected files from address: " + socket.getInetAddress().getHostAddress());
            for (FileInfo remoteFile : remoteFiles) {
                if (file.getFileName().equals(remoteFile.getFileName())) {
                    sendMessage("client requests " + remoteFile.getFileName());
                    getFileFromServer(host, remoteFile, to);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            sendMessage("error " + e.getMessage());
        }
    }

    private void getFileFromServer(Host host, FileInfo fileInfo) {
        InetAddress ip = host.getIp();
        try (Socket socket = new Socket(ip, Prefs.dataPort); ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); DataInputStream in = new DataInputStream(socket.getInputStream())) {
            out.writeObject(fileInfo);
            File file = new File(Prefs.inboxFolder, fileInfo.getFileName());
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, lengthRead);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            sendMessage("file " + fileInfo.getFileName() + " received.");
        } catch (IOException e) {
            sendMessage("error " + e.getMessage());
        }
    }

    private void getFileFromServer(Host host, FileInfo fileInfo, File to) { // By DimarZio Jan 1, 2026
        InetAddress ip = host.getIp();
        try (Socket socket = new Socket(ip, Prefs.dataPort); ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); DataInputStream in = new DataInputStream(socket.getInputStream())) {
            out.writeObject(fileInfo);
            FileOutputStream fileOutputStream = new FileOutputStream(to);
            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, lengthRead);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            sendMessage("file " + fileInfo.getFileName() + " received.");
        } catch (IOException e) {
            sendMessage("error " + e.getMessage());
        }
    }

    public ArrayList<FileInfo> getFileListFromServer(Host host) {
        InetAddress ip = host.getIp();
        try (Socket socket = new Socket(ip, Prefs.communicationPort)) {
            socket.setSoTimeout(500);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ArrayList<FileInfo> remoteFiles = (ArrayList<FileInfo>) in.readObject();
            sendMessage("Received files list from address: " + socket.getInetAddress().getHostAddress()); // + ", from port: " + socket.getPort());
            //return compareWithLocalFiles(remoteFiles, getFilesInInbox());
            return remoteFiles;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("in getFileListFromServer " + host.getHostname() + " " + e.getMessage());
            removeFromHosts(host);
            return null;
        }
    }

    private void removeFromHosts(Host host) {
        if (discoveredHosts.contains(host)) {
            sendMessage("Host " + host.getHostname() + " is unreachable. Removing...");
            synchronized (discoveredHosts) {
                discoveredHosts.remove(host);
            }
        }
    }

    private ArrayList<FileInfo> compareWithLocalFiles(ArrayList<FileInfo> remoteFiles, ArrayList<FileInfo> localFiles) {
        ArrayList<FileInfo> filesINeed = new ArrayList<>();
        for (FileInfo remoteFile : remoteFiles) {
            int index = localFiles.indexOf(remoteFile);
            if (index != -1) {
                if (localFiles.get(index).getDateModified() < remoteFile.getDateModified()) {
                    filesINeed.add(remoteFile);
                }
            } else {
                filesINeed.add(remoteFile);
            }
        }
        return filesINeed;
    }

    public static boolean doINeedThisFile(FileInfo remoteFile) {
        try {
            ArrayList<FileInfo> localFiles = getFilesInInbox();
            int index = localFiles.indexOf(remoteFile);
            if (index == -1)
                return true;
            else {
                if (localFiles.get(index).getDateModified() < remoteFile.getDateModified())
                    return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }


    private void sendMessage(String message) {
        messenger.sendMessage(message);
    }

    private InetAddress getLocalIP() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

            for (NetworkInterface nif : Collections.list(nets)) {
                for (InterfaceAddress ipaddress : nif.getInterfaceAddresses()) {
                    String strIpAddress = ipaddress.getAddress().getHostAddress();
                    if (strIpAddress.contains("192.168.")) {
                        return ipaddress.getAddress();
                    }
                }
            }
        } catch (SocketException se) {
            sendMessage("cannot create socket");
        }
        return null;
    }
}
