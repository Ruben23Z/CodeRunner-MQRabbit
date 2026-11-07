package demo;

import client.ExecucaoClient;
import client.ManagerClient;
import clientemanagerstubs.RunServerEndereco;
import execucaostubs.Estado;
import execucaostubs.InfoPedido;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.io.ByteArrayOutputStream;

// Um cliente simulado: submete num servidor, consulta noutro e confirma a entrega única
class ClienteDemo implements Runnable {
    private final int numero;
    private final String managerIp;
    private final int managerPorto;
    private final String nomePrograma;
    private final String programa;
    private final String input;
    private final Estado esperado;

    // resultados, lidos pelo main depois do join
    String servidor = "-";
    Estado obtido = Estado.ESTADO_DESCONHECIDO;
    boolean ok = false;
    String erro = "";

    ClienteDemo(int numero, String managerIp, int managerPorto,
                String nomePrograma, String programa, String input, Estado esperado) {
        this.numero = numero;
        this.managerIp = managerIp;
        this.managerPorto = managerPorto;
        this.nomePrograma = nomePrograma;
        this.programa = programa;
        this.input = input;
        this.esperado = esperado;
    }

    @Override
    public void run() {
        ManagerClient manager = new ManagerClient(managerIp, managerPorto);
        ExecucaoClient a = null;
        ExecucaoClient b = null;
        try {
// 1. servidor A: submeter (em RESOURCE_EXHAUSTED, pede outro servidor)
            String uuid = null;
            for (int t = 1; t <= 3 && uuid == null; t++) {
                RunServerEndereco ea = manager.obterServidor();
                if (a != null) a.fechar();
                a = new ExecucaoClient(ea.getIp(), ea.getPorto());
                servidor = ea.getIp() + ":" + ea.getPorto();
                try {
                    uuid = a.submeter(programa.getBytes(), input.getBytes());
                } catch (StatusRuntimeException e) {
                    if (e.getStatus().getCode() != Status.Code.RESOURCE_EXHAUSTED || t == 3) throw e;
                    System.out.println("[cliente " + numero + "] " + servidor + " cheio, a pedir outro");
                }
            }
            System.out.println("[cliente " + numero + "] " + nomePrograma + " submetido em " + servidor + " -> " + uuid);
            // 2. servidor B (round-robin): consultar até o resultado estar disponível
            RunServerEndereco eb = manager.obterServidor();
            b = new ExecucaoClient(eb.getIp(), eb.getPorto());
            InfoPedido info = null;
            for (int i = 0; i < 120; i++) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayOutputStream err = new ByteArrayOutputStream();
                info = b.consultar(uuid, out, err);
                if (info != null && info.getResultadoDisponivel()) break;
                Thread.sleep(1000);
            }
            if (info == null || !info.getResultadoDisponivel()) {
                erro = "sem resultado ao fim de 120 s";
                return;
            }
            obtido = info.getEstado();

            // 3. segunda consulta em A: o resultado já foi entregue
            ByteArrayOutputStream out2 = new ByteArrayOutputStream();
            ByteArrayOutputStream err2 = new ByteArrayOutputStream();
            InfoPedido segunda = a.consultar(uuid, out2, err2);
            boolean jaEntregue = segunda != null && !segunda.getResultadoDisponivel();

            ok = (obtido == esperado) && jaEntregue;
            if (!jaEntregue) erro = "a 2.ª consulta devolveu o resultado outra vez";
        } catch (StatusRuntimeException e) {
            erro = e.getStatus().getCode() + " " + e.getStatus().getDescription();
        } catch (Exception e) {
            erro = e.toString();
        } finally {
            if (a != null) a.fechar();
            if (b != null) b.fechar();
            manager.fechar();
        }
    }
}