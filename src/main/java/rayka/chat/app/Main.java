package rayka.chat.app;

import rayka.chat.app.service.UDPService;
import rayka.chat.app.service.UDPServiceImpl;
import rayka.chat.app.swing.ChatClientSwing;

import javax.swing.*;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        String prefixoRede = JOptionPane.showInputDialog(null, "Digite o prefixo da sua rede (EX: 192.168.80)", "Definição de Rede Local", JOptionPane.QUESTION_MESSAGE);
        if (prefixoRede == null || prefixoRede.isEmpty()) {
            System.exit(0);
        }
        final UDPService udpService = new UDPServiceImpl(prefixoRede);
        new ChatClientSwing(udpService);
    }
}