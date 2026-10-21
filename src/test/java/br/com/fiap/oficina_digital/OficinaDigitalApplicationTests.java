package br.com.fiap.oficina_digital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Integração pendente: deve usar PostgreSQL/Testcontainers e as migrations Flyway, não H2 com create-drop.")
class OficinaDigitalApplicationTests {
    @Test
    void contextoDaAplicacaoInicia() {
        // Reativar quando o teste de integração executar migrations PostgreSQL em banco
        // isolado.
    }
}
