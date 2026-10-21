package br.com.fiap.oficina_digital.cliente.dominio;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EnderecoTest {
    @Test void normalizaCepUfEComplemento() { var e=new Endereco("01310-100"," Paulista ","100", " ","Bela Vista","São Paulo","sp");assertEquals("01310100",e.cep());assertEquals("SP",e.uf());assertNull(e.complemento()); }
    @Test void rejeitaCepAusente() { assertThrows(IllegalArgumentException.class,()->new Endereco(null,"Rua","1",null,"Bairro","Cidade","SP")); }
    @Test void rejeitaCepInvalido() { assertThrows(IllegalArgumentException.class,()->new Endereco("abc","Rua","1",null,"Bairro","Cidade","SP")); }
    @Test void rejeitaUfInvalida() { assertThrows(IllegalArgumentException.class,()->new Endereco("01001000","Rua","1",null,"Bairro","Cidade","XX")); }
    @Test void rejeitaCamposObrigatoriosVazios() { assertThrows(IllegalArgumentException.class,()->new Endereco("01001000"," ","1",null,"Bairro","Cidade","SP")); }
    @Test void rejeitaComplementoMuitoLongo() { assertThrows(IllegalArgumentException.class,()->new Endereco("01001000","Rua","1","x".repeat(101),"Bairro","Cidade","SP")); }
}
