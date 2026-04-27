const express = require("express");
const router = express.Router();
const { sql } = require("../config/db");


// LISTAR

router.get("/", async (req, res) => {
  try {
    const result = await sql.query("SELECT * FROM Estacionamento");
    res.json(result.recordset);
  } catch (err) {
    console.error("Erro ao listar estacionamentos:", err);
    res.status(500).send(err.message);
  }
});


//  CRIAR (POST) COM REGRAS

router.post("/", async (req, res) => {
  const {
    nome,
    qtdVagasTotais,
    precoHora,
    latitude,
    longitude,
    endereco,
    horarioAbertura,
    horarioFechamento,
    ativo
  } = req.body;

  //  REGRA 1: campos obrigatórios
  if (!nome || !qtdVagasTotais || !precoHora || !latitude || !longitude || !endereco) {
    return res.status(400).send("Dados obrigatórios faltando");
  }

  //  REGRA 2: vagas válidas
  if (qtdVagasTotais <= 0) {
    return res.status(400).send("Quantidade de vagas inválida");
  }

  //  REGRA 3: preço válido
  if (precoHora <= 0) {
    return res.status(400).send("Preço deve ser maior que zero");
  }

  //  REGRA 4: limite de vagas
  if (qtdVagasTotais > 500) {
    return res.status(400).send("Estacionamento não pode ter mais que 500 vagas");
  }

  //  REGRA 5: horário válido
  if (horarioAbertura >= horarioFechamento) {
    return res.status(400).send("Horário de funcionamento inválido");
  }

  try {
    await sql.query(`
      INSERT INTO Estacionamento 
      (nome, qtdVagasTotais, qtdVagasDisponiveis, precoHora, latitude, longitude, endereco, horarioAbertura, horarioFechamento, ativo)
      VALUES 
      ('${nome}', ${qtdVagasTotais}, ${qtdVagasTotais}, ${precoHora}, ${latitude}, ${longitude}, '${endereco}', '${horarioAbertura}', '${horarioFechamento}', ${ativo ?? 1})
    `);

    res.status(201).send("Estacionamento criado com sucesso");
  } catch (err) {
    console.error("Erro ao criar estacionamento:", err);
    res.status(500).send(err.message);
  }
});


//  EDITAR 

router.put("/:id", async (req, res) => {
  const { id } = req.params;

  const {
    nome,
    qtdVagasTotais,
    precoHora,
    latitude,
    longitude,
    endereco,
    horarioAbertura,
    horarioFechamento,
    ativo
  } = req.body;

  // regras
  if (!nome || qtdVagasTotais <= 0 || precoHora <= 0) {
    return res.status(400).send("Dados inválidos");
  }

  if (horarioAbertura >= horarioFechamento) {
    return res.status(400).send("Horário inválido");
  }

  try {
    await sql.query(`
      UPDATE Estacionamento
      SET 
        nome = '${nome}',
        qtdVagasTotais = ${qtdVagasTotais},
        precoHora = ${precoHora},
        latitude = ${latitude},
        longitude = ${longitude},
        endereco = '${endereco}',
        horarioAbertura = '${horarioAbertura}',
        horarioFechamento = '${horarioFechamento}',
        ativo = ${ativo ?? 1}
      WHERE id = ${id}
    `);

    res.send("Estacionamento atualizado com sucesso");
  } catch (err) {
    console.error("Erro ao atualizar:", err);
    res.status(500).send(err.message);
  }
});


// DELETAR 

router.delete("/:id", async (req, res) => {
  const { id } = req.params;

  try {
    await sql.query(`
      DELETE FROM Estacionamento WHERE id = ${id}
    `);

    res.send("Estacionamento deletado com sucesso");
  } catch (err) {
    console.error("Erro ao deletar:", err);
    res.status(500).send(err.message);
  }
});

module.exports = router;