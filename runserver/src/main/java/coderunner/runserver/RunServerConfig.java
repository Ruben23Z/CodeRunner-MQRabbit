package coderunner.runserver;
public class RunServerConfig {

    private final String ip;
    private final int porto;
    private final String managerIp;
    private final int managerPorto;
    private final int workers;
    private final int filaMax;


    public RunServerConfig(String ip, int porto, String managerIp, int managerPorto, int workers, int filaMax) {
        this.ip = ip;
        this.porto = porto;
        this.managerIp = managerIp;
        this.managerPorto = managerPorto;
        this.workers = workers;
        this.filaMax = filaMax;
        check(ip, porto, managerIp, managerPorto, workers, filaMax);
    }


    public String getIp() {
        return ip;
    }

    public int getPorto() {
        return porto;
    }

    public String getManagerIp() {
        return managerIp;
    }

    public int getManagerPorto() {
        return managerPorto;
    }

    public int getWorkers() {
        return workers;
    }

    public int getFilaMax() {
        return filaMax;
    }

    public static void check(String ip, int porto, String managerIp, int managerPorto, int workers, int filaMax) {
        if (ip == null || ip.isBlank()) throw new IllegalArgumentException("ip vazio");
        if (porto < 1 || porto > 65535) throw new IllegalArgumentException("porto fora de 1-65535: " + porto);
        if (managerIp == null || managerIp.isBlank()) throw new IllegalArgumentException("managerIp vazio");
        if (managerPorto < 1 || managerPorto > 65535)
            throw new IllegalArgumentException("managerPorto fora de 1-65535: " + managerPorto);
        if (workers < 1) throw new IllegalArgumentException("workers (N) tem de ser >= 1: " + workers);
        if (filaMax < 1) throw new IllegalArgumentException("filaMax (Q) tem de ser >= 1: " + filaMax);
    }


    public static RunServerConfig fromArgs(String[] args) {
        if (args.length < 4 || args.length > 6) throw new IllegalArgumentException("número de argumentos inválido");

        String ip = args[0];
        int porto = lerInt("porto", args[1]);

        String managerIP = args[2];
        int managerPorto = lerInt("managerPorto", args[3]);
        int workers, fila;
        if (args.length > 4) {
            workers = lerInt("N", args[4]);
        } else {
            workers = 2;

        }
        if (args.length > 5) {

            fila = lerInt("Q", args[5]);
        } else {
            fila = 100;
        }

        return new RunServerConfig(ip, porto, managerIP, managerPorto, workers, fila);
    }

    private static int lerInt(String nome, String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nome + " não é um número: " + valor);
        }
    }

    public String endereco() {
        return ip + ":" + porto;
    }

}
