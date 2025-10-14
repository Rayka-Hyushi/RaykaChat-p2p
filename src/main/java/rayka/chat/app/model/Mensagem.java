package rayka.chat.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Mensagem {
    public enum TipoMensagem {sonda, msg_individual, fim_chat, msg_grupo};

    private TipoMensagem tipoMensagem;
    private String usuario;
    private String status;
    private String msg;

    @JsonCreator
    public static TipoMensagem fromString(String value) {
        return TipoMensagem.valueOf(value.toLowerCase());
    }
}
