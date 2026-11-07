package demo;


import execucaostubs.Estado;

public class DemoMain {

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            System.err.println("Uso: DemoMain <managerIp> <managerPorto> <nClientes>");
            System.exit(1);
        }
        String managerIp = args[0];
        int managerPorto = Integer.parseInt(args[1]);
        int n = Integer.parseInt(args[2]);

        // os 4 comportamentos do Anexo 3
        String[] nomes = {"soma", "erro", "loop", "memory"};
        String[] programas = {"import sys\nnumeros = [float(l) for l in sys.stdin if l.strip()]\n" + "print('count:', len(numeros))\nprint('sum:', sum(numeros))\n", "print(1 / 0)\n", "while True:\n    pass\n", "blocos = []\nwhile True:\n    blocos.append(bytearray(10 * 1024 * 1024))\n"};
        String[] inputs = {"10\n20\n30.5\n-5\n", "", "", ""};
        Estado[] esperados = {Estado.CONCLUIDO, Estado.ERRO, Estado.TIMEOUT, Estado.ERRO};

        ClienteDemo[] clientes = new ClienteDemo[n];
        Thread[] threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            int k = i % 4;
            clientes[i] = new ClienteDemo(i + 1, managerIp, managerPorto, nomes[k], programas[k], inputs[k], esperados[k]);
            threads[i] = new Thread(clientes[i]);
            threads[i].start();
        }
        for (int i = 0; i < n; i++) {
            threads[i].join();
        }

        System.out.println();
        System.out.println("cliente | programa | esperado  | obtido    | servidor        | resultado");
        int falhas = 0;
        for (int i = 0; i < n; i++) {
            ClienteDemo c = clientes[i];
            String resultado = c.ok ? "OK" : "FALHA " + c.erro;
            System.out.println((i + 1) + " | " + nomes[i % 4] + " | " + esperados[i % 4] + " | " + c.obtido + " | " + c.servidor + " | " + resultado);
            if (!c.ok) falhas++;
        }
        System.out.println();
        System.out.println(falhas == 0 ? "TUDO OK" : falhas + " FALHA(S)");
        System.exit(falhas == 0 ? 0 : 1);
    }
}

//java -jar manager\build\libs\manager.jar 9000 127.0.0.1 6379
//java -jar runserver\build\libs\runserver.jar 127.0.0.1 9001 127.0.0.1 9000
//
//docker rm -f redis
//docker run -d --name redis -p 6379:6379 redis:8
//docker ps --filter name=redis
//Test-NetConnection 127.0.0.1 -Port 6379

//java -jar runserver\build\libs\runserver.jar 127.0.0.1 9001 127.0.0.1 9000
//java -jar runserver\build\libs\runserver.jar 127.0.0.1 9002 127.0.0.1 9000
//java -jar runserver\build\libs\runserver.jar 127.0.0.1 9003 127.0.0.1 9000

//java -jar client\build\libs\client.jar 127.0.0.1 9000
//
//$linhas = @("1")
//   1..8 | ForEach-Object { $linhas += "2"; $linhas += "C:\Users\ruben\OneDrive\CD\CodeRunner03\work\loop.py"; $linhas += "" }
//$linhas += "99"
//$linhas | java -jar client\build\libs\client.jar 127.0.0.1 9000
////
//C:\Users\ruben\OneDrive\CD\CodeRunner03\work\loop.py
//C:\Users\ruben\OneDrive\CD\CodeRunner03\work\input.txt