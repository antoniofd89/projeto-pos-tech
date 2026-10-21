package br.com.fiap.oficina_digital.cliente.dominio;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    void cpfComExatamenteDezoitoAnosNaoAtendeRegraDeMaioridade() {
        var nascimento = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusYears(18);
        assertThrows(IllegalArgumentException.class, () -> Cliente.criar(
                new Documento("52998224725"), "Maria da Silva", "maria@example.com", "11987654321", null, nascimento));
    }

    @Test
    void normalizaEmailETelefone() {
        var cliente = Cliente.criar(new Documento("11222333000181"), "Oficina Exemplo", "  CONTATO@EXAMPLE.COM ",
                "+55 (11) 98765-4321", null, null);
        assertEquals("contato@example.com", cliente.getEmail());
        assertEquals("5511987654321", cliente.getTelefone());
    }
    private Documento cpf(){return new Documento("52998224725");}
    @Test void criaPessoaFisicaMaiorDeDezoito() { var c=Cliente.criar(cpf()," Ana ","ANA@EXEMPLO.COM","(11) 99999-9999",null,LocalDate.of(1990,1,1));assertEquals("ana@exemplo.com",c.getEmail());assertEquals("11999999999",c.getTelefone());assertNotNull(c.getDataCadastro()); }
    @Test void cnpjNaoExigeNascimento() { var c=Cliente.criar(new Documento("11222333000181"),"Oficina Exemplo Ltda","contato@oficina.com","1133334444",null,null);assertNull(c.getNascimento()); }
    @Test void cpfExigeNascimento() { assertThrows(IllegalArgumentException.class,()->Cliente.criar(cpf(),"Ana","ana@exemplo.com","11999999999",null,null)); }
    @Test void rejeitaPessoaComExatamenteDezoitoAnos() { assertThrows(IllegalArgumentException.class,()->Cliente.criar(cpf(),"Ana","ana@exemplo.com","11999999999",null,LocalDate.now().minusYears(18))); }
    @Test void rejeitaNascimentoFuturo() { assertThrows(IllegalArgumentException.class,()->Cliente.criar(cpf(),"Ana","ana@exemplo.com","11999999999",null,LocalDate.now().plusDays(1))); }
    @Test void rejeitaEmailVazio() { assertThrows(IllegalArgumentException.class,()->Cliente.criar(cpf(),"Ana"," ","11999999999",null,LocalDate.of(1990,1,1))); }
    @Test void rejeitaEmailInvalido() { assertThrows(IllegalArgumentException.class,()->Cliente.criar(cpf(),"Ana","invalido","11999999999",null,LocalDate.of(1990,1,1))); }
    @Test void rejeitaTelefoneVazio() { assertThrows(IllegalArgumentException.class,()->Cliente.criar(cpf(),"Ana","ana@exemplo.com"," ",null,LocalDate.of(1990,1,1))); }
    @Test void rejeitaTelefoneCurto() { assertThrows(IllegalArgumentException.class,()->Cliente.criar(cpf(),"Ana","ana@exemplo.com","1234",null,LocalDate.of(1990,1,1))); }
    @Test void rejeitaNomeVazio() { assertThrows(IllegalArgumentException.class,()->Cliente.criar(cpf()," ","ana@exemplo.com","11999999999",null,LocalDate.of(1990,1,1))); }
    @Test void atualizaDadosEInativaUmaVez() { var c=Cliente.criar(cpf(),"Ana","ana@exemplo.com","11999999999",null,LocalDate.of(1990,1,1));c.atualizarDados("Ana Silva","ana.silva@exemplo.com","11988887777",null,LocalDate.of(1990,1,1));assertEquals("Ana Silva",c.getNome());c.inativar();assertFalse(c.isAtivo());assertThrows(IllegalStateException.class,c::inativar);assertThrows(IllegalStateException.class,()->c.atualizarDados("Ana","ana@exemplo.com","11999999999",null,LocalDate.of(1990,1,1))); }
    @Test void rejeitaNomeAcimaDoLimite() { assertThrows(IllegalArgumentException.class,()->Cliente.criar(cpf(),"n".repeat(151),"ana@exemplo.com","11999999999",null,LocalDate.of(1990,1,1))); }
}
