package vladis.luv.wificopy.transport;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.Callable;

public class DataTransferHandler implements Callable {

    private final Socket socket;

    public DataTransferHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public Boolean call() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream()); 
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())
            ) {
            FileInfo fileInfo = (FileInfo) in.readObject();
            File file = new File(Prefs.outboxFolder, fileInfo.getFileName());
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = fileInputStream.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
            fileInputStream.close();

        } catch (IOException | ClassNotFoundException e) {
            e.getMessage();
        }
        return true;
    }

}
