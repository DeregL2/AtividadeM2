package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.Prioridade;
import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.exception.ChamadoNaoEncontradoException;
import com.pfc.thindesk.repository.ChamadoRepository;
import com.pfc.thindesk.util.Textos;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChamadoService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    public Chamado criarChamado(Chamado chamado) {
        validar(chamado);
        tratar(chamado);
        if (chamado.getDataAbertura() == null) {
            chamado.setDataAbertura(LocalDateTime.now());
        }
        ajustarDataFechamento(chamado);
        return chamadoRepository.save(chamado);
    }

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll();
    }

    public Chamado buscarPorId(String id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new ChamadoNaoEncontradoException(id));
    }

    public Chamado atualizarChamado(String id, Chamado chamadoAtualizado) {
        Chamado chamado = buscarPorId(id);
        validar(chamadoAtualizado);
        chamado.setDescricao(chamadoAtualizado.getDescricao());
        chamado.setStatus(chamadoAtualizado.getStatus());
        chamado.setTipo(chamadoAtualizado.getTipo());
        chamado.setTecnico(chamadoAtualizado.getTecnico());
        chamado.setUsuario(chamadoAtualizado.getUsuario());
        if (chamadoAtualizado.getPrioridade() != null) {
            chamado.setPrioridade(chamadoAtualizado.getPrioridade());
        }
        tratar(chamado);
        ajustarDataFechamento(chamado);
        return chamadoRepository.save(chamado);
    }

    public Chamado atualizarStatus(String id, String status) {
        Chamado chamado = buscarPorId(id);
        chamado.setStatus(StatusChamado.de(status).name());
        ajustarDataFechamento(chamado);
        return chamadoRepository.save(chamado);
    }

    public int tratarBaseExistente() {
        List<Chamado> chamados = chamadoRepository.findAll();
        for (Chamado chamado : chamados) {
            tratar(chamado);
            if (chamado.getDataAbertura() == null && chamado.getId() != null && ObjectId.isValid(chamado.getId())) {
                chamado.setDataAbertura(new ObjectId(chamado.getId()).getDate()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
            ajustarDataFechamento(chamado);
        }
        chamadoRepository.saveAll(chamados);
        return chamados.size();
    }

    private void validar(Chamado chamado) {
        if (chamado == null || Textos.vazio(chamado.getDescricao())) {
            throw new IllegalArgumentException("A descrição do chamado é obrigatória");
        }
        if (!Textos.vazio(chamado.getStatus())) {
            StatusChamado.de(chamado.getStatus());
        }
        if (!Textos.vazio(chamado.getPrioridade())) {
            Prioridade.de(chamado.getPrioridade());
        }
    }

    private void tratar(Chamado chamado) {
        chamado.setDescricao(Textos.limparOuPadrao(chamado.getDescricao(), "Sem descrição"));
        chamado.setStatus(StatusChamado.deOuPadrao(chamado.getStatus(), StatusChamado.ABERTO).name());
        chamado.setPrioridade(Prioridade.deOuPadrao(chamado.getPrioridade(), Prioridade.MEDIA).name());
        chamado.setTipo(Textos.limparOuPadrao(chamado.getTipo(), "Não informado"));
        chamado.setTecnico(Textos.limparOuPadrao(chamado.getTecnico(), "Não atribuído"));
        chamado.setUsuario(Textos.limparOuPadrao(chamado.getUsuario(), "Não informado"));
    }

    private void ajustarDataFechamento(Chamado chamado) {
        boolean finalizado = StatusChamado.de(chamado.getStatus()).isFinalizado();
        if (finalizado && chamado.getDataFechamento() == null) {
            chamado.setDataFechamento(LocalDateTime.now());
        }
        if (!finalizado) {
            chamado.setDataFechamento(null);
        }
    }
}