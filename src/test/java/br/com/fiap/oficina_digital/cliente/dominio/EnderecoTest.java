package br.com.fiap.oficina_digital.cliente.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Endereco")
class EnderecoTest {


    private static Endereco enderecoValido() {
        return new Endereco("01310-100", "Av. Paulista", "1000",
                null, "Bela Vista", "São Paulo", "SP");
    }


    @Nested
    @DisplayName("CEP")
    class Cep {

        @Test
        @DisplayName("deve aceitar CEP válido sem traço e armazenar somente dígitos")
        void deveAceitarCepSemTraco() {
            var e = new Endereco("01310100", "Av. Paulista", "1000",
                    null, "Bela Vista", "São Paulo", "SP");

            assertThat(e.cep()).isEqualTo("01310100");
        }

        @Test
        @DisplayName("deve aceitar CEP válido com traço e normalizar para somente dígitos")
        void deveAceitarCepComTracoENormalizar() {
            var e = enderecoValido();

            assertThat(e.cep()).isEqualTo("01310100");
        }

        @Test
        @DisplayName("deve rejeitar CEP nulo")
        void deveRejeitarCepNulo() {
            assertThatThrownBy(() -> new Endereco(null, "Av. Paulista", "1000",
                    null, "Bela Vista", "São Paulo", "SP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CEP");
        }

        @ParameterizedTest(name = "CEP inválido: \"{0}\"")
        @DisplayName("deve rejeitar CEP com formato inválido")
        @ValueSource(strings = {
                "0131010",       // 7 dígitos
                "013101000",     // 9 dígitos
                "0131010A",      // letra
                ""               // vazio
        })
        void deveRejeitarCepInvalido(String cep) {
            assertThatThrownBy(() -> new Endereco(cep, "Av. Paulista", "1000",
                    null, "Bela Vista", "São Paulo", "SP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CEP");
        }
    }


    @Nested
    @DisplayName("UF")
    class Uf {

        @Test
        @DisplayName("deve aceitar UF válida em maiúsculo")
        void deveAceitarUfValidaMaiuscula() {
            var e = enderecoValido();

            assertThat(e.uf()).isEqualTo("SP");
        }

        @Test
        @DisplayName("deve aceitar UF em minúsculo e normalizar para maiúsculo")
        void deveAceitarUfMinusculaENormalizar() {
            var e = new Endereco("01310100", "Av. Paulista", "1000",
                    null, "Bela Vista", "São Paulo", "sp");

            assertThat(e.uf()).isEqualTo("SP");
        }

        @ParameterizedTest(name = "UF inválida: \"{0}\"")
        @DisplayName("deve rejeitar UF inexistente no Brasil")
        @ValueSource(strings = {"XX", "ZZ", "AB", "BB"})
        void deveRejeitarUfInexistente(String uf) {
            assertThatThrownBy(() -> new Endereco("01310100", "Av. Paulista", "1000",
                    null, "Bela Vista", "São Paulo", uf))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("UF");
        }

        @Test
        @DisplayName("deve rejeitar UF nula")
        void deveRejeitarUfNula() {
            assertThatThrownBy(() -> new Endereco("01310100", "Av. Paulista", "1000",
                    null, "Bela Vista", "São Paulo", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }


    @Nested
    @DisplayName("Complemento")
    class Complemento {

        @Test
        @DisplayName("deve aceitar complemento nulo (campo opcional)")
        void deveAceitarComplementoNulo() {
            var e = new Endereco("01310100", "Av. Paulista", "1000",
                    null, "Bela Vista", "São Paulo", "SP");

            assertThat(e.complemento()).isNull();
        }

        @Test
        @DisplayName("deve aceitar complemento em branco e tratar como nulo")
        void deveAceitarComplementoEmBrancoComoNulo() {
            var e = new Endereco("01310100", "Av. Paulista", "1000",
                    "   ", "Bela Vista", "São Paulo", "SP");

            assertThat(e.complemento()).isNull();
        }

        @Test
        @DisplayName("deve aceitar complemento válido e remover espaços nas bordas")
        void deveAceitarComplementoValido() {
            var e = new Endereco("01310100", "Av. Paulista", "1000",
                    "  Apto 42  ", "Bela Vista", "São Paulo", "SP");

            assertThat(e.complemento()).isEqualTo("Apto 42");
        }

        @Test
        @DisplayName("deve rejeitar complemento com mais de 100 caracteres")
        void deveRejeitarComplementoAcimaDe100Chars() {
            String complementoLongo = "A".repeat(101);
            assertThatThrownBy(() -> new Endereco("01310100", "Av. Paulista", "1000",
                    complementoLongo, "Bela Vista", "São Paulo", "SP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Complemento");
        }

        @Test
        @DisplayName("deve aceitar complemento com exatamente 100 caracteres")
        void deveAceitarComplementoComExatamente100Chars() {
            String complemento100 = "A".repeat(100);
            var e = new Endereco("01310100", "Av. Paulista", "1000",
                    complemento100, "Bela Vista", "São Paulo", "SP");

            assertThat(e.complemento()).hasSize(100);
        }
    }

    @Nested
    @DisplayName("Campos obrigatórios de texto")
    class CamposObrigatorios {

        @Test
        @DisplayName("deve criar endereço completo e válido com sucesso")
        void deveCriarEnderecoCompletoComSucesso() {
            var e = new Endereco("01310-100", "Av. Paulista", "1000",
                    "Apto 42", "Bela Vista", "São Paulo", "SP");

            assertThat(e.logradouro()).isEqualTo("Av. Paulista");
            assertThat(e.numero()).isEqualTo("1000");
            assertThat(e.bairro()).isEqualTo("Bela Vista");
            assertThat(e.cidade()).isEqualTo("São Paulo");
            assertThat(e.complemento()).isEqualTo("Apto 42");
        }

        @Test
        @DisplayName("deve rejeitar logradouro nulo")
        void deveRejeitarLogradouroNulo() {
            assertThatThrownBy(() -> new Endereco("01310100", null, "1000",
                    null, "Bela Vista", "São Paulo", "SP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Logradouro");
        }

        @Test
        @DisplayName("deve rejeitar logradouro em branco")
        void deveRejeitarLogradouroEmBranco() {
            assertThatThrownBy(() -> new Endereco("01310100", "   ", "1000",
                    null, "Bela Vista", "São Paulo", "SP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Logradouro");
        }

        @Test
        @DisplayName("deve rejeitar logradouro com mais de 200 caracteres")
        void deveRejeitarLogradouroAcimaDe200Chars() {
            assertThatThrownBy(() -> new Endereco("01310100", "A".repeat(201), "1000",
                    null, "Bela Vista", "São Paulo", "SP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Logradouro");
        }

        @Test
        @DisplayName("deve rejeitar número nulo")
        void deveRejeitarNumeroNulo() {
            assertThatThrownBy(() -> new Endereco("01310100", "Av. Paulista", null,
                    null, "Bela Vista", "São Paulo", "SP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Numero");
        }

        @Test
        @DisplayName("deve rejeitar bairro em branco")
        void deveRejeitarBairroEmBranco() {
            assertThatThrownBy(() -> new Endereco("01310100", "Av. Paulista", "1000",
                    null, "  ", "São Paulo", "SP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bairro");
        }

        @Test
        @DisplayName("deve rejeitar cidade nula")
        void deveRejeitarCidadeNula() {
            assertThatThrownBy(() -> new Endereco("01310100", "Av. Paulista", "1000",
                    null, "Bela Vista", null, "SP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cidade");
        }

        @Test
        @DisplayName("deve remover espaços nas bordas do logradouro")
        void deveRemoverEspacosNasBordasDoLogradouro() {
            var e = new Endereco("01310100", "  Av. Paulista  ", "1000",
                    null, "Bela Vista", "São Paulo", "SP");

            assertThat(e.logradouro()).isEqualTo("Av. Paulista");
        }
    }

    @Nested
    @DisplayName("Contrato do record")
    class ContratoDoRecord {

        @Test
        @DisplayName("dois endereços com os mesmos valores devem ser iguais (equals/hashCode)")
        void doisEnderecosMesmosValoresDevemSerIguais() {
            var e1 = enderecoValido();
            var e2 = enderecoValido();

            assertThat(e1).isEqualTo(e2);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }

        @Test
        @DisplayName("dois endereços com cidades diferentes não devem ser iguais")
        void doisEnderecosCidadesDiferentesNaoDevemSerIguais() {
            var e1 = enderecoValido(); // São Paulo/SP
            var e2 = new Endereco("20040-020", "Av. Rio Branco", "1",
                    null, "Centro", "Rio de Janeiro", "RJ");

            assertThat(e1).isNotEqualTo(e2);
        }
    }
}
