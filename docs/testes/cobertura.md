# Cobertura de testes

Gere o relatório HTML com:

```bash
./mvnw verify
```

O relatório fica em `target/site/jacoco/index.html`. A checagem mínima de cobertura de linhas do domínio continua habilitada no Maven; as exclusões abaixo afetam a visualização do relatório, não removem código do artefato nem da aplicação.

## Exclusões do relatório JaCoCo

| Padrão | Motivo |
| --- | --- |
| `**/*Application.class` | Classe de inicialização do Spring Boot; o bootstrap é responsabilidade do framework. |
| `**/Configuracao*.class` | Classes de configuração/wiring do Spring, sem regra de negócio própria. |
| `**/*Jpa.class` | Entidades de persistência passivas, compostas principalmente por campos e metadados JPA. |
| `**/*$*.class` | Tipos aninhados gerados para DTOs/records e estruturas auxiliares; regras de negócio devem ser testadas nas classes externas. |
| `**/cliente/apresentacao/dto/**` | DTOs simples de transporte; validações Bean Validation são declarativas e devem ser verificadas em testes de API quando esses testes forem adicionados. |

Controladores, serviços de aplicação, adaptadores, mapeadores e componentes que contêm comportamento continuam no relatório e devem receber testes. Exclusões devem ser revistas quando uma dessas classes passar a conter lógica relevante. Os testes unitários atuais não substituem testes de integração das restrições de entrada nem da configuração de segurança.
