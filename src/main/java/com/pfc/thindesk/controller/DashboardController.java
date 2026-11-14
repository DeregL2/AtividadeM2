package com.pfc.thindesk.controller;

import com.pfc.thindesk.dto.Agrupamento;
import com.pfc.thindesk.dto.ContagemDTO;
import com.pfc.thindesk.dto.FiltroDashboard;
import com.pfc.thindesk.dto.IndicadoresDTO;
import com.pfc.thindesk.dto.OpcoesFiltroDTO;
import com.pfc.thindesk.dto.PeriodoDTO;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.DashboardService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/indicadores")
    public IndicadoresDTO indicadores(FiltroDashboard filtro) {
        return dashboardService.indicadores(filtro.normalizado());
    }

    @GetMapping("/por-periodo")
    public List<PeriodoDTO> porPeriodo(FiltroDashboard filtro,
            @RequestParam(defaultValue = "MES") String agrupamento) {
        return dashboardService.porPeriodo(filtro.normalizado(), Agrupamento.de(agrupamento));
    }

    @GetMapping("/por-status")
    public List<ContagemDTO> porStatus(FiltroDashboard filtro) {
        return dashboardService.porStatus(filtro.normalizado());
    }

    @GetMapping("/tipos-mais-atendidos")
    public List<ContagemDTO> tiposMaisAtendidos(FiltroDashboard filtro,
            @RequestParam(defaultValue = "5") int limite,
            @RequestParam(defaultValue = "false") boolean apenasConcluidos) {
        return dashboardService.tiposMaisAtendidos(filtro.normalizado(), validarLimite(limite), apenasConcluidos);
    }

    @GetMapping("/por-tecnico")
    public List<ContagemDTO> porTecnico(FiltroDashboard filtro,
            @RequestParam(defaultValue = "5") int limite) {
        return dashboardService.porTecnico(filtro.normalizado(), validarLimite(limite));
    }

    @GetMapping("/chamados")
    public List<Chamado> chamados(FiltroDashboard filtro,
            @RequestParam(defaultValue = "20") int limite) {
        return dashboardService.listar(filtro.normalizado(), validarLimite(limite));
    }

    @GetMapping("/opcoes-filtro")
    public OpcoesFiltroDTO opcoesFiltro() {
        return dashboardService.opcoesFiltro();
    }

    private int validarLimite(int limite) {
        if (limite < 1 || limite > 100) {
            throw new IllegalArgumentException("O parâmetro limite deve estar entre 1 e 100");
        }
        return limite;
    }
}