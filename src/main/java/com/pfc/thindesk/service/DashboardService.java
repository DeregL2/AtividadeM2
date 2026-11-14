package com.pfc.thindesk.service;

import com.pfc.thindesk.dto.Agrupamento;
import com.pfc.thindesk.dto.ContagemDTO;
import com.pfc.thindesk.dto.FiltroDashboard;
import com.pfc.thindesk.dto.IndicadoresDTO;
import com.pfc.thindesk.dto.OpcoesFiltroDTO;
import com.pfc.thindesk.dto.PeriodoDTO;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.Prioridade;
import com.pfc.thindesk.entity.StatusChamado;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Chamado> buscar(FiltroDashboard filtro) {
        Query query = new Query();
        if (filtro.dataInicio() != null || filtro.dataFim() != null) {
            Criteria criteria = Criteria.where("dataAbertura");
            if (filtro.dataInicio() != null) {
                criteria = criteria.gte(filtro.dataInicio().atStartOfDay());
            }
            if (filtro.dataFim() != null) {
                criteria = criteria.lt(filtro.dataFim().plusDays(1).atStartOfDay());
            }
            query.addCriteria(criteria);
        }
        if (filtro.status() != null) {
            query.addCriteria(Criteria.where("status").is(filtro.status()));
        }
        if (filtro.prioridade() != null) {
            query.addCriteria(Criteria.where("prioridade").is(filtro.prioridade()));
        }
        if (filtro.tipo() != null) {
            query.addCriteria(Criteria.where("tipo").regex("^" + Pattern.quote(filtro.tipo()) + "$", "i"));
        }
        if (filtro.tecnico() != null) {
            query.addCriteria(Criteria.where("tecnico").regex("^" + Pattern.quote(filtro.tecnico()) + "$", "i"));
        }
        query.with(Sort.by(Sort.Direction.DESC, "dataAbertura"));
        return mongoTemplate.find(query, Chamado.class);
    }

    public IndicadoresDTO indicadores(FiltroDashboard filtro) {
        return calcularIndicadores(buscar(filtro));
    }

    public List<PeriodoDTO> porPeriodo(FiltroDashboard filtro, Agrupamento agrupamento) {
        return agruparPorPeriodo(buscar(filtro), agrupamento);
    }

    public List<ContagemDTO> porStatus(FiltroDashboard filtro) {
        return contarPorStatus(buscar(filtro));
    }

    public List<ContagemDTO> tiposMaisAtendidos(FiltroDashboard filtro, int limite, boolean apenasConcluidos) {
        List<Chamado> chamados = buscar(filtro);
        if (apenasConcluidos) {
            chamados = chamados.stream()
                    .filter(chamado -> StatusChamado.CONCLUIDO.name().equals(chamado.getStatus()))
                    .toList();
        }
        return ranking(chamados, Chamado::getTipo, limite);
    }

    public List<ContagemDTO> porTecnico(FiltroDashboard filtro, int limite) {
        return ranking(buscar(filtro), Chamado::getTecnico, limite);
    }

    public List<Chamado> listar(FiltroDashboard filtro, int limite) {
        return buscar(filtro).stream().limit(limite).toList();
    }

    public OpcoesFiltroDTO opcoesFiltro() {
        List<String> tipos = mongoTemplate.findDistinct(new Query(), "tipo", Chamado.class, String.class)
                .stream().sorted().toList();
        List<String> tecnicos = mongoTemplate.findDistinct(new Query(), "tecnico", Chamado.class, String.class)
                .stream().sorted().toList();
        List<String> status = Arrays.stream(StatusChamado.values()).map(Enum::name).toList();
        List<String> prioridades = Arrays.stream(Prioridade.values()).map(Enum::name).toList();
        return new OpcoesFiltroDTO(status, prioridades, tipos, tecnicos);
    }

    public IndicadoresDTO calcularIndicadores(List<Chamado> chamados) {
        long total = chamados.size();
        long abertos = contar(chamados, StatusChamado.ABERTO);
        long emAndamento = contar(chamados, StatusChamado.EM_ANDAMENTO);
        long concluidos = contar(chamados, StatusChamado.CONCLUIDO);
        long cancelados = contar(chamados, StatusChamado.CANCELADO);
        long criticosPendentes = chamados.stream()
                .filter(chamado -> Prioridade.CRITICA.name().equals(chamado.getPrioridade()))
                .filter(chamado -> !StatusChamado.deOuPadrao(chamado.getStatus(), StatusChamado.ABERTO).isFinalizado())
                .count();
        double taxaResolucao = total == 0 ? 0 : arredondar(concluidos * 100.0 / total);
        double tempoMedioHoras = chamados.stream()
                .filter(chamado -> StatusChamado.CONCLUIDO.name().equals(chamado.getStatus()))
                .filter(chamado -> chamado.getDataAbertura() != null && chamado.getDataFechamento() != null)
                .mapToLong(chamado -> Duration.between(chamado.getDataAbertura(), chamado.getDataFechamento()).toMinutes())
                .average()
                .orElse(0) / 60.0;
        return new IndicadoresDTO(total, abertos, emAndamento, concluidos, cancelados, criticosPendentes,
                taxaResolucao, arredondar(tempoMedioHoras));
    }

    public List<PeriodoDTO> agruparPorPeriodo(List<Chamado> chamados, Agrupamento agrupamento) {
        Map<String, long[]> mapa = new TreeMap<>();
        for (Chamado chamado : chamados) {
            if (chamado.getDataAbertura() != null) {
                String chave = agrupamento.chave(chamado.getDataAbertura().toLocalDate());
                mapa.computeIfAbsent(chave, k -> new long[2])[0]++;
            }
            if (StatusChamado.CONCLUIDO.name().equals(chamado.getStatus()) && chamado.getDataFechamento() != null) {
                String chave = agrupamento.chave(chamado.getDataFechamento().toLocalDate());
                mapa.computeIfAbsent(chave, k -> new long[2])[1]++;
            }
        }
        return mapa.entrySet().stream()
                .map(item -> new PeriodoDTO(item.getKey(), item.getValue()[0], item.getValue()[1]))
                .toList();
    }

    public List<ContagemDTO> contarPorStatus(List<Chamado> chamados) {
        long total = chamados.size();
        return Arrays.stream(StatusChamado.values())
                .map(status -> {
                    long quantidade = contar(chamados, status);
                    return new ContagemDTO(status.name(), quantidade, percentual(quantidade, total));
                })
                .toList();
    }

    public List<ContagemDTO> ranking(List<Chamado> chamados, Function<Chamado, String> campo, int limite) {
        long total = chamados.size();
        Map<String, Long> contagem = chamados.stream()
                .collect(Collectors.groupingBy(
                        chamado -> campo.apply(chamado) == null ? "Não informado" : campo.apply(chamado),
                        Collectors.counting()));
        return contagem.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limite)
                .map(item -> new ContagemDTO(item.getKey(), item.getValue(), percentual(item.getValue(), total)))
                .toList();
    }

    private long contar(List<Chamado> chamados, StatusChamado status) {
        return chamados.stream().filter(chamado -> status.name().equals(chamado.getStatus())).count();
    }

    private double percentual(long parte, long total) {
        return total == 0 ? 0 : arredondar(parte * 100.0 / total);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}