package com.pfc.thindesk.config;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.Prioridade;
import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import com.pfc.thindesk.service.ChamadoService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DadosIniciais implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private static final String[] TIPOS = {"Hardware", "Software", "Rede", "Acesso e Senhas", "Impressora", "E-mail", "Manutenção"};
    private static final String[] DESCRICOES = {
        "Computador não liga", "Erro ao abrir o sistema", "Sem acesso à internet", "Reset de senha do usuário",
        "Impressora não imprime", "Caixa de e-mail cheia", "Troca de periférico"};
    private static final String[] TECNICOS = {"João", "Maria", "Carlos", "Ana", "Pedro"};
    private static final String[] USUARIOS = {"Alberto", "Beatriz", "Camila", "Diego", "Eduarda", "Fábio", "Gabriela"};

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private ChamadoService chamadoService;

    @Value("${thindesk.dados-exemplo.habilitado:true}")
    private boolean habilitado;

    @Value("${thindesk.dados-exemplo.quantidade:150}")
    private int quantidade;

    @Override
    public void run(String... args) {
        int tratados = chamadoService.tratarBaseExistente();
        log.info("Tratamento de dados concluído: {} chamados verificados", tratados);
        if (!habilitado || chamadoRepository.count() >= 20) {
            return;
        }
        chamadoRepository.saveAll(gerarChamados());
        log.info("{} chamados de exemplo criados para o dashboard", quantidade);
    }

    private List<Chamado> gerarChamados() {
        Random random = new Random(2026);
        LocalDateTime agora = LocalDateTime.now();
        List<Chamado> chamados = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            int indiceTipo = sortearTipo(random);
            LocalDateTime abertura = agora.minusDays(random.nextInt(180)).minusHours(random.nextInt(24));
            StatusChamado status = sortearStatus(random, abertura.isBefore(agora.minusDays(30)));

            Chamado chamado = new Chamado();
            chamado.setDescricao(DESCRICOES[indiceTipo]);
            chamado.setTipo(TIPOS[indiceTipo]);
            chamado.setTecnico(TECNICOS[random.nextInt(TECNICOS.length)]);
            chamado.setUsuario(USUARIOS[random.nextInt(USUARIOS.length)]);
            chamado.setPrioridade(Prioridade.values()[random.nextInt(10) < 7 ? random.nextInt(2) : 2 + random.nextInt(2)].name());
            chamado.setStatus(status.name());
            chamado.setDataAbertura(abertura);
            if (status.isFinalizado()) {
                LocalDateTime fechamento = abertura.plusHours(1 + random.nextInt(72));
                chamado.setDataFechamento(fechamento.isAfter(agora) ? agora : fechamento);
            }
            chamados.add(chamado);
        }
        return chamados;
    }

    private int sortearTipo(Random random) {
        int sorteio = random.nextInt(100);
        if (sorteio < 25) {
            return 1;
        }
        if (sorteio < 45) {
            return 0;
        }
        if (sorteio < 60) {
            return 3;
        }
        if (sorteio < 72) {
            return 2;
        }
        return 4 + random.nextInt(3);
    }

    private StatusChamado sortearStatus(Random random, boolean antigo) {
        int sorteio = random.nextInt(100);
        if (antigo) {
            if (sorteio < 80) {
                return StatusChamado.CONCLUIDO;
            }
            return sorteio < 90 ? StatusChamado.CANCELADO : StatusChamado.EM_ANDAMENTO;
        }
        if (sorteio < 40) {
            return StatusChamado.CONCLUIDO;
        }
        if (sorteio < 70) {
            return StatusChamado.EM_ANDAMENTO;
        }
        return sorteio < 95 ? StatusChamado.ABERTO : StatusChamado.CANCELADO;
    }
}