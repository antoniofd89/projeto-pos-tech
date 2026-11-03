package br.com.fiap.oficina_digital.cliente.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cliente")
class ClienteTest {


    private static final Documento DOCUMENTO = new Documento("529.982.247-25");
    private static final Endereco ENDERECO = new Endereco(
            "01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP");
    private static final LocalDate NASCIMENTO_VALIDO = LocalDate.now().minusYears(25);

    private static Cliente clienteValido() {
        return Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                "(11) 99999-9999", ENDERECO, NASCIMENTO_VALIDO);
    }

    @Nested
    @DisplayName("Criação com sucesso")
    class CriacaoComSucesso {

        @Test
        @DisplayName("deve criar cliente com todos os dados válidos")
        void deveCriarClienteComDadosValidos() {
            var cliente = clienteValido();

            assertThat(cliente.getId()).isNotNull();
            assertThat(cliente.getDataCadastro()).isNotNull();
            assertThat(cliente.getNome()).isEqualTo("João Silva");
            assertThat(cliente.getEmail()).isEqualTo("joao@email.com");
            assertThat(cliente.getDocumento()).isEqualTo(DOCUMENTO);
        }

        @Test
        @DisplayName("duas chamadas a criar() devem gerar IDs diferentes")
        void duasChamadasCriarDevemGerarIdsDiferentes() {
            var c1 = clienteValido();
            var c2 = clienteValido();

            assertThat(c1.getId()).isNotEqualTo(c2.getId());
        }

        @Test
        @DisplayName("deve aceitar nome com exatamente 150 caracteres")
        void deveAceitarNomeComExatamente150Chars() {
            String nome150 = "A".repeat(150);
            var cliente = Cliente.criar(DOCUMENTO, nome150, "joao@email.com",
                    "11999999999", ENDERECO, NASCIMENTO_VALIDO);

            assertThat(cliente.getNome()).hasSize(150);
        }

        @Test
        @DisplayName("deve normalizar e-mail para minúsculo")
        void deveNormalizarEmailParaMinusculo() {
            var cliente = Cliente.criar(DOCUMENTO, "João Silva", "JOAO@EMAIL.COM",
                    "11999999999", ENDERECO, NASCIMENTO_VALIDO);

            assertThat(cliente.getEmail()).isEqualTo("joao@email.com");
        }

        @Test
        @DisplayName("deve normalizar telefone removendo formatação")
        void deveNormalizarTelefoneRemovendoFormatacao() {
            var cliente = Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    "(11) 99999-9999", ENDERECO, NASCIMENTO_VALIDO);

            assertThat(cliente.getTelefone()).isEqualTo("11999999999");
        }

        @Test
        @DisplayName("deve aceitar cliente com exatamente 18 anos")
        void deveAceitarClienteComExatamente18Anos() {
            LocalDate dezoitoAnosOntem = LocalDate.now().minusYears(18).minusDays(1);
            var cliente = Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    "11999999999", ENDERECO, dezoitoAnosOntem);

            assertThat(cliente.getNascimento()).isEqualTo(dezoitoAnosOntem);
        }
    }

    @Nested
    @DisplayName("Validação de nome")
    class ValidacaoDeNome {

        @Test
        @DisplayName("deve rejeitar nome nulo")
        void deveRejeitarNomeNulo() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, null, "joao@email.com",
                    "11999999999", ENDERECO, NASCIMENTO_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nome");
        }

        @Test
        @DisplayName("deve rejeitar nome em branco")
        void deveRejeitarNomeEmBranco() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "   ", "joao@email.com",
                    "11999999999", ENDERECO, NASCIMENTO_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nome");
        }

        @Test
        @DisplayName("deve rejeitar nome com mais de 150 caracteres")
        void deveRejeitarNomeAcimaDe150Chars() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "A".repeat(151), "joao@email.com",
                    "11999999999", ENDERECO, NASCIMENTO_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nome");
        }
    }

    @Nested
    @DisplayName("Validação de e-mail")
    class ValidacaoDeEmail {

        @Test
        @DisplayName("deve rejeitar e-mail nulo")
        void deveRejeitarEmailNulo() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", null,
                    "11999999999", ENDERECO, NASCIMENTO_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("E-mail");
        }

        @Test
        @DisplayName("deve rejeitar e-mail sem @")
        void deveRejeitarEmailSemArroba() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "joaoemail.com",
                    "11999999999", ENDERECO, NASCIMENTO_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("E-mail");
        }

        @Test
        @DisplayName("deve rejeitar e-mail sem domínio após @")
        void deveRejeitarEmailSemDominio() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "joao@",
                    "11999999999", ENDERECO, NASCIMENTO_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("E-mail");
        }

        @Test
        @DisplayName("deve rejeitar e-mail em branco")
        void deveRejeitarEmailEmBranco() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "   ",
                    "11999999999", ENDERECO, NASCIMENTO_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("E-mail");
        }
    }

    @Nested
    @DisplayName("Validação de telefone")
    class ValidacaoDeTelefone {

        @Test
        @DisplayName("deve rejeitar telefone nulo")
        void deveRejeitarTelefoneNulo() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    null, ENDERECO, NASCIMENTO_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Telefone");
        }

        @Test
        @DisplayName("deve rejeitar telefone com menos de 10 dígitos após limpeza")
        void deveRejeitarTelefoneComMenosDe10Digitos() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    "119999999", ENDERECO, NASCIMENTO_VALIDO))  // 9 dígitos
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Telefone");
        }

        @Test
        @DisplayName("deve rejeitar telefone com mais de 15 dígitos após limpeza")
        void deveRejeitarTelefoneComMaisDe15Digitos() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    "1199999999999999", ENDERECO, NASCIMENTO_VALIDO))  // 16 dígitos
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Telefone");
        }

        @Test
        @DisplayName("deve rejeitar telefone em branco")
        void deveRejeitarTelefoneEmBranco() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    "   ", ENDERECO, NASCIMENTO_VALIDO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Telefone");
        }
    }

    @Nested
    @DisplayName("Validação de nascimento")
    class ValidacaoDeNascimento {

        @Test
        @DisplayName("deve rejeitar nascimento nulo")
        void deveRejeitarNascimentoNulo() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    "11999999999", ENDERECO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nascimento");
        }

        @Test
        @DisplayName("deve rejeitar nascimento no futuro")
        void deveRejeitarNascimentoNoFuturo() {
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    "11999999999", ENDERECO, LocalDate.now().plusDays(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("futuro");
        }

        @Test
        @DisplayName("deve rejeitar cliente com menos de 18 anos")
        void deveRejeitarClienteComMenosDe18Anos() {
            LocalDate dezesseteAnos = LocalDate.now().minusYears(17);
            assertThatThrownBy(() -> Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    "11999999999", ENDERECO, dezesseteAnos))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("18 anos");
        }

        @Test
        @DisplayName("deve aceitar cliente que completa 18 anos exatamente hoje")
        void deveAceitarClienteQueCompleta18AnosHoje() {
            // isAfter é estritamente maior que: nascido exatamente hoje - 18 anos
            // NÃO é after de si mesmo → regra de negócio permite
            LocalDate dezoitoAnosHoje = LocalDate.now().minusYears(18);
            var cliente = Cliente.criar(DOCUMENTO, "João Silva", "joao@email.com",
                    "11999999999", ENDERECO, dezoitoAnosHoje);

            assertThat(cliente.getNascimento()).isEqualTo(dezoitoAnosHoje);
        }
    }

    @Nested
    @DisplayName("Atualização de dados")
    class AtualizacaoDeDados {

        @Test
        @DisplayName("deve atualizar dados válidos e refletir nos getters")
        void deveAtualizarDadosValidos() {
            var cliente = clienteValido();

            cliente.atualizarDados("Maria Souza", "maria@email.com",
                    "21988887777", ENDERECO, LocalDate.now().minusYears(30));

            assertThat(cliente.getNome()).isEqualTo("Maria Souza");
            assertThat(cliente.getEmail()).isEqualTo("maria@email.com");
            assertThat(cliente.getTelefone()).isEqualTo("21988887777");
        }

        @Test
        @DisplayName("deve manter ID e data de cadastro inalterados após atualização")
        void deveManterIdEDataCadastroAposAtualizacao() {
            var cliente = clienteValido();
            var idOriginal = cliente.getId();
            var dataCadastroOriginal = cliente.getDataCadastro();

            cliente.atualizarDados("Maria Souza", "maria@email.com",
                    "21988887777", ENDERECO, LocalDate.now().minusYears(30));

            assertThat(cliente.getId()).isEqualTo(idOriginal);
            assertThat(cliente.getDataCadastro()).isEqualTo(dataCadastroOriginal);
        }

        @Test
        @DisplayName("deve rejeitar atualização com nome inválido e preservar estado anterior")
        void deveRejeitarAtualizacaoComNomeInvalidoEPreservarEstado() {
            var cliente = clienteValido();
            var nomeAntes = cliente.getNome();

            assertThatThrownBy(() ->
                    cliente.atualizarDados(null, "maria@email.com",
                            "21988887777", ENDERECO, LocalDate.now().minusYears(30)))
                    .isInstanceOf(IllegalArgumentException.class);

            // Estado deve ser o mesmo de antes
            assertThat(cliente.getNome()).isEqualTo(nomeAntes);
        }
    }

    @Nested
    @DisplayName("Ativação e documento de pessoa jurídica")
    class AtivacaoEDocumentoPessoaJuridica {

        @Test
        @DisplayName("deve reativar cliente inativo e impedir transições repetidas")
        void deveReativarClienteInativo() {
            var cliente = clienteValido();
            assertThat(cliente.isAtivo()).isTrue();

            cliente.inativar();
            assertThat(cliente.isAtivo()).isFalse();
            assertThatThrownBy(() -> cliente.atualizarDados("Maria Souza", "maria@email.com",
                    "21988887777", ENDERECO, LocalDate.now().minusYears(30)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("inativo");
            assertThatThrownBy(cliente::inativar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já está inativo");

            assertThat(cliente.ativar()).isSameAs(cliente);
            assertThat(cliente.isAtivo()).isTrue();
            assertThatThrownBy(cliente::ativar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já está ativo");
        }

        @Test
        @DisplayName("deve rejeitar data de nascimento para CNPJ")
        void deveRejeitarNascimentoParaCnpj() {
            assertThatThrownBy(() -> Cliente.criar(new Documento("11222333000181"), "Oficina Exemplo",
                    "oficina@exemplo.com", "1133334444", null, LocalDate.now().minusYears(30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não se aplica a CNPJ");
        }
    }

    @Nested
    @DisplayName("Reconstituição")
    class Reconstituicao {

        @Test
        @DisplayName("reconstituir deve preservar o ID fornecido")
        void devePreservarIdFornecido() {
            var idEsperado = UUID.randomUUID();
            var cliente = Cliente.reconstituir(idEsperado, DOCUMENTO, "João Silva",
                    "joao@email.com", "11999999999", ENDERECO,
                    NASCIMENTO_VALIDO, java.time.Instant.now());

            assertThat(cliente.getId()).isEqualTo(idEsperado);
        }

        @Test
        @DisplayName("reconstituir com ID diferente deve retornar ID diferente de criar()")
        void reconstituirDeveUsarIdFornecidoNaoCriarNovo() {
            var idFixo = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var clienteReconstituido = Cliente.reconstituir(idFixo, DOCUMENTO, "João Silva",
                    "joao@email.com", "11999999999", ENDERECO,
                    NASCIMENTO_VALIDO, java.time.Instant.now());

            var clienteNovo = clienteValido();

            assertThat(clienteReconstituido.getId()).isEqualTo(idFixo);
            assertThat(clienteNovo.getId()).isNotEqualTo(idFixo);
        }
    }
}
