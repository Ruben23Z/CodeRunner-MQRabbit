package runserver;


public class Resultado {
    private final String estado;
    private final int codigoSaida;
    private final String detalhe;

    public Resultado(String estado, int codigoSaida, String detalhe) {
        this.estado = estado;
        this.codigoSaida = codigoSaida;
        this.detalhe = detalhe;
    }

    public String getEstado() {
        return estado;
    }

    public int getCodigoSaida() {
        return codigoSaida;
    }

    public String getDetalhe() {
        return detalhe;
    }
}