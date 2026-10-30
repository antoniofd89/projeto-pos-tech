package br.com.fiap.oficina_digital.cliente.infraestrutura.persistencia;

import br.com.fiap.oficina_digital.cliente.dominio.Cliente;
import br.com.fiap.oficina_digital.cliente.dominio.Documento;
import br.com.fiap.oficina_digital.cliente.dominio.Endereco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("AdaptadorRepositorioCliente")
class AdaptadorRepositorioClienteTest {

    @Autowired
    AdaptadorRepositorioCliente adaptador;

    private static final Endereco ENDERECO = new Endereco(
            "01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP");

    private static Cliente novoCliente(String cpf, String nome) {
        return Cliente.criar(
                new Documento(cpf),
                nome,
                nome.toLowerCase().replace(" ", "") + "@email.com",
                "11999999999",
                ENDERECO,
                LocalDate.of(1990, 1, 15)
        );
    }

    private Cliente clienteSalvo;

    @BeforeEach
    void setUp() {
        clienteSalvo = adaptador.salvar(novoCliente("529.982.247-25", "João Silva"));
    }

    @Nested
    @DisplayName("salvar")
    class Salvar {

        @Test
        @DisplayName("deve persistir cliente e retornar com ID preenchido")
        void devePersistirClienteComId() {
            assertThat(clienteSalvo.getId()).isNotNull();
            assertThat(clienteSalvo.getNome()).isEqualTo("João Silva");
        }

        @Test
        @DisplayName("salvar duas vezes o mesmo cliente não deve duplicar (update)")
        void salvarMesmoClienteDeveAtualizarNaoDuplicar() {
            clienteSalvo.atualizarDados("João Atualizado", "joao.atualizado@email.com",
                    "11999999999", ENDERECO, LocalDate.of(1990, 1, 15));

            adaptador.salvar(clienteSalvo);

            var lista = adaptador.listar();
            long qtdComMesmoId = lista.stream()
                    .filter(c -> c.getId().equals(clienteSalvo.getId()))
                    .count();

            assertThat(qtdComMesmoId).isEqualTo(1);
            assertThat(lista.stream()
                    .filter(c -> c.getId().equals(clienteSalvo.getId()))
                    .findFirst().get().getNome())
                    .isEqualTo("João Atualizado");
        }
    }

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("deve encontrar cliente pelo ID após salvar")
        void deveEncontrarClientePeloId() {
            var resultado = adaptador.buscarPorId(clienteSalvo.getId());

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(clienteSalvo.getId());
            assertThat(resultado.get().getNome()).isEqualTo("João Silva");
        }

        @Test
        @DisplayName("deve retornar Optional.empty() para ID inexistente")
        void deveRetornarVazioParaIdInexistente() {
            var resultado = adaptador.buscarPorId(UUID.randomUUID());

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("buscarPorDocumento")
    class BuscarPorDocumento {

        @Test
        @DisplayName("deve encontrar cliente pelo documento (CPF) após salvar")
        void deveEncontrarClientePeloDocumento() {
            var resultado = adaptador.buscarPorDocumento(new Documento("529.982.247-25"));

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getDocumento().numero()).isEqualTo("52998224725");
        }

        @Test
        @DisplayName("deve retornar Optional.empty() para documento não cadastrado")
        void deveRetornarVazioParaDocumentoNaoCadastrado() {
            var resultado = adaptador.buscarPorDocumento(new Documento("11.222.333/0001-81"));

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("deve retornar todos os clientes salvos")
        void deveRetornarTodosOsClientes() {
            adaptador.salvar(novoCliente("111.444.777-35", "Ana Costa"));

            var lista = adaptador.listar();

            assertThat(lista).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("deve retornar lista com pelo menos o cliente do setUp")
        void deveRetornarPeloMenosUmCliente() {
            var lista = adaptador.listar();

            assertThat(lista).isNotEmpty();
            assertThat(lista.stream().anyMatch(c -> c.getNome().equals("João Silva"))).isTrue();
        }
    }
}
