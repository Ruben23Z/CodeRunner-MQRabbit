package coderunner.runserver;

import com.github.dockerjava.core.DefaultDockerClientConfig;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
public class DockerUtil {
    public DockerClient criarCliente(){
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        var http = new ZerodepDockerHttpClient.Builder().dockerHost(config.getDockerHost()).sslConfig(config.getSSLConfig()).build();
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
}
