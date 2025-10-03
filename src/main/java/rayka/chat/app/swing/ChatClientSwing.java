package rayka.chat.app.swing;

import lombok.Getter;
import lombok.SneakyThrows;
import rayka.chat.app.model.Usuario;
import rayka.chat.app.service.UDPService;
import rayka.chat.app.service.UDPServiceMensagemListener;
import rayka.chat.app.service.UDPServiceUsuarioListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * User: Rafael
 * Date: 13/10/14
 * Time: 10:28
 */
public class ChatClientSwing extends JFrame {
    private final Usuario meuUsuario;
    private JList listaChat;
    private DefaultListModel<Usuario> dfListModel;
    private JTabbedPane tabbedPane = new JTabbedPane();
    private Set<Usuario> chatsAbertos = new HashSet<>();
    private UDPService udpService;
    private Usuario USER_GERAL = new Usuario("Geral", null, null);

    public ChatClientSwing(UDPService service, String prefixoRede) throws UnknownHostException {
        this.udpService = service;
        setLayout(new GridBagLayout());
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Status");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(Usuario.StatusUsuario.DISPONIVEL.name());
        rbMenuItem.setSelected(true);
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(Usuario.StatusUsuario.DISPONIVEL);
                udpService.usuarioAlterado(meuUsuario);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(Usuario.StatusUsuario.NAO_PERTURBE.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(Usuario.StatusUsuario.NAO_PERTURBE);
                udpService.usuarioAlterado(meuUsuario);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(Usuario.StatusUsuario.VOLTO_LOGO.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(Usuario.StatusUsuario.VOLTO_LOGO);
                udpService.usuarioAlterado(meuUsuario);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popupMenu = new JPopupMenu();
                    final int tab = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                    if (tab > 0 && tab < tabbedPane.getTabCount()) {
                        JMenuItem item = new JMenuItem("Fechar");
                        item.addActionListener(new ActionListener() {
                            @SneakyThrows
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                PainelChatPVT painel = (PainelChatPVT) tabbedPane.getComponentAt(tab);
                                if (painel != null) {
                                    udpService.enviarFimChat(painel.getUsuario()); // Envia a mensagem fim_chat para o usuário antes de fechar a aba
                                    tabbedPane.remove(tab);
                                    chatsAbertos.remove(painel.getUsuario());
                                }
                            }
                        });
                        popupMenu.add(item);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
        add(new JScrollPane(criaLista()), new GridBagConstraints(0, 0, 1, 1, 0.1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        setSize(800, 600);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - this.getWidth()) / 2;
        final int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Rayka Chat P2P - Redes de Computadores");
        String nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuario: ");

        InetAddress ipLocal = encontrarIPLocal(prefixoRede); // Chama o método para encontrar o ip correto entre as interfaces de rede do usuário
        if (ipLocal == null) {
            JOptionPane.showMessageDialog(
                    this, "Não foi possível encontrar um IP válido na sub-rede " + prefixoRede,
                    "Erro de Rede", JOptionPane.ERROR_MESSAGE);
            System.exit(1); // Se o IP não for encontrado, finaliza com status 1
        }

        this.meuUsuario = new Usuario(nomeUsuario, Usuario.StatusUsuario.DISPONIVEL, ipLocal);
        udpService.usuarioAlterado(meuUsuario);
        udpService.addListenerMensagem(new MensagemListener());
        udpService.addListenerUsuario(new UsuarioListener());
        setVisible(true);
    }

    private InetAddress encontrarIPLocal(String prefixoRede) { // Busca entre as interfaces de rede, a que possui o ip inserido pelo usuário
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (!ni.isLoopback() && ni.isUp()) {
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        InetAddress addr = ia.getAddress();
                        // Verifica se o endereço não é nulo e se começa com o prefixo da rede
                        if (addr instanceof java.net.Inet4Address && addr.getHostAddress().startsWith(prefixoRede)) {
                            return addr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Retorna null se não encontrar
    }

    private JComponent criaLista() {
        dfListModel = new DefaultListModel();
        listaChat = new JList(dfListModel);
        listaChat.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    Usuario user = (Usuario) list.getModel().getElementAt(index);
                    if (chatsAbertos.add(user)) {
                        tabbedPane.add(user.toString(), new PainelChatPVT(user, false));
                    }
                }
            }
        });
        chatsAbertos.add(USER_GERAL);
        tabbedPane.add("Geral", new PainelChatPVT(USER_GERAL, true));
        return listaChat;
    }

    @Getter
    class PainelChatPVT extends JPanel {
        JTextArea areaChat;
        JTextField campoEntrada;
        Usuario usuario;
        boolean chatGeral = false;

        PainelChatPVT(Usuario usuario, boolean chatGeral) {
            setLayout(new GridBagLayout());
            areaChat = new JTextArea();
            this.usuario = usuario;
            areaChat.setEditable(false);
            campoEntrada = new JTextField();
            this.chatGeral = chatGeral;
            campoEntrada.addActionListener(new ActionListener() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JTextField) e.getSource()).setText("");
                    areaChat.append(meuUsuario.getNome() + "> " + e.getActionCommand() + "\n");
                    udpService.enviarMensagem(e.getActionCommand(), usuario, chatGeral);
                }
            });
            add(new JScrollPane(areaChat), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(campoEntrada, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        }


    }

    private class UsuarioListener implements UDPServiceUsuarioListener {
        @Override
        public void usuarioAdicionado(final Usuario usuario) { // Adiciona usuários na lista de usuários online
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!dfListModel.contains(usuario)) {
                        dfListModel.addElement(usuario);
                    }
                }
            });
        }
        
        @Override
        public void usuarioRemovido(final Usuario usuario) { // Remove usuários da lista de usuários online e fecha a janela de chat se estiver aberta
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    dfListModel.removeElement(usuario); // Remove o usuário da lista de online
                    
                    if (chatsAbertos.contains(usuario)) {
                        for (int i = 1; i < tabbedPane.getTabCount(); i++) { // Itera nas abas para encontrar e fechar a aba do usuário removido
                            Component component = tabbedPane.getComponentAt(i); // (Começa em 1 para ignorar a aba "Geral")
                            if (component instanceof PainelChatPVT) {
                                PainelChatPVT p = (PainelChatPVT) component;
                                
                                if (p.getUsuario().equals(usuario)) { // Verifica se o usuário do painel é o usuário removido
                                    tabbedPane.remove(i);
                                    chatsAbertos.remove(usuario);
                                    break; // Sai do loop após a remoção
                                }
                            }
                        }
                    }
                }
            });
        }
        
        @Override
        public void usuarioAlterado(final Usuario usuario) { // Atualiza os objetos na lista de usuários online
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    int index = dfListModel.indexOf(usuario);
                    
                    if (index >= 0) {
                        dfListModel.remove(index); // Remove o objeto antigo
                        dfListModel.add(index, usuario); // Adiciona o novo objeto
                    } else {
                        dfListModel.addElement(usuario); // Se não foi encontrado, adicione-o no final (como se fosse um novo usuário)
                    }
                }
            });
        }
    }

    private class MensagemListener implements UDPServiceMensagemListener {
        @Override
        public void mensagemRecebida(String mensagem, Usuario remetente, boolean chatGeral) {
            SwingUtilities.invokeLater(new Runnable() { // Garante que a manipulação da UI ocorra na Event Dispatch Thread
                @Override
                public void run() {
                    PainelChatPVT painel = null;
                    int tabIndex = -1;
                    
                    if (chatGeral) {
                        painel = (PainelChatPVT) tabbedPane.getComponentAt(0);
                        tabIndex = 0;
                    } else {
                        for (int i = 1; i < tabbedPane.getTabCount(); i++) { // Tenta encontrar o chat aberto
                            PainelChatPVT p = (PainelChatPVT) tabbedPane.getComponentAt(i);
                            if (p.getUsuario().equals(remetente)) {
                                painel = p;
                                tabIndex = i; // Armazena o índice do chat encontrado
                                break;
                            }
                        }
                        
                        if (painel == null) { // Se o chat não estiver aberto, abre automaticamente
                            if (chatsAbertos.add(remetente)) {
                                painel = new PainelChatPVT(remetente, false);
                                tabbedPane.add(remetente.toString(), painel);
                                tabIndex = tabbedPane.getTabCount() - 1; // O índice será o último adicionado
                            }
                        }
                    }
                    if (painel != null) { // Se um painel válido for processado:
                        painel.getAreaChat().append(remetente.getNome() + "> " + mensagem + "\n"); // Anexa a mensagem
                    }
                }
            });
        }

        @Override
        public void chatFechado(final Usuario remetente) { // Notifica o usuário quando o usuário remoto fecha o chat individual
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (chatsAbertos.contains(remetente)) { // Se o usuario remoto esta presente nas janelas de chat abertas
                        for (int i = 1; i < tabbedPane.getTabCount(); i++) { // Percorre as janelas abertas
                            Component component = tabbedPane.getComponentAt(i);
                            if (component instanceof PainelChatPVT) {
                                PainelChatPVT p = (PainelChatPVT) component;
                                if (p.getUsuario().equals(remetente)) { // Busca a janela do usuário remoto
                                    JOptionPane.showMessageDialog(ChatClientSwing.this,
                                            remetente.getNome() + " encerrou o chat.", "Chat Encerrado",
                                            JOptionPane.INFORMATION_MESSAGE); // Notifica que o usuário remoto fechou o chat
                                    tabbedPane.remove(i);
                                    chatsAbertos.remove(remetente); // e fecha a janela de chat no lado do usuário também
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
