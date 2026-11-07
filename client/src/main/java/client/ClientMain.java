package client;

import clientemanagerstubs.RunServerEndereco;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import execucaostubs.Estado;
import execucaostubs.InfoPedido;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Scanner;

public class ClientMain {

    private static final int MAX_TENTATIVAS = 3;

    private final Scanner in = new Scanner(System.in);
    private ExecucaoClient servidor; // null até obter um
    private final ManagerClient manager;

    public ClientMain(ManagerClient manager) {
        this.manager = manager;
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: ClientMain <managerIp> <managerPorto>");
            return;
        }


        String ip = args[0];
        int porto;

        try {
            porto = Integer.parseInt(args[1].trim());
        } catch (NumberFormatException e) {
            System.err.println("Porto inválido.");
            return;
        }
        ManagerClient manager = new ManagerClient(ip, porto);
        ClientMain cliente = new ClientMain(manager);

        try {
            cliente.iniciar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Execução interrompida.");
        } catch (IOException e) {
            System.err.println("Erro de leitura: " + e.getMessage());
        } finally {
            cliente.fechar();
        }
    }

    private int menu() {
        int op;
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Obter servidor");
            System.out.println(" 2 - Submeter programa");
            System.out.println(" 3 - Consultar resultado");
            System.out.println(" 4 - Trocar de servidor");
            System.out.println("99 - Sair");
            System.out.println("Opção?");
            try {
                op = Integer.parseInt(in.nextLine().trim());
            } catch (NumberFormatException e) {
                op = -1;
            }
        } while (!((op >= 1 && op <= 4) || op == 99));
        return op;
    }

    private void iniciar() throws IOException, InterruptedException {

        boolean fim = false;

        while (!fim) {
            try {
                int opcao = menu();

                switch (opcao) {
                    case 1:
                        obterServidor();
                        break;

                    case 2:
                        submeter();
                        break;

                    case 3:
                        consultar();
                        break;

                    case 4:
                        obterServidor();
                        break;

                    case 99:
                        fim = true;
                        break;
                }

            } catch (StatusRuntimeException e) {
                System.out.println("Erro: " + e.getStatus().getCode() + " - " + e.getStatus().getDescription());
            }
        }
    }

    private void obterServidor() throws InterruptedException {
        RunServerEndereco rse = manager.obterServidor();
        if (servidor != null) {
            servidor.fechar();
        }

        servidor = new ExecucaoClient(rse.getIp(), rse.getPorto());

        System.out.println("Servidor atribuído: " + rse.getIp() + ":" + rse.getPorto());
    }

    private void submeter() throws InterruptedException {
        if (servidor == null) obterServidor();
        System.out.println("Caminho do ficheiro .py:");
        String caminhoProg = in.nextLine().trim();
        System.out.println("Caminho do input (linha vazia = sem input):");
        String caminhoInput = in.nextLine().trim();
        byte[] prog;
        byte[] input = new byte[0];
        try {
            prog = Files.readAllBytes(Path.of(caminhoProg));
            if (!caminhoInput.isEmpty()) input = Files.readAllBytes(Path.of(caminhoInput));
        } catch (NoSuchFileException e) {
            System.out.println("Ficheiro não encontrado: " + e.getFile());
            return;
        } catch (IOException e) {
            System.out.println("Erro a ler ficheiro: " + e.getMessage());
            return;
        }
        int tentativas = 1;

        while (tentativas <= MAX_TENTATIVAS) {
            try {
                String id = servidor.submeter(prog, input);
                System.out.println("Pedido aceite. UUID: " + id);
                return;

            } catch (StatusRuntimeException e) {

                // Só repete se a fila estiver cheia
                if (e.getStatus().getCode() != Status.Code.RESOURCE_EXHAUSTED || tentativas >= MAX_TENTATIVAS) {
                    throw e;
                }

                tentativas++;

                System.out.println("Servidor cheio, a pedir outro...");
                obterServidor();
            }
        }
    }

    private void consultar() throws InterruptedException, IOException {
        System.out.println("UUID do pedido:");
        String id = in.nextLine().trim();
        if (id.isEmpty()) return;
        if (servidor == null) obterServidor();

        System.out.println("--- resultado ---");
        InfoPedido info = servidor.consultar(id, System.out, System.err);
        System.out.println();
        System.out.println("--- fim ---");

        if (info == null) {
            System.out.println("Resposta sem informação do pedido.");
            return;
        }
        System.out.println("Estado: " + info.getEstado());
        if (info.getEstado() != Estado.PENDENTE && info.getEstado() != Estado.EM_EXECUCAO) {
            System.out.println("Código de saída: " + info.getCodigoSaida());
        }
        if (!info.getMensagem().isEmpty()) {
            System.out.println("Mensagem: " + info.getMensagem());
        }
        if (info.getEstado() == Estado.PENDENTE || info.getEstado() == Estado.EM_EXECUCAO) {
            System.out.println("Ainda não terminou; tente consultar mais tarde.");
        }
    }

    private void fechar() {
        if (servidor != null) servidor.fechar();
        manager.fechar();
    }
}
