const express = require("express");
const cors = require("cors");
require("dotenv").config();

require("./config/db");

const app = express();
const usuarioRoutes = require("./routes/usuarioRoutes");

app.use(cors());
app.use(express.json());

app.use("/usuarios", usuarioRoutes);

app.get("/", (req, res) => {
  res.send("Backend Parada Certa funcionando 🚗");
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Servidor rodando na porta ${PORT}`);
});