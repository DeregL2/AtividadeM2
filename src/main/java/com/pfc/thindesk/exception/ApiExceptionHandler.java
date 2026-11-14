package com.pfc.thindesk.exception;

import com.pfc.thindesk.controller.ChamadoController;
import com.pfc.thindesk.controller.DashboardController;
import com.pfc.thindesk.dto.ErroDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {DashboardController.class, ChamadoController.class})
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ChamadoNaoEncontradoException.class)
    public ResponseEntity<ErroDTO> naoEncontrado(ChamadoNaoEncontradoException e, HttpServletRequest request) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroDTO> argumentoInvalido(IllegalArgumentException e, HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErroDTO> parametroInvalido(BindException e, HttpServletRequest request) {
        FieldError campo = e.getFieldError();
        String mensagem = campo == null
                ? "Parâmetros inválidos"
                : "Valor inválido para o parâmetro " + campo.getField() + ": " + campo.getRejectedValue()
                        + " (use o formato yyyy-MM-dd para datas)";
        return erro(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroDTO> tipoInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro " + e.getName() + ": " + e.getValue(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroDTO> corpoInvalido(HttpMessageNotReadableException e, HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErroDTO> bancoIndisponivel(DataAccessException e, HttpServletRequest request) {
        log.error("Erro de acesso ao banco de dados", e);
        return erro(HttpStatus.SERVICE_UNAVAILABLE, "Banco de dados indisponível no momento", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroDTO> erroInterno(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado na API", e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição", request);
    }

    private ResponseEntity<ErroDTO> erro(HttpStatus status, String mensagem, HttpServletRequest request) {
        ErroDTO corpo = new ErroDTO(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensagem,
                request.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}