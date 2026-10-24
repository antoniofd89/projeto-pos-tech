package br.com.fiap.oficina_digital.cliente.aplicacao;

import br.com.fiap.oficina_digital.cliente.dominio.Cliente;
import br.com.fiap.oficina_digital.cliente.dominio.Documento;
import br.com.fiap.oficina_digital.cliente.dominio.Endereco;
import br.com.fiap.oficina_digital.cliente.dominio.RepositorioCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServicoCliente")
class ServicoClienteTest {

    @Mock
    RepositorioCliente repositorio;

    @InjectMocks
    ServicoCliente servico;

    private static final String CPF_VALIDO = "529.982.247-25";
    private static final Endereco ENDERECO = new Endereco(
            "01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP");

    private DadosCliente dadosValidos() {
        return new DadosCliente("João Silva", "joao@email.com",
                "(11) 99999-9999", ENDERECO,
                LocalDate.now().minusYears(25).toString());
    }

    private Cliente clienteFake() {
        return Cliente.criar(new Documento(CPF_VALIDO), "João Silva",
                "joao@email.com", "11999999999", ENDERECO,
                LocalDate.now().minusYears(25));
    }

    @Nested
    @DisplayName("cadastrar")
    class Cadastrar {

        @Test
        @DisplayName("deve salvar e retornar cliente quando documento não existir")
        void deveSalvarClienteComDocumentoNovo() {
            var fake = clienteFake();
            when(repositorio.buscarPorDocumento(any())).thenReturn(Optional.empty());
            when(repositorio.salvar(any())).thenReturn(fake);

            var resultado = servico.cadastrar(CPF_VALIDO, dadosValidos());

            assertThat(resultado).isNotNull();
            assertThat(resultado.getNome()).isEqualTo("João Silva");
            verify(repositorio, times(1)).salvar(any());
        }

        @Test
        @DisplayName("deve lançar IllegalStateException quando documento já estiver cadastrado")
        void deveLancarExcecaoSeDocumentoJaExistir() {
            when(repositorio.buscarPorDocumento(any())).thenReturn(Optional.of(clienteFake()));

            assertThatThrownBy(() -> servico.cadastrar(CPF_VALIDO, dadosValidos()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("documento");

            verify(repositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException para data de nascimento malformada")
        void deveLancarExcecaoParaDataMalformada() {
            when(repositorio.buscarPorDocumento(any())).thenReturn(Optional.empty());
            var dadosComDataErrada = new DadosCliente("João Silva", "joao@email.com",
                    "11999999999", ENDERECO, "25/10/2000"); // formato inválido

            assertThatThrownBy(() -> servico.cadastrar(CPF_VALIDO, dadosComDataErrada))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("YYYY-MM-DD");

            verify(repositorio, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar cliente quando ID existir")
        void deveRetornarClienteQuandoIdExistir() {
            var fake = clienteFake();
            when(repositorio.buscarPorId(fake.getId())).thenReturn(Optional.of(fake));

            var resultado = servico.buscarPorId(fake.getId());

            assertThat(resultado.getId()).isEqualTo(fake.getId());
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando ID não existir")
        void deveLancarExcecaoQuandoIdNaoExistir() {
            var idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servico.buscarPorId(idInexistente))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não encontrado");
        }
    }

    @Nested
    @DisplayName("buscarPorDocumento")
    class BuscarPorDocumento {

        @Test
        @DisplayName("deve retornar cliente quando documento existir")
        void deveRetornarClienteQuandoDocumentoExistir() {
            var fake = clienteFake();
            when(repositorio.buscarPorDocumento(any())).thenReturn(Optional.of(fake));

            var resultado = servico.buscarPorDocumento(CPF_VALIDO);

            assertThat(resultado.getDocumento().numero()).isEqualTo("52998224725");
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando documento não existir")
        void deveLancarExcecaoQuandoDocumentoNaoExistir() {
            when(repositorio.buscarPorDocumento(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servico.buscarPorDocumento(CPF_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não encontrado");
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("deve delegar ao repositório com paginação válida")
        void deveDelegarAoRepositorioComPaginacaoValida() {
            when(repositorio.listar(0, 20)).thenReturn(List.of(clienteFake()));

            var resultado = servico.listar(0, 20);

            assertThat(resultado).hasSize(1);
            verify(repositorio, times(1)).listar(0, 20);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException para página negativa")
        void deveLancarExcecaoParaPaginaNegativa() {
            assertThatThrownBy(() -> servico.listar(-1, 20))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Paginação");

            verifyNoInteractions(repositorio);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException para tamanho zero")
        void deveLancarExcecaoParaTamanhoZero() {
            assertThatThrownBy(() -> servico.listar(0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Paginação");

            verifyNoInteractions(repositorio);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException para tamanho acima de 100")
        void deveLancarExcecaoParaTamanhoAcimaDe100() {
            assertThatThrownBy(() -> servico.listar(0, 101))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Paginação");

            verifyNoInteractions(repositorio);
        }

        @Test
        @DisplayName("deve aceitar tamanho exatamente igual a 100")
        void deveAceitarTamanhoIgualA100() {
            when(repositorio.listar(0, 100)).thenReturn(List.of());

            var resultado = servico.listar(0, 100);

            assertThat(resultado).isEmpty();
            verify(repositorio).listar(0, 100);
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        @Test
        @DisplayName("deve carregar, atualizar e salvar o cliente")
        void deveCarregarAtualizarESalvar() {
            var fake = clienteFake();
            when(repositorio.buscarPorId(fake.getId())).thenReturn(Optional.of(fake));
            when(repositorio.salvar(any())).thenReturn(fake);

            var resultado = servico.atualizar(fake.getId(), dadosValidos());

            assertThat(resultado).isNotNull();
            verify(repositorio, times(1)).buscarPorId(fake.getId());
            verify(repositorio, times(1)).salvar(fake);
        }

        @Test
        @DisplayName("deve lançar exceção se o cliente não for encontrado para atualização")
        void deveLancarExcecaoSeClienteNaoEncontrado() {
            var idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servico.atualizar(idInexistente, dadosValidos()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não encontrado");

            verify(repositorio, never()).salvar(any());
        }
    }
}
