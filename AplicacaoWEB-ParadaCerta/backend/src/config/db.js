const mysql = require("mysql2");

const connection = mysql.createConnection({
  host: "127.0.0.1",
  user: "AdmParadaCerta",
  password: "",
  database: "ParadaCerta",
  port: 3306
});

connection.connect((err) => {
  if (err) {
    console.error("Erro ao conectar no MySQL:", err);
  } else {
    console.log("Conectado ao MySQL");
  }
});

module.exports = connection;