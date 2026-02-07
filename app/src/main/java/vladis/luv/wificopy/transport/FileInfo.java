package vladis.luv.wificopy.transport;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


public class FileInfo implements Serializable{
    private static final long serialVersionUID = 28972L;

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.fileName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileInfo other = (FileInfo) obj;
        return Objects.equals(this.fileName, other.fileName);
    }
    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getDateModified() {
        return dateModified;
    }


    private String fileName;
    private long fileSize;
    private long dateModified;
    
    public FileInfo(String fileName, long fileSize, long dateModified){
        this.dateModified = dateModified;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public String getDateModifiedS(){
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateModified), zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.y HH:mm:ss");
        return zdt.format(formatter);
    }

    public String getFileSizeS() {
        float size_f = (float)fileSize;
        if (size_f < 1000) {
            return new String(fileSize + " байт");
        }
        if (size_f < 1000000) {
            return String.format("%.2f Кб", size_f/1024);
        }
        if (size_f < 1000000000) {
            return String.format("%.2f Мб", size_f/(1024*1024));
        }
        if (size_f < 1000000000000.0) {
            return String.format("%.2f Гб", size_f/(1024*1024*1024));
        }
        return new String(fileSize + " байт");
    }
}
