package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.ChamadoService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    @Autowired
    private ChamadoService chamadoService;

    @PostMapping
    public ResponseEntity<Chamado> criarChamado(@RequestBody Chamado chamado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chamadoService.criarChamado(chamado));
    }

    @GetMapping
    public List<Chamado> listarChamados() {
        return chamadoService.listarChamados();
    }

    @GetMapping("/{id}")
    public Chamado buscarChamado(@PathVariable String id) {
        return chamadoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Chamado atualizarChamado(@PathVariable String id, @RequestBody Chamado chamadoAtualizado) {
        return chamadoService.atualizarChamado(id, chamadoAtualizado);
    }

    @PatchMapping("/{id}/status")
    public Chamado atualizarStatus(@PathVariable String id, @RequestBody Map<String, String> corpo) {
        return chamadoService.atualizarStatus(id, corpo.get("status"));
    }

    @PostMapping("/tratar-dados")
    public Map<String, Integer> tratarDados() {
        return Map.of("registrosTratados", chamadoService.tratarBaseExistente());
    }
}