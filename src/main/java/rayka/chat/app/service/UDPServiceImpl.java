package rayka.chat.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import rayka.chat.app.model.Mensagem;
import rayka.chat.app.model.Usuario;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UDPServiceImpl implements UDPService {
    private final String prefixoRede;

    public UDPServiceImpl(String prefixoRede) {
        this.prefixoRede = prefixoRede; // Inicializar as threads necessárias para a aplicação
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
                mensagem.setTipoMensagem(Mensagem.TipoMensagem.sonda); // Do tipo sonda
                mensagem.setUsuario(usuario.getNome()); // Com o nome de usuario
                mensagem.setStatus(usuario.getStatus().toString()); // E status atual
                ObjectMapper mapper = new ObjectMapper();
                String strMensagem = mapper.writeValueAsString(mensagem);
                byte[] bMensagem = strMensagem.getBytes(); // Converte a mensagem para um array de bytes
                for (int i = 1; i < 255; i++) { // Percorre todos os endereços da sub-rede
                    DatagramPacket packet = new DatagramPacket( // Cria um DatagramPacket
                            bMensagem, // Com a mensagem
                            bMensagem.length, // Tamanho da mensagem
                            InetAddress.getByName(UDPServiceImpl.this.prefixoRede + "." + i), // Endereço de destino
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

    private void processarPacote(DatagramPacket packet) throws JsonProcessingException, UnknownHostException { // Processa pacotes recebidos
        String strMensagem = new String(packet.getData(), 0, packet.getLength()); // Serializa o pacote recebido
        ObjectMapper mapper = new ObjectMapper();
        Mensagem mensagemRecebida = mapper.readValue(strMensagem, Mensagem.class); // Cria o objeto da mensagem recebida

        if (this.usuario != null && packet.getAddress().equals(this.usuario.getEndereco())) { // Ignora pacotes de broadcast do próprio IP
            return;
        }

        if (mensagemRecebida.getTipoMensagem() == Mensagem.TipoMensagem.sonda) { // Se a mensagem for do tipo sonda
            Usuario usuarioSonda = new Usuario(); // Monta o usuario remetente
            usuarioSonda.setNome(mensagemRecebida.getUsuario());
            usuarioSonda.setStatus(Usuario.StatusUsuario.valueOf(mensagemRecebida.getStatus()));
            usuarioSonda.setEndereco(packet.getAddress());

            ultimasSondas.put(usuarioSonda.getEndereco(), System.currentTimeMillis()); // Mapeia o timestamp para saber o tempo da ultima sonda
            usuariosOnline.put(usuarioSonda.getEndereco(), usuarioSonda); // Mapeia o objeto do usuario recebido

            if (usuarioListener != null) {
                usuarioListener.usuarioAlterado(usuarioSonda);
            }
        } else if (mensagemRecebida.getTipoMensagem() == Mensagem.TipoMensagem.msg_individual ||
                mensagemRecebida.getTipoMensagem() == Mensagem.TipoMensagem.msg_grupo) { // Se for do tipo grupo ou individual
            if (mensagemListener != null) {
                Usuario remetente = new Usuario(); // Monta o usuário remetente
                remetente.setNome(mensagemRecebida.getUsuario());
                remetente.setStatus(Usuario.StatusUsuario.valueOf(mensagemRecebida.getStatus()));
                remetente.setEndereco(packet.getAddress());
                boolean isChatGeral = mensagemRecebida.getTipoMensagem() == Mensagem.TipoMensagem.msg_grupo;
                mensagemListener.mensagemRecebida(mensagemRecebida.getMsg(), remetente, isChatGeral); // Mostra a mensagem recebida de acordo com seu tipo
            }
        } else if (mensagemRecebida.getTipoMensagem() == Mensagem.TipoMensagem.fim_chat) { // Se for do tipo fim_chat
            if (mensagemListener != null) {
                Usuario remetente = new Usuario(); // Monta o usuário remetente
                remetente.setNome(mensagemRecebida.getUsuario());
                remetente.setEndereco(packet.getAddress());
                mensagemListener.chatFechado(remetente); // Chama o método para notificar o usuário que o chat foi fechado
            }
        }
    }

    private class LimpaUsuariosInativos implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(10000);
                for (Map.Entry<InetAddress, Long> entry : ultimasSondas.entrySet()) {
                    InetAddress enderecoInativo = entry.getKey();
                    long ultimoTimestamp = entry.getValue();

                    long diferenca = System.currentTimeMillis() - ultimoTimestamp;
                    if (diferenca > 30000) {
                        Usuario remover = usuariosOnline.get(enderecoInativo);
                        if (remover != null && usuarioListener != null) {
                            usuarioListener.usuarioRemovido(remover);
                        }
                        usuariosOnline.remove(enderecoInativo);
                        ultimasSondas.remove(enderecoInativo);
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
        msg.setMsg(mensagem);
        msg.setStatus(usuario.getStatus().toString());

        try (DatagramSocket socket = new DatagramSocket()) {
            if (chatGeral) {
                msg.setTipoMensagem(Mensagem.TipoMensagem.msg_grupo);
                String strMensagem = mapper.writeValueAsString(msg);
                byte[] bMensagem = strMensagem.getBytes();
                for (int i = 1; i < 255; i++) {
                    DatagramPacket packet = new DatagramPacket(
                            bMensagem,
                            bMensagem.length,
                            InetAddress.getByName(this.prefixoRede + "." + i),
                            8080
                    );
                    socket.send(packet);
                }
            } else {
                msg.setTipoMensagem(Mensagem.TipoMensagem.msg_individual);
                String strMensagem = mapper.writeValueAsString(msg);
                byte[] bMensagem = strMensagem.getBytes();
                DatagramPacket packet = new DatagramPacket(
                        bMensagem,
                        bMensagem.length,
                        InetAddress.getByAddress(destinatario.getEndereco().getAddress()),
                        8080
                );
                socket.send(packet);
            }
        }
    }

    @Override
    public void enviarFimChat(Usuario destinatario) throws IOException { // Envia a mensagem de fim_chat
        Mensagem msg = new Mensagem();
        ObjectMapper mapper = new ObjectMapper();

        msg.setUsuario(usuario.getNome());
        msg.setTipoMensagem(Mensagem.TipoMensagem.fim_chat);

        try (DatagramSocket socket = new DatagramSocket()) {
            String strMensagem = mapper.writeValueAsString(msg);
            byte[] bMensagem = strMensagem.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    bMensagem,
                    bMensagem.length,
                    InetAddress.getByAddress(destinatario.getEndereco().getAddress()),
                    8080
            );
            socket.send(packet);
        }
    }

    private final Map<InetAddress, Long> ultimasSondas = new ConcurrentHashMap<>(); // Mapeia a última vez que um usuario enviou uma sonda
    private final Map<InetAddress, Usuario> usuariosOnline = new ConcurrentHashMap<>(); // Mapeia o IP para o objeto Usuario completo

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
