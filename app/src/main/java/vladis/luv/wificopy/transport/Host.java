package vladis.luv.wificopy.transport;

import java.net.InetAddress;
import java.util.Objects;

public class Host {
    private InetAddress ip;
    private String hostname;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.ip);
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
        final Host other = (Host) obj;
        return Objects.equals(this.ip, other.ip);
    }

    public InetAddress getIp() {
        return ip;
    }

    public String getHostname() {
        return hostname;
    }


    public Host(InetAddress ip, String hostname){
        this.ip = ip;
        this.hostname = hostname;
    }
}