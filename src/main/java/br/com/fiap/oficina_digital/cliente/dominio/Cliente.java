package br.com.fiap.oficina_digital.cliente.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Cliente {
    private final UUID id;
    private final Documento documento;
    private String nome;
    private String email;
    private String telefone;
    private Endereco endereco;
    private LocalDate nascimento;
    private Instant dataCadastro;

    private Cliente(UUID id, Documento documento, String nome, String email, String telefone,
                    Endereco endereco, LocalDate nascimento, Instant dataCadastro) {
       this.id = Objects.requireNonNull(id);
       this.documento = Objects.requireNonNull(documento);
       definirDados(nome, email, telefone, endereco, nascimento);
       this.dataCadastro = Objects.requireNonNull(dataCadastro);
    }

    private void definirDados(String nome, String email, String telefone, Endereco endereco, LocalDate nascimento) {
        if (nome == null || nome.isBlank() || nome.trim().length() > 150)
            throw new IllegalArgumentException("Nome obrigatório, com até 150 caracteres.");
        
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("E-mail obrigatório.");
        String novoEmail = email.trim().toLowerCase(Locale.ROOT);
        if (novoEmail.length() > 254 || !novoEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("E-mail inválido.");
        
        if (telefone == null || telefone.isBlank())
            throw new IllegalArgumentException("Telefone obrigatório.");
        String novoTelefone = telefone.trim().replaceAll("[() +\\-]", "");
        if (!novoTelefone.matches("[0-9]{10,15}"))
            throw new IllegalArgumentException("Telefone deve conter de 10 a 15 dígitos.");

        if (nascimento == null) throw new IllegalArgumentException("Nascimento obrigatório.");

        if (nascimento.isAfter(LocalDate.now())) throw new IllegalArgumentException("Nascimento não pode ser no futuro.");

        if (nascimento.isAfter(LocalDate.now().minusYears(18)))
            throw new IllegalArgumentException("Cliente deve ter pelo menos 18 anos.");
        // Validar tudo antes de alterar o objeto.
        this.nome = nome.trim(); 
        this.email = novoEmail;
        this.telefone = novoTelefone;
        this.endereco = endereco;
        this.nascimento = nascimento;
    }

    public static Cliente criar(Documento documento, String nome, String email, String telefone,
                                Endereco endereco, LocalDate nascimento) {
        return new Cliente(UUID.randomUUID(), documento, nome, email, telefone, endereco,
                nascimento, Instant.now());
    }

    public void atualizarDados(String nome, String email, String telefone, Endereco endereco,
                               LocalDate nascimento) {
        definirDados(nome, email, telefone, endereco, nascimento);
    }
        public static Cliente reconstituir(UUID id, Documento documento, String nome,
            String email, String telefone, Endereco endereco, LocalDate nascimento, Instant dataCadastro) {
        return new Cliente(id, documento, nome, email, telefone, endereco, nascimento, dataCadastro);
    }

    public UUID getId() { return id; }
    public Documento getDocumento() { return documento; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public Endereco getEndereco() { return endereco; }
    public LocalDate getNascimento() { return nascimento; }
    public Instant getDataCadastro() { return dataCadastro; }

}
