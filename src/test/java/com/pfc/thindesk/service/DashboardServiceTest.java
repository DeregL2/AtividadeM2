package com.pfc.thindesk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pfc.thindesk.dto.Agrupamento;
import com.pfc.thindesk.dto.ContagemDTO;
import com.pfc.thindesk.dto.IndicadoresDTO;
import com.pfc.thindesk.dto.PeriodoDTO;
import com.pfc.thindesk.entity.Chamado;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private final DashboardService service = new DashboardService();

    private Chamado chamado(String status, String tipo, String prioridade, LocalDateTime abertura, LocalDateTime fechamento) {
        Chamado chamado = new Chamado();
        chamado.setDescricao("Teste");
        chamado.setStatus(status);
        chamado.setTipo(tipo);
        chamado.setTecnico("João");
        chamado.setPrioridade(prioridade);
        chamado.setDataAbertura(abertura);
        chamado.setDataFechamento(fechamento);
        return chamado;
    }

    private List<Chamado> base() {
        LocalDateTime jan = LocalDateTime.of(2026, 1, 10, 8, 0);
        LocalDateTime fev = LocalDateTime.of(2026, 2, 5, 9, 0);
        return List.of(
                chamado("CONCLUIDO", "Hardware", "ALTA", jan, jan.plusHours(4)),
                chamado("CONCLUIDO", "Software", "MEDIA", jan, jan.plusHours(8)),
                chamado("ABERTO", "Software", "CRITICA", fev, null),
                chamado("EM_ANDAMENTO", "Software", "BAIXA", fev, null),
                chamado("CANCELADO", "Rede", "CRITICA", fev, fev.plusHours(1)));
    }

    @Test
    void deveCalcularIndicadoresGerais() {
        IndicadoresDTO indicadores = service.calcularIndicadores(base());

        assertEquals(5, indicadores.total());
        assertEquals(1, indicadores.abertos());
        assertEquals(1, indicadores.emAndamento());
        assertEquals(2, indicadores.concluidos());
        assertEquals(1, indicadores.cancelados());
        assertEquals(1, indicadores.criticosPendentes());
        assertEquals(40.0, indicadores.taxaResolucao());
        assertEquals(6.0, indicadores.tempoMedioResolucaoHoras());
    }

    @Test
    void deveRetornarIndicadoresZeradosSemChamados() {
        IndicadoresDTO indicadores = service.calcularIndicadores(List.of());

        assertEquals(0, indicadores.total());
        assertEquals(0.0, indicadores.taxaResolucao());
        assertEquals(0.0, indicadores.tempoMedioResolucaoHoras());
    }

    @Test
    void deveAgruparChamadosPorMes() {
        List<PeriodoDTO> periodos = service.agruparPorPeriodo(base(), Agrupamento.MES);

        assertEquals(2, periodos.size());
        assertEquals(new PeriodoDTO("2026-01", 2, 2), periodos.get(0));
        assertEquals(new PeriodoDTO("2026-02", 3, 0), periodos.get(1));
    }

    @Test
    void deveContarTodosOsStatus() {
        List<ContagemDTO> status = service.contarPorStatus(base());

        assertEquals(4, status.size());
        assertEquals(new ContagemDTO("CONCLUIDO", 2, 40.0), status.get(2));
    }

    @Test
    void deveMontarRankingDeTiposDoMaiorParaOMenor() {
        List<ContagemDTO> ranking = service.ranking(base(), Chamado::getTipo, 2);

        assertEquals(2, ranking.size());
        assertEquals(new ContagemDTO("Software", 3, 60.0), ranking.get(0));
        assertEquals("Hardware", ranking.get(1).nome());
    }
}