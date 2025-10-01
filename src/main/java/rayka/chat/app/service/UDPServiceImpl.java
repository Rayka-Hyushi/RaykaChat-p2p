package rayka.chat.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import rayka.chat.app.model.Mensagem;
import rayka.chat.app.model.Usuario;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class UDPServiceImpl implements UDPService {
    
    public UDPServiceImpl() { // Inicializar as threads necessárias para a aplicação
        new Thread(new RecebeSonda()).start(); // Receber sondas
        new Thread(new EnviaSonda()).start(); // Enviar sondas
        new Thread(new LimpaUsuariosInativos()).start(); // Limpar lista de usuários ativos
    }
    
    private class EnviaSonda implements Runnable { // Envia sondas para identificar os usuários online
        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(5000); // A cada 5 segundos
                if (usuario == null) {
                    continue;
                }
                Mensagem mensagem = new Mensagem(); // Cria uma mensagem
                mensagem.setTipo(Mensagem.TipoMensagem.sonda); // Do tipo sonda
                mensagem.setUsuario(usuario.getNome()); // Com o nome de usuario
                mensagem.setStatus(usuario.getStatus().toString()); // E status atual
                ObjectMapper mapper = new ObjectMapper();
                String strMensagem = mapper.writeValueAsString(mensagem);
                byte[] bMensagem = strMensagem.getBytes(); // Converte a mensagem para um array de bytes
                for (int i = 1; i < 255; i++) { // Percorre todos os endereços da sub-rede
                    DatagramPacket packet = new DatagramPacket( // Cria um DatagramPacket
                            bMensagem, // Com a mensagem
                            bMensagem.length, // Tamanho da mensagem
                            InetAddress.getByName("172.25.80." + i), // Endereço de destino
                            8080 // E porta
                    );
                    DatagramSocket socket = new DatagramSocket();
                    socket.send(packet); // Envia o pacote
                }
            }
        }
    }

    private class RecebeSonda implements Runnable { // Recebe sondas enviadas por outros usuários
        @SneakyThrows
        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(8080)) { // Cria um socket na porta 8080
                while (true) {
                    byte[] buf = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length); // Cria um pacote
                    socket.receive(packet); // Recebe o pacote enviado por outros usuarios no objeto packet
                    processarPacote(packet); // Chama o método para processar o pacote recebido
                }
            }
        }
    }
    
    private void processarPacote(DatagramPacket packet) throws JsonProcessingException { // Processa pacotes recebidos
        String strMensagem = new String(packet.getData(), 0, packet.getLength()); // Serializa o pacote recebido
        ObjectMapper mapper = new ObjectMapper();
        Mensagem mensagemRecebida = mapper.readValue(strMensagem, Mensagem.class); // Cria o objeto da mensagem recebida
        
        if (mensagemRecebida.getTipo() == Mensagem.TipoMensagem.sonda) { // Se a mensagem for do tipo sonda
            Usuario usuarioSonda = new Usuario(); // Monta o usuario remetente
            usuarioSonda.setNome(mensagemRecebida.getUsuario());
            usuarioSonda.setStatus(Usuario.StatusUsuario.valueOf(mensagemRecebida.getStatus()));
            usuarioSonda.setEndereco(packet.getAddress());
            
            ultimasSondas.put(usuarioSonda.getEndereco(), System.currentTimeMillis()); // Mapeia o timestamp para saber o tempo da ultima sonda
            usuariosOnline.put(usuarioSonda.getEndereco(), usuarioSonda); // Mapeia o objeto do usuario recebido
            
            if (usuarioListener != null) {
                usuarioListener.usuarioAlterado(usuarioSonda);
            }
        } else if (mensagemRecebida.getTipo() == Mensagem.TipoMensagem.msg_individual ||
                   mensagemRecebida.getTipo() == Mensagem.TipoMensagem.msg_grupo) {
            if (mensagemListener != null) {
                Usuario remetente = new Usuario();
                remetente.setNome(mensagemRecebida.getUsuario());
                remetente.setStatus(Usuario.StatusUsuario.valueOf(mensagemRecebida.getStatus()));
                remetente.setEndereco(packet.getAddress());
                boolean isChatGeral = mensagemRecebida.getTipo() == Mensagem.TipoMensagem.msg_grupo;
                mensagemListener.mensagemRecebida(mensagemRecebida.getTexto(), remetente, isChatGeral);
            }
        }
    }
    
    private class LimpaUsuariosInativos implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(10000);
                for (int i = 0; i <= ultimasSondas.size(); i++) {
                    Long diferenca = System.currentTimeMillis() - ultimasSondas.get("172.25.80." + i);
                    if (diferenca > 30000) {
                        InetAddress enderecoInativo = InetAddress.getByName("172.25.80." + i);
                        Usuario remover = usuariosOnline.get(enderecoInativo);
                        remover.setNome();
                        usuarioListener.usuarioRemovido();
                        ultimasSondas.remove("172.25.80." + i);
                    }
                }
            }
        }
    }
    
    @Override
    public void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral) throws IOException {
        Mensagem msg = new Mensagem();
        ObjectMapper mapper = new ObjectMapper();
        
        msg.setUsuario(usuario.getNome());
        msg.setTexto(mensagem);
        msg.setStatus(usuario.getStatus().toString());
        
        try (DatagramSocket socket = new DatagramSocket()) {
            if (chatGeral) {
                msg.setTipo(Mensagem.TipoMensagem.msg_grupo);
                String strMensagem = mapper.writeValueAsString(msg);
                byte[] bMensagem = strMensagem.getBytes();
                for (int i = 1; i < 255; i++) {
                    DatagramPacket packet = new DatagramPacket(
                            bMensagem,
                            bMensagem.length,
                            InetAddress.getByName("172.25.80." + i),
                            8080
                    );
                    socket.send(packet);
                }
            } else {
                msg.setTipo(Mensagem.TipoMensagem.msg_individual);
                String strMensagem = mapper.writeValueAsString(msg);
                byte[] bMensagem = strMensagem.getBytes();
                DatagramPacket packet = new DatagramPacket(
                        bMensagem,
                        bMensagem.length,
                        InetAddress.getByName(destinatario.getEndereco().toString()),
                        8080
                );
                socket.send(packet);
            }
        }
    }

    private final Map<InetAddress, Long> ultimasSondas = new HashMap<>(); // Mapeia a última vez que um usuario enviou uma sonda
    private final Map<InetAddress, Usuario> usuariosOnline = new HashMap<>(); // Mapeia o IP para o objeto Usuario completo
    
    private Usuario usuario = null;
    @Override
    public void usuarioAlterado(Usuario usuario) {
        this.usuario = usuario;
    }

    private UDPServiceMensagemListener mensagemListener = null;
    @Override
    public void addListenerMensagem(UDPServiceMensagemListener listener) {
        this.mensagemListener = listener;
    }

    private UDPServiceUsuarioListener usuarioListener = null;
    @Override
    public void addListenerUsuario(UDPServiceUsuarioListener listener) {
        this.usuarioListener = listener;
    }
}
