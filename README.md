# Parada Certa

Bem-vindo ao Parada Certa. Este repositorio guarda o sistema completo do projeto: backend, painel web, API auxiliar de QR Code, aplicativo Android, scripts de banco e documentacao do TCC.

A ideia deste README e ser o seu primeiro ponto de apoio. Se voce acabou de clonar o projeto, comece por aqui com calma: configure o ambiente uma vez, rode os servicos na ordem certa e depois o desenvolvimento fica bem mais tranquilo.

## O Que Existe Aqui

```text
ProjetoParadaCerta/
|-- Documentacao Projeto/              Documentos, entregas e materiais de apoio
|-- Apresentacao Projeto/              Arquivos de apresentacao
`-- SistemaParadaCerta/
    |-- APIs-ParadaCerta/              API principal Spring Boot usada pelo app e painel
    |-- AplicacaoMobile-ParadaCerta/   App Android em Kotlin/Jetpack Compose
    |-- AplicacaoQRCode-ParadaCerta/   API auxiliar para operacao/QR Code
    |-- AplicacoesWEB/                 Painel web administrativo e copia da API QR usada no web
    |-- SQL - Parada Certa/            Scripts do banco SQL Server
    |-- .env.example                   Modelo das variaveis de ambiente
    `-- .env                           Configuracao local, nao deve ser versionada
```

Observacao importante: existem algumas pastas historicas/copiadas no projeto. Para desenvolvimento atual, use principalmente o conteudo dentro de `SistemaParadaCerta`.

## Pre-Requisitos

Antes de rodar tudo, instale:

- Java JDK 17
- Maven
- Node.js 18 ou superior
- Android Studio, para o app mobile
- SQL Server local ou acessivel pela rede
- Git

Opcional, mas recomendado:

- VS Code para o painel web
- IntelliJ IDEA para as APIs Spring Boot
- SQL Server Management Studio ou Azure Data Studio para executar os scripts SQL

## Primeiro Passo: Configurar o Ambiente

Entre na pasta principal do sistema:

```powershell
cd C:\Users\gusta\OneDrive\Documentos\Projetos\ProjetoParadaCerta\SistemaParadaCerta
```

Copie o arquivo de exemplo:

```powershell
Copy-Item .env.example .env
```

Depois abra o `.env` e preencha pelo menos:

```properties
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=ParadaCerta;encrypt=false;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=seu_usuario
SPRING_DATASOURCE_PASSWORD=sua_senha

PORT=8080
QR_API_PORT=8081

ANDROID_API_BASE_URL=http://10.0.2.2:8080/
GOOGLE_MAPS_API_KEY=sua_chave_google_maps
```

O Android usa `10.0.2.2` quando roda no emulador, porque esse endereco aponta para o `localhost` da sua maquina. Em celular fisico, troque pelo IP do computador na rede, por exemplo `http://192.168.0.10:8080/`.

## Banco De Dados

Crie o banco `ParadaCerta` no SQL Server e execute o script principal:

```text
SistemaParadaCerta/SQL - Parada Certa/CREATE TABLES - ParadaCerta.sql
```

Se estiver trabalhando com os recursos mais recentes de endereco/exclusao de conta da API de QR, execute tambem:

```text
SistemaParadaCerta/AplicacoesWEB/AplicacaoQRCode-ParadaCerta/paradacerta-generateqrcode-api/src/main/resources/db/06-ALTER-Estacionamento-Enderecos-ExclusaoConta.sql
```

Se algo falhar no backend logo ao iniciar, quase sempre e credencial do `.env`, SQL Server parado ou banco sem as tabelas esperadas.

## Rodando A API Principal

Esta e a API usada pelo app Android e por boa parte do painel administrativo.

```powershell
cd SistemaParadaCerta\APIs-ParadaCerta\paradacerta-api
mvn spring-boot:run
```

Por padrao ela sobe em:

```text
http://localhost:8080
```

Teste rapido:

```text
http://localhost:8080/api/health
```

## Rodando A API De QR Code

Esta API cuida de fluxos administrativos/operacionais relacionados a estacionamento, operador e QR Code.

```powershell
cd SistemaParadaCerta\AplicacoesWEB\AplicacaoQRCode-ParadaCerta\paradacerta-generateqrcode-api
mvn spring-boot:run
```

Por padrao ela sobe em:

```text
http://localhost:8081
```

Use uma porta diferente da API principal. O `.env.example` ja deixa `PORT=8080` e `QR_API_PORT=8081` para evitar conflito.

## Rodando O Painel Web

O painel web fica em:

```text
SistemaParadaCerta/AplicacoesWEB/AplicacaoWEB-ParadaCerta/Parada-Certa-Front
```

Ele e um frontend estatico. A forma mais simples e abrir com uma extensao como Live Server no VS Code, preferencialmente em:

```text
http://localhost:5500
```

Comece por:

```text
index.html
```

ou pelo fluxo de login:

```text
escolha-login.html
login-admin.html
```

O arquivo `pc-api.js` aponta para `http://localhost:8080`, entao mantenha a API principal rodando antes de testar telas que buscam dados reais.

## Rodando O App Android

Abra no Android Studio a pasta:

```text
SistemaParadaCerta/AplicacaoMobile-ParadaCerta/ParadaCerta
```

Espere o Gradle sincronizar, confira se o `.env` tem `ANDROID_API_BASE_URL` e `GOOGLE_MAPS_API_KEY`, e rode o app pelo Android Studio.

Para emulador Android:

```properties
ANDROID_API_BASE_URL=http://10.0.2.2:8080/
```

Para aparelho fisico:

```properties
ANDROID_API_BASE_URL=http://IP_DA_SUA_MAQUINA:8080/
```

## Rodando Os Testes

Backend principal:

```powershell
cd SistemaParadaCerta\APIs-ParadaCerta\paradacerta-api
mvn test
```

API de QR:

```powershell
cd SistemaParadaCerta\AplicacoesWEB\AplicacaoQRCode-ParadaCerta\paradacerta-generateqrcode-api
mvn test
```

Painel web:

```powershell
cd SistemaParadaCerta\AplicacoesWEB\AplicacaoWEB-ParadaCerta\Parada-Certa-Front
npm test
```

Mobile:

```powershell
cd SistemaParadaCerta\AplicacaoMobile-ParadaCerta\ParadaCerta
.\gradlew.bat testDebugUnitTest
```

## Problemas Comuns

`Port 8080 already in use`  
Algum processo ja esta usando a porta. Pare o processo ou mude `PORT` no `.env`.

`Cannot resolve symbol springframework`  
O Maven ainda nao baixou as dependencias. Recarregue o `pom.xml` na IDE ou rode `mvn test`/`mvn spring-boot:run` pelo terminal.

`Erro ao conectar no SQL Server`  
Confira se o SQL Server esta ligado, se o banco `ParadaCerta` existe e se usuario/senha no `.env` estao corretos.

`App Android nao conecta na API`  
No emulador use `10.0.2.2`, nao `localhost`. Em aparelho fisico use o IP da maquina e confirme que celular e computador estao na mesma rede.

`Google Maps nao aparece`  
Verifique `GOOGLE_MAPS_API_KEY` no `.env` e as restricoes da chave no Google Cloud.

## Para Contribuir Sem Se Perder

Antes de mexer, rode os testes da parte que voce vai alterar. Depois de mexer, rode de novo. Parece simples, mas salva muito tempo.

Evite commitar arquivos locais como `.env`, `target/`, `build/`, `.idea/` e arquivos temporarios do Word. O projeto tem informacoes sensiveis em configuracoes locais, entao trate o `.env` como algo seu, nao do repositorio.

Quando abrir um problema, descreva:

- qual modulo voce estava rodando;
- qual comando usou;
- qual erro apareceu;
- se a API, banco e app estavam ligados ao mesmo tempo.

Isso ajuda a proxima pessoa a continuar exatamente de onde voce parou.

## Um Norte Rapido

Se voce so quer ver o sistema funcionando pela primeira vez, siga esta ordem:

1. Configure `.env`.
2. Crie o banco e rode os scripts SQL.
3. Suba a API principal na porta `8080`.
4. Suba a API de QR na porta `8081`, se for testar fluxo de operador/QR.
5. Abra o painel web pelo Live Server.
6. Abra o app Android no Android Studio.

Pronto. A partir dai, o projeto deixa de ser uma pasta enorme e vira um sistema com pecas conversando entre si.
