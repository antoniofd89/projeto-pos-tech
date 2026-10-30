package br.com.fiap.oficina_digital.cliente.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Documento")
class DocumentoTest {

    @Nested
    @DisplayName("CPF")
    class Cpf {

        @Test
        @DisplayName("deve aceitar CPF válido com máscara e armazenar somente dígitos")
        void deveAceitarCpfValidoComMascara() {
            var doc = new Documento("529.982.247-25");

            assertThat(doc.numero()).isEqualTo("52998224725");
        }

        @Test
        @DisplayName("deve aceitar CPF válido sem máscara")
        void deveAceitarCpfValidoSemMascara() {
            var doc = new Documento("52998224725");

            assertThat(doc.numero()).isEqualTo("52998224725");
        }

        @Test
        @DisplayName("deve rejeitar CPF com dígito verificador errado")
        void deveRejeitarCpfComDigitoVerificadorErrado() {
            assertThatThrownBy(() -> new Documento("529.982.247-26"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");
        }

        @ParameterizedTest(name = "CPF com todos os dígitos iguais: {0}")
        @DisplayName("deve rejeitar CPF com todos os dígitos iguais")
        @ValueSource(strings = {
                "000.000.000-00",
                "111.111.111-11",
                "999.999.999-99"
        })
        void deveRejeitarCpfComTodosOsDigitosIguais(String cpf) {
            assertThatThrownBy(() -> new Documento(cpf))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");
        }
    }

    @Nested
    @DisplayName("CNPJ")
    class Cnpj {

        @Test
        @DisplayName("deve aceitar CNPJ válido com máscara e armazenar somente dígitos")
        void deveAceitarCnpjValidoComMascara() {
            var doc = new Documento("11.222.333/0001-81");

            assertThat(doc.numero()).isEqualTo("11222333000181");
        }

        @Test
        @DisplayName("deve aceitar CNPJ válido sem máscara")
        void deveAceitarCnpjValidoSemMascara() {
            var doc = new Documento("11222333000181");

            assertThat(doc.numero()).isEqualTo("11222333000181");
        }

        @Test
        @DisplayName("deve rejeitar CNPJ com dígito verificador errado")
        void deveRejeitarCnpjComDigitoVerificadorErrado() {
            assertThatThrownBy(() -> new Documento("11.222.333/0001-82"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");
        }

        @ParameterizedTest(name = "CNPJ com todos os dígitos iguais: {0}")
        @DisplayName("deve rejeitar CNPJ com todos os dígitos iguais")
        @ValueSource(strings = {
                "00.000.000/0000-00",
                "11.111.111/1111-11"
        })
        void deveRejeitarCnpjComTodosOsDigitosIguais(String cnpj) {
            assertThatThrownBy(() -> new Documento(cnpj))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");
        }
    }

    @Nested
    @DisplayName("Entradas inválidas")
    class EntradasInvalidas {

        @Test
        @DisplayName("deve rejeitar número nulo")
        void deveRejeitarNulo() {
            assertThatThrownBy(() -> new Documento(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve rejeitar string vazia")
        void deveRejeitarStringVazia() {
            assertThatThrownBy(() -> new Documento(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve rejeitar string em branco (apenas espaços)")
        void deveRejeitarStringEmBranco() {
            assertThatThrownBy(() -> new Documento("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve rejeitar tamanho inválido (nem CPF nem CNPJ)")
        void deveRejeitarTamanhoInvalido() {
            assertThatThrownBy(() -> new Documento("123456789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");
        }

        @Test
        @DisplayName("deve rejeitar número com letras após remoção da máscara")
        void deveRejeitarNumeroComLetras() {
            // após remover . / - , "529.9AB.247-25" vira "5299AB24725" → não numérico
            assertThatThrownBy(() -> new Documento("529.9AB.247-25"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");
        }
    }

    @Nested
    @DisplayName("Contrato do record")
    class ContratoDoRecord {

        @Test
        @DisplayName("dois Documentos com mesmo número devem ser iguais (equals/hashCode)")
        void doisDocumentosComMesmoNumeroDevemSerIguais() {
            var doc1 = new Documento("529.982.247-25");
            var doc2 = new Documento("52998224725");

            assertThat(doc1).isEqualTo(doc2);
            assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode());
        }

        @Test
        @DisplayName("dois Documentos com números diferentes não devem ser iguais")
        void doisDocumentosComNumerosDiferentesNaoDevemSerIguais() {
            var doc1 = new Documento("529.982.247-25");   // CPF válido
            var doc2 = new Documento("11.222.333/0001-81"); // CNPJ válido

            assertThat(doc1).isNotEqualTo(doc2);
        }
    }
}
