const sql = require("mssql");

const config = {
  user: "AdmParadaCerta",
  password: "sa",
  server: "localhost",
  port: 1433,
  database: "ParadaCerta",
  options: {
    encrypt: false,
    trustServerCertificate: true
  }
};

async function connect() {
  try {
    await sql.connect(config);
    console.log("Conectado ao SQL Server");
  } catch (err) {
    console.error("Erro ao conectar:", err);
    throw err;
  }
}

module.exports = { sql, connect };