package br.com.fiap.oficina_digital.comum.infraestrutura;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratadorErrosApi {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail tratarValidacao(MethodArgumentNotValidException erro, HttpServletRequest requisicao) {
        String campos = erro.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos", campos, requisicao);
    }

    @ExceptionHandler({ IllegalArgumentException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class })
    ProblemDetail tratarRequisicaoInvalida(Exception erro, HttpServletRequest requisicao) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", erro.getMessage(), requisicao);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail tratarCredencial(BadCredentialsException erro, HttpServletRequest requisicao) {
        return problema(HttpStatus.UNAUTHORIZED, "Autenticação recusada", "E-mail ou senha inválidos.", requisicao);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail tratarAcessoNegado(AccessDeniedException erro, HttpServletRequest requisicao) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil não tem permissão para esta operação.",
                requisicao);
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail tratarConflito(IllegalStateException erro, HttpServletRequest requisicao) {
        return problema(HttpStatus.CONFLICT, "Conflito de estado", erro.getMessage(), requisicao);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail tratarIntegridade(DataIntegrityViolationException erro, HttpServletRequest requisicao) {
        return problema(HttpStatus.CONFLICT, "Conflito de dados",
                "Um registro com os mesmos dados já existe ou viola uma regra.", requisicao);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    ProblemDetail tratarNaoEncontrado(Exception erro, HttpServletRequest requisicao) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", erro.getMessage(), requisicao);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail tratarErroInterno(Exception erro, HttpServletRequest requisicao) {
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado.", requisicao);
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalhe, HttpServletRequest requisicao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status,
                detalhe == null ? "Requisição inválida." : detalhe);
        problema.setTitle(titulo);
        problema.setProperty("instante", Instant.now());
        problema.setProperty("caminho", requisicao.getRequestURI());
        return problema;
    }
}
