package vladis.luv.wificopy.transport;

import java.util.Objects;

public class HostS {
    private String ip;
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
        final HostS other = (HostS) obj;
        return Objects.equals(this.ip, other.ip);
    }

    public String getIp() {
        return ip;
    }

    public String getHostname() {
        return hostname;
    }


    public HostS(String ip, String hostname){
        this.ip = ip;
        this.hostname = hostname;
    }
}