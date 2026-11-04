package runserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EstadoExecucao {

    public static String decCodigo(int codigo) {

        if (codigo == 0) {
            return "CONCLUIDO";
        }

        if (codigo == 124) {
            return "TIMEOUT";
        }

        return "ERRO";
    }

    public static int lerCodigo(Path exitTxt) {

        try {
            String conteudo = Files.readString(exitTxt);
            int parse =Integer.parseInt(conteudo.trim());

            return parse;
        } catch (IOException e) {
            return -1;

        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
