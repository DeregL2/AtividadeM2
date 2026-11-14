package com.pfc.thindesk.dto;

import java.util.List;

public record OpcoesFiltroDTO(
        List<String> status,
        List<String> prioridades,
        List<String> tipos,
        List<String> tecnicos) {
}