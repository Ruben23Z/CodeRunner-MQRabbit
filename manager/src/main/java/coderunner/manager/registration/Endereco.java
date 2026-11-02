package coderunner.manager.registration;

import java.util.Objects;

public class Endereco {
    private final String ip;
    private final int porto;

    public Endereco(String ip, int porto) {
        this.ip = ip;
        this.porto = porto;
    }

    public String ip() {
        return ip;
    }

    public int porto() {
        return porto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Endereco other)) return false;
        return porto == other.porto && ip.equals(other.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, porto);
    }

    @Override
    public String toString() {
        return ip + ":" + porto;
    }
}