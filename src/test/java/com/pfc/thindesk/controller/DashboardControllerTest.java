package com.pfc.thindesk.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pfc.thindesk.dto.IndicadoresDTO;
import com.pfc.thindesk.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@WithMockUser(username = "admin")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void deveRetornarIndicadores() throws Exception {
        when(dashboardService.indicadores(any())).thenReturn(new IndicadoresDTO(10, 2, 3, 4, 1, 1, 40.0, 12.5));

        mockMvc.perform(get("/api/dashboard/indicadores").param("dataInicio", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.taxaResolucao").value(40.0));
    }

    @Test
    void deveRetornar400ParaDataInvalida() throws Exception {
        mockMvc.perform(get("/api/dashboard/indicadores").param("dataInicio", "14/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deveRetornar400QuandoDataInicioMaiorQueDataFim() throws Exception {
        mockMvc.perform(get("/api/dashboard/por-status")
                        .param("dataInicio", "2026-12-01")
                        .param("dataFim", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A dataInicio não pode ser posterior à dataFim"));
    }

    @Test
    void deveRetornar400ParaStatusInvalido() throws Exception {
        mockMvc.perform(get("/api/dashboard/indicadores").param("status", "PERDIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400ParaAgrupamentoInvalido() throws Exception {
        mockMvc.perform(get("/api/dashboard/por-periodo").param("agrupamento", "ANO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400ParaLimiteForaDoIntervalo() throws Exception {
        mockMvc.perform(get("/api/dashboard/tipos-mais-atendidos").param("limite", "500"))
                .andExpect(status().isBadRequest());
    }
}