# 🚀 GUIA RÁPIDO — Abrir o Projeto no IntelliJ IDEA

## Passo 1 — Extrair o ZIP

Extraia o arquivo `paradacerta-api.zip` em uma pasta de sua preferência, por exemplo:
```
C:\projetos\paradacerta-api
```

---

## Passo 2 — Abrir no IntelliJ IDEA

1. Abra o **IntelliJ IDEA**
2. Clique em **File → Open**
3. Navegue até a pasta `paradacerta-api` (a que contém o `pom.xml`)
4. Clique em **OK**

O IntelliJ vai:
- Reconhecer automaticamente que é um projeto Maven
- Baixar todas as dependências do Spring Boot
- Configurar o projeto

⏱️ Aguarde alguns minutos na primeira vez (download das libs).

---

## Passo 3 — Configurar o Banco de Dados

Abra o arquivo:
```
src/main/resources/application.properties
```

E edite estas linhas com suas credenciais:
```properties
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA
```

Se o SQL Server não estiver em `localhost`, ajuste também:
```properties
spring.datasource.url=jdbc:sqlserver://SEU_IP:1433;databaseName=ParadaCerta;...
```

---

## Passo 4 — Rodar a API

### Opção A — Via IntelliJ (mais fácil):
1. Abra o arquivo: `src/main/java/com/paradacerta/api/ParadaCertaApiApplication.java`
2. Clique com o botão direito no arquivo
3. Selecione **Run 'ParadaCertaApiApplication'**

### Opção B — Via terminal:
```bash
cd paradacerta-api
mvn spring-boot:run
```

---

## Passo 5 — Confirmar que está rodando

No console do IntelliJ, você deve ver:
```
Started ParadaCertaApiApplication in X.XXX seconds (JVM running for Y.YYY)
```

Abra o navegador e acesse:
```
http://localhost:8080/api/health
```

Deve retornar:
```json
{
  "sucesso": true,
  "mensagem": "API Parada Certa está no ar!"
}
```

✅ **Pronto!** A API está rodando e o Android pode se conectar a ela.

---

## 🔧 Problemas comuns

**"Cannot resolve symbol 'springframework'"**
→ O Maven não baixou as dependências. Clique com botão direito no `pom.xml` → **Maven → Reload Project**

**"Port 8080 already in use"**
→ Outra aplicação está usando a porta 8080. Mate o processo ou mude a porta em `application.properties`:
```properties
server.port=8081
```

**Erro ao conectar ao SQL Server**
→ Verifique se:
- O SQL Server está rodando
- As credenciais em `application.properties` estão corretas
- O firewall está permitindo conexões na porta 1433

---

## 📱 Próximo passo

Agora configure o Android para chamar esta API. Veja o arquivo `COMO_RODAR_API.md` (Parte 2).
