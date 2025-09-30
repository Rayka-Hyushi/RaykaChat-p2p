package rayka.chat.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import rayka.chat.app.model.Mensagem;
import rayka.chat.app.model.Usuario;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPServiceImpl implements UDPService {

    private class EnviaSonda implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(1000);
                if (usuario == null) {
                    continue;
                }
                Mensagem mensagem = new Mensagem();
                mensagem.setTipoMensagem(sonda);
                mensagem.setUsuario(usuario.getNome());
                mensagem.setStatus(usuario.getStatus().toString());
                ObjectMapper mapper = new ObjectMapper();
                String strMensagem = mapper.writeValueAsString(mensagem);
                byte[] bMensagem = strMensagem.getBytes();

                for (int i = 1; i < 255; i++) {
                    DatagramPacket pacote = new DatagramPacket(bMensagem, bMensagem.length, InetAddress.getByName("192.168.83." + i), 8080);
                    DatagramSocket socket = new DatagramSocket();
                    socket.send(pacote);
                }
            }
        }
    }

    @Override
    public void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral) {

    }

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
