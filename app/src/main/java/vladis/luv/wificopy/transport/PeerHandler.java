package vladis.luv.wificopy.transport;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class PeerHandler implements Callable {

    private final Socket socket;
    private final ArrayList<FileInfo> localFiles;

    public PeerHandler(Socket socket, ArrayList<FileInfo> localFiles) throws IOException{
        if(socket != null && localFiles != null) {
            this.socket = socket;
            this.localFiles = localFiles;
        }else
            throw new IOException("in PeerHandler either socket or fileList is null");
    }

    @Override
    public Boolean call() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(localFiles);
            out.flush();
            //out.close();             closeable!!!
            //ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            //FileInfo requestedFile = (FileInfo) in.readObject();
            //System.out.println("server said: requested " + requestedFile.getFileName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
/*
    private void copyFile(String localFile) {
        System.out.println("copying file from server " + localFile);

        File inFile = new File(inDir, file);
        File outFile = new File(outBoxDir, file);
        try (
            InputStream in = new BufferedInputStream(new FileInputStream(outFile)); 
            OutputStream out = new BufferedOutputStream(new FileOutputStream(inFile))) {

            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

    } */
}
