package br.com.fiap.oficina_digital.identidade.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.fiap.oficina_digital.identidade.dominio.PerfilAcesso;
import br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia.RepositorioUsuarioJpa;
import br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia.UsuarioAdministrativoJpa;

class ProvisionarAdministradorInicialTest {

    private final RepositorioUsuarioJpa usuarios = mock(RepositorioUsuarioJpa.class);
    private final PasswordEncoder codificador = mock(PasswordEncoder.class);
    private final ProvisionarAdministradorInicial provisionador =
            new ProvisionarAdministradorInicial(usuarios, codificador);
    private final ApplicationArguments argumentos = mock(ApplicationArguments.class);

    @Test
    void naoCriaAdministradorQuandoConfiguracaoEstaVazia() {
        configurar(" ", " ");

        provisionador.run(argumentos);

        verify(usuarios, never()).save(any());
        verify(codificador, never()).encode(any());
    }

    @Test
    void rejeitaConfiguracaoParcialOuSenhaCurta() {
        configurar("admin@oficina.com", "curta");

        assertThatThrownBy(() -> provisionador.run(argumentos))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ao menos 12 caracteres");
    }

    @Test
    void naoDuplicaAdministradorJaExistente() {
        configurar("admin@oficina.com", "senha-longa-segura");
        when(usuarios.existsByEmailIgnoreCase("admin@oficina.com")).thenReturn(true);

        provisionador.run(argumentos);

        verify(usuarios, never()).save(any());
        verify(codificador, never()).encode(any());
    }

    @Test
    void criaAdministradorComEmailNormalizadoESenhaCodificada() {
        configurar(" Admin@Oficina.com ", "senha-longa-segura");
        when(usuarios.existsByEmailIgnoreCase(" Admin@Oficina.com ")).thenReturn(false);
        when(codificador.encode("senha-longa-segura")).thenReturn("hash-seguro");
        when(usuarios.save(any(UsuarioAdministrativoJpa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        provisionador.run(argumentos);

        org.mockito.ArgumentCaptor<UsuarioAdministrativoJpa> capturador =
                org.mockito.ArgumentCaptor.forClass(UsuarioAdministrativoJpa.class);
        verify(usuarios).save(capturador.capture());
        var salvo = capturador.getValue();
        assertThat(salvo.email).isEqualTo("admin@oficina.com");
        assertThat(salvo.senhaHash).isEqualTo("hash-seguro");
        assertThat(salvo.perfil).isEqualTo(PerfilAcesso.ADMIN);
        assertThat(salvo.ativo).isTrue();
        assertThat(salvo.criadoEm).isNotNull();
    }

    private void configurar(String email, String senha) {
        ReflectionTestUtils.setField(provisionador, "email", email);
        ReflectionTestUtils.setField(provisionador, "senha", senha);
    }
}
