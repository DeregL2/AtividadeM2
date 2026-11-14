package com.pfc.thindesk.dto;

import com.pfc.thindesk.util.Textos;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.util.Arrays;

public enum Agrupamento {

    DIA,
    SEMANA,
    MES;

    public static Agrupamento de(String valor) {
        if (Textos.vazio(valor)) {
            return MES;
        }
        String chave = Textos.chave(valor);
        return Arrays.stream(values())
                .filter(agrupamento -> agrupamento.name().equals(chave))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agrupamento inválido: " + valor + ". Valores aceitos: " + Arrays.toString(values())));
    }

    public String chave(LocalDate data) {
        return switch (this) {
            case DIA -> data.toString();
            case SEMANA -> String.format("%d-S%02d",
                    data.get(IsoFields.WEEK_BASED_YEAR),
                    data.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MES -> YearMonth.from(data).toString();
        };
    }
}