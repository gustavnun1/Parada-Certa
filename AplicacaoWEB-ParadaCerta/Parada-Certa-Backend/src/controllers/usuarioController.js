const db = require("../config/db");

exports.cadastrar = (req, res) => {
  const { nome, email, senha } = req.body;

  const sql = "INSERT INTO usuarios (nome, email, senha) VALUES (?, ?, ?)";

  db.query(sql, [nome, email, senha], (err, result) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ erro: "Erro ao cadastrar usuário" });
    }

    res.status(201).json({
      mensagem: "Usuário cadastrado com sucesso"
    });
  });
};

exports.login = (req, res) => {
  const { email, senha } = req.body;

  const sql = "SELECT * FROM usuarios WHERE email = ? AND senha = ?";

  db.query(sql, [email, senha], (err, result) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ erro: "Erro no login" });
    }

    if (result.length === 0) {
      return res.status(401).json({ erro: "Email ou senha inválidos" });
    }

    res.json({
      mensagem: "Login realizado com sucesso",
      usuario: result[0]
    });
  });
};

exports.listar = (req, res) => {
  const sql = "SELECT * FROM usuarios";

  db.query(sql, (err, result) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ erro: "Erro ao listar usuários" });
    }

    res.status(200).json(result);
  });
};

exports.atualizar = (req, res) => {
  const { id } = req.params;
  const { nome, email, senha } = req.body;

  let sql = "";
  let valores = [];

  if (senha && senha.trim() !== "") {
    sql = "UPDATE usuarios SET nome = ?, email = ?, senha = ? WHERE id = ?";
    valores = [nome, email, senha, id];
  } else {
    sql = "UPDATE usuarios SET nome = ?, email = ? WHERE id = ?";
    valores = [nome, email, id];
  }

  db.query(sql, valores, (err, result) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ erro: "Erro ao atualizar usuário" });
    }

    if (result.affectedRows === 0) {
      return res.status(404).json({ erro: "Usuário não encontrado" });
    }

    res.status(200).json({
      mensagem: "Usuário atualizado com sucesso"
    });
  });
};

exports.deletar = (req, res) => {
  const { id } = req.params;

  const sql = "DELETE FROM usuarios WHERE id = ?";

  db.query(sql, [id], (err, result) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ erro: "Erro ao deletar usuário" });
    }

    if (result.affectedRows === 0) {
      return res.status(404).json({ erro: "Usuário não encontrado" });
    }

    res.status(200).json({
      mensagem: "Usuário deletado com sucesso"
    });
  });
};