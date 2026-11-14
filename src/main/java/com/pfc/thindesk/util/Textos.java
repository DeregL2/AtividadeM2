package com.pfc.thindesk.util;

import java.text.Normalizer;

public final class Textos {

    private Textos() {
    }

    public static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    public static String chave(String valor) {
        String semAcento = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toUpperCase().replaceAll("[\\s-]+", "_");
    }

    public static String limparOuPadrao(String valor, String padrao) {
        if (vazio(valor)) {
            return padrao;
        }
        return valor.trim().replaceAll("\\s+", " ");
    }
}