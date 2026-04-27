const express = require("express");
const cors = require("cors");
require("dotenv").config();

const { connect } = require("./config/db");
const estacionamentoRoutes = require("./routes/estacionamentoRoutes");

const app = express();

app.use(cors());
app.use(express.json());

app.use("/estacionamentos", estacionamentoRoutes);

app.get("/", (req, res) => {
  res.send("Backend Parada Certa funcionando 🚗");
});

const PORT = process.env.PORT || 3000;

connect().then(() => {
  app.listen(PORT, () => {
    console.log(`Servidor rodando na porta ${PORT}`);
  });
});