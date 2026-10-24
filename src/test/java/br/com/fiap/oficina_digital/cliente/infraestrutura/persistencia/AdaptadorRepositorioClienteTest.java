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

            var lista = adaptador.listar(0, 100);
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
        @DisplayName("deve retornar clientes paginados ordenados por nome")
        void deveRetornarClientesPaginadosOrdenadosPorNome() {
            // Salva um segundo cliente com nome anterior na ordenação
            adaptador.salvar(novoCliente("11.222.333/0001-81", "Ana Costa"));

            var pagina0 = adaptador.listar(0, 10);

            assertThat(pagina0).hasSizeGreaterThanOrEqualTo(2);
            // Ana vem antes de João na ordenação por nome
            assertThat(pagina0.get(0).getNome()).isEqualTo("Ana Costa");
            assertThat(pagina0.get(1).getNome()).isEqualTo("João Silva");
        }

        @Test
        @DisplayName("deve respeitar o tamanho da página")
        void deveRespeitarTamanhoDaPagina() {
            adaptador.salvar(novoCliente("11.222.333/0001-81", "Ana Costa"));

            var pagina = adaptador.listar(0, 1);

            assertThat(pagina).hasSize(1);
        }

        @Test
        @DisplayName("deve retornar lista vazia para página além do total")
        void deveRetornarVazioParaPaginaAlemDoTotal() {
            var pagina = adaptador.listar(999, 10);

            assertThat(pagina).isEmpty();
        }
    }
}
