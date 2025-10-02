package rayka.chat.app.service;

import rayka.chat.app.model.Usuario;

public interface UDPServiceMensagemListener {

    /**
     * Notifica que uma mensagem foi recebida
     *
     * @param mensagem
     * @param remetente
     * @param chatGeral
     */
    void mensagemRecebida(String mensagem, Usuario remetente, boolean chatGeral);
}
