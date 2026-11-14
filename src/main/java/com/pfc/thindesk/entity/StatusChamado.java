package com.pfc.thindesk.entity;

import com.pfc.thindesk.util.Textos;
import java.util.Arrays;

public enum StatusChamado {

    ABERTO("Aberto"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusChamado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isFinalizado() {
        return this == CONCLUIDO || this == CANCELADO;
    }

    public static StatusChamado de(String valor) {
        if (Textos.vazio(valor)) {
            throw new IllegalArgumentException("Status não informado");
        }
        String chave = Textos.chave(valor);
        return Arrays.stream(values())
                .filter(status -> status.name().equals(chave))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Status inválido: " + valor + ". Valores aceitos: " + Arrays.toString(values())));
    }

    public static StatusChamado deOuPadrao(String valor, StatusChamado padrao) {
        try {
            return de(valor);
        } catch (IllegalArgumentException e) {
            return padrao;
        }
    }
}