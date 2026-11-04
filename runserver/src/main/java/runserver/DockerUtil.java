package runserver;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class DockerUtil {
    public DockerClient criarCliente(){
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient http = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost()).sslConfig(config.getSSLConfig()).build();
        return DockerClientImpl.getInstance(config, http);
    }

    public void removerOrfaos(DockerClient docker, int porto){
        String prefixo = "/run-" + porto + "-";

        for(Container c: docker.listContainersCmd().withShowAll(true).exec()){
            for(String nome: c.getNames()){
                if (nome.startsWith(prefixo)) {
                    docker.removeContainerCmd(c.getId()).withForce(true).exec();
                    System.out.println("Removido container órfão " + nome);
                }
            }
        }
    }
    public void verificar(DockerClient docker, String imagem) {
        try {
            docker.pingCmd().exec();
        } catch (Exception e) {
            throw new IllegalStateException("Docker inacessível: " + e.getMessage(), e);
        }
        try {
            docker.inspectImageCmd(imagem).exec();
        } catch (NotFoundException e) {
            throw new IllegalStateException("Imagem em falta. Executar: docker pull " + imagem, e);
        }
    }
}
