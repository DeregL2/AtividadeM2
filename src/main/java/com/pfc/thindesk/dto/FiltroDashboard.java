package com.pfc.thindesk.dto;

import com.pfc.thindesk.entity.Prioridade;
import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.util.Textos;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record FiltroDashboard(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
        String status,
        String tipo,
        String tecnico,
        String prioridade) {

    public static FiltroDashboard vazio() {
        return new FiltroDashboard(null, null, null, null, null, null);
    }

    public FiltroDashboard normalizado() {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A dataInicio não pode ser posterior à dataFim");
        }
        return new FiltroDashboard(
                dataInicio,
                dataFim,
                Textos.vazio(status) ? null : StatusChamado.de(status).name(),
                Textos.vazio(tipo) ? null : tipo.trim(),
                Textos.vazio(tecnico) ? null : tecnico.trim(),
                Textos.vazio(prioridade) ? null : Prioridade.de(prioridade).name());
    }
}