package com.pfc.thindesk.entity;

import com.pfc.thindesk.util.Textos;
import java.util.Arrays;

public enum Prioridade {

    BAIXA,
    MEDIA,
    ALTA,
    CRITICA;

    public static Prioridade de(String valor) {
        if (Textos.vazio(valor)) {
            throw new IllegalArgumentException("Prioridade não informada");
        }
        String chave = Textos.chave(valor);
        return Arrays.stream(values())
                .filter(prioridade -> prioridade.name().equals(chave))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prioridade inválida: " + valor + ". Valores aceitos: " + Arrays.toString(values())));
    }

    public static Prioridade deOuPadrao(String valor, Prioridade padrao) {
        try {
            return de(valor);
        } catch (IllegalArgumentException e) {
            return padrao;
        }
    }
}