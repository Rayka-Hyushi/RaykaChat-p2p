package rayka.chat.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Mensagem {
    public enum TipoMensagem {sonda, msg_individual, fim_chat, msg_grupo};

    private TipoMensagem tipo;
    private String usuario;
    private String status;
    private String texto;
}
