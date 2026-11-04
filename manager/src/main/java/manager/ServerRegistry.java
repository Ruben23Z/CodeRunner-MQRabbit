package manager;

import java.util.ArrayList;
import java.util.List;


public class ServerRegistry {
    private final List<Endereco> servidores = new ArrayList<>();
    private int proximo = 0;

    public synchronized boolean add(String ip, int porto) {
        var endereco = new Endereco(ip, porto);
        if (servidores.contains(endereco)) return false;
        servidores.add(endereco);
        return true;
    }

    /**
     * Round-robin; null se não houver servidores.
     */
//    next() devolve os servidores por ordem, um de cada vez, voltando ao primeiro depois do último.
    public synchronized Endereco next() {
        if (servidores.isEmpty()) return null;
        var e = servidores.get(proximo % servidores.size());
        proximo = (proximo + 1) % servidores.size();
        return e;
    }

    public synchronized int size() {
        return servidores.size();
    }
}