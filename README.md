# Oficina Digital

Tech Challenge - Sistema Integrado de Atendimento e Execução de Serviços.

Projeto base da Oficina Digital, desenvolvido com Spring Boot e Maven.

## Tecnologias

- Java 25
- Spring Boot 4.1.1
- Spring Web MVC
- Maven Wrapper

## Pré-requisitos

- JDK 25 ou superior compatível com a configuração do projeto
- Não é necessário instalar o Maven: o projeto inclui o Maven Wrapper

Verifique a versão do Java:

```bash
java -version
```

## Executando localmente

Clone o projeto e entre na pasta:

```bash
git clone https://github.com/antoniofd89/projeto-pos-tech.git
cd projeto-pos-tech
```

Inicie a aplicação com o Maven Wrapper:

```bash
./mvnw spring-boot:run
```

No Windows, use:

```bat
mvnw.cmd spring-boot:run
```

Por padrão, a aplicação é iniciada na porta `8080`.

## Testes

Execute os testes automatizados com:

```bash
./mvnw test
```

## Estrutura principal

```text
src/
├── main/
│   ├── java/       # Código-fonte da aplicação
│   └── resources/  # Configurações e recursos
└── test/           # Testes automatizados
```

## Projeto relacionado

O repositório remoto deste projeto está disponível em:

<https://github.com/antoniofd89/projeto-pos-tech>
