package com.pfc.thindesk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.exception.ChamadoNaoEncontradoException;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @InjectMocks
    private ChamadoService chamadoService;

    @Test
    void deveNormalizarDadosAoCriarChamado() {
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        Chamado chamado = new Chamado();
        chamado.setDescricao("  Impressora   travando ");
        chamado.setStatus("Concluído");

        Chamado salvo = chamadoService.criarChamado(chamado);

        assertEquals("Impressora travando", salvo.getDescricao());
        assertEquals("CONCLUIDO", salvo.getStatus());
        assertEquals("MEDIA", salvo.getPrioridade());
        assertEquals("Não atribuído", salvo.getTecnico());
        assertNotNull(salvo.getDataAbertura());
        assertNotNull(salvo.getDataFechamento());
    }

    @Test
    void deveManterDataFechamentoVaziaParaChamadoAberto() {
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        Chamado chamado = new Chamado();
        chamado.setDescricao("Sem internet");
        chamado.setStatus("em andamento");

        Chamado salvo = chamadoService.criarChamado(chamado);

        assertEquals("EM_ANDAMENTO", salvo.getStatus());
        assertNull(salvo.getDataFechamento());
    }

    @Test
    void deveRecusarChamadoSemDescricao() {
        Chamado chamado = new Chamado();

        assertThrows(IllegalArgumentException.class, () -> chamadoService.criarChamado(chamado));
    }

    @Test
    void deveRecusarStatusInvalido() {
        Chamado chamado = new Chamado();
        chamado.setDescricao("Teste");
        chamado.setStatus("PERDIDO");

        assertThrows(IllegalArgumentException.class, () -> chamadoService.criarChamado(chamado));
    }

    @Test
    void deveLancarErroQuandoChamadoNaoExiste() {
        when(chamadoRepository.findById("999")).thenReturn(Optional.empty());

        assertThrows(ChamadoNaoEncontradoException.class, () -> chamadoService.buscarPorId("999"));
    }
}