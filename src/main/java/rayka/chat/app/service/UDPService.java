package rayka.chat.app.service;

import rayka.chat.app.model.Usuario;

import java.io.IOException;

public interface UDPService {

    /**
     * Envia uma mensagem para um destinatário
     * @param mensagem
     * @param destinatario
     * @param chatGeral
     */
    void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral) throws IOException;

    /**
     * Notifica que o próprio usuário foi alterado
     * @param usuario
     */
    void usuarioAlterado(Usuario usuario);

    /**
     * Adiciona um listener para indicar o recebimento de mensagens
     * @param listener
     */
    void addListenerMensagem(UDPServiceMensagemListener listener);

    /**
     * Adiciona um listener para indicar recebimento e/ou alterações em usuários
     * @param listener
     */
    void addListenerUsuario(UDPServiceUsuarioListener listener);


}
