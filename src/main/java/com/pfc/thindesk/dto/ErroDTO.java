package com.pfc.thindesk.dto;

import java.time.LocalDateTime;

public record ErroDTO(LocalDateTime dataHora, int status, String erro, String mensagem, String caminho) {
}