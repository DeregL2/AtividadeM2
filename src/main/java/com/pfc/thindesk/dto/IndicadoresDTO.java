package com.pfc.thindesk.dto;

public record IndicadoresDTO(
        long total,
        long abertos,
        long emAndamento,
        long concluidos,
        long cancelados,
        long criticosPendentes,
        double taxaResolucao,
        double tempoMedioResolucaoHoras) {
}