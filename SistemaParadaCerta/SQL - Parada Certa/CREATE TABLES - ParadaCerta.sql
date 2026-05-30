 -- =============================================
  -- SCRIPT COMPLETO - BANCO PARADA CERTA
  -- Execução única, sem ALTER TABLE
  --
  -- Este script é a CONSOLIDAÇÃO MASTER. Quem clonar o repositório e
  -- executar SOMENTE este arquivo terá o schema final correto.
  -- =============================================

  USE [ParadaCerta]
  GO

  -- =============================================
  -- 1. REMOVER OBJETOS EXISTENTES
  -- =============================================

  IF OBJECT_ID('dbo.sp_RegistrarPagamento', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_RegistrarPagamento;
  IF OBJECT_ID('dbo.sp_RegistrarEntrada',  'P') IS NOT NULL DROP PROCEDURE dbo.sp_RegistrarEntrada;
  GO

  IF OBJECT_ID('TR_Sessao_AtualizaVagas',    'TR') IS NOT NULL DROP TRIGGER TR_Sessao_AtualizaVagas;
  IF OBJECT_ID('TR_Vaga_AtualizaDisponiveis','TR') IS NOT NULL DROP TRIGGER TR_Vaga_AtualizaDisponiveis;
  IF OBJECT_ID('TR_Avaliacao_AtualizaMedia', 'TR') IS NOT NULL DROP TRIGGER TR_Avaliacao_AtualizaMedia;
  GO

  -- Remover na ordem correta (filhos antes dos pais)
  IF OBJECT_ID('dbo.AssinaturaPlanoPagamento','U') IS NOT NULL DROP TABLE dbo.AssinaturaPlanoPagamento;
  IF OBJECT_ID('dbo.EstacionamentoFoto',      'U') IS NOT NULL DROP TABLE dbo.EstacionamentoFoto;
  IF OBJECT_ID('dbo.QrCodeEntrada',           'U') IS NOT NULL DROP TABLE dbo.QrCodeEntrada;
  IF OBJECT_ID('dbo.OperadorEstacionamento',  'U') IS NOT NULL DROP TABLE dbo.OperadorEstacionamento;
  IF OBJECT_ID('dbo.AdmEstacionamento',       'U') IS NOT NULL DROP TABLE dbo.AdmEstacionamento;
  IF OBJECT_ID('dbo.Avaliacao',               'U') IS NOT NULL DROP TABLE dbo.Avaliacao;
  IF OBJECT_ID('dbo.Vaga',                    'U') IS NOT NULL DROP TABLE dbo.Vaga;
  IF OBJECT_ID('dbo.FormaPagamento',          'U') IS NOT NULL DROP TABLE dbo.FormaPagamento;
  IF OBJECT_ID('dbo.SessaoEstacionamento',    'U') IS NOT NULL DROP TABLE dbo.SessaoEstacionamento;
  IF OBJECT_ID('dbo.VagasEstacionamento',     'U') IS NOT NULL DROP TABLE dbo.VagasEstacionamento;
  IF OBJECT_ID('dbo.Estacionamento',          'U') IS NOT NULL DROP TABLE dbo.Estacionamento;
  IF OBJECT_ID('dbo.Endereco',                'U') IS NOT NULL DROP TABLE dbo.Endereco;
  IF OBJECT_ID('dbo.Veiculo',                 'U') IS NOT NULL DROP TABLE dbo.Veiculo;
  IF OBJECT_ID('dbo.Cliente',                 'U') IS NOT NULL DROP TABLE dbo.Cliente;
  IF OBJECT_ID('dbo.Gerente',                 'U') IS NOT NULL DROP TABLE dbo.Gerente;
  GO

  -- =============================================
  -- 2. TABELAS PRINCIPAIS (MOTORISTA)
  -- =============================================

  CREATE TABLE [dbo].[Cliente] (
      [id]             [bigint]        IDENTITY(1,1) NOT NULL,
      [nome]           [nvarchar](200) NOT NULL,
      [cpf]            [varchar](11)   NOT NULL,
      [email]          [nvarchar](200) NOT NULL,
      [senha]          [nvarchar](255) NOT NULL,
      [dataNascimento] [date]          NOT NULL,
      [numeroCelular]  [varchar](15)   NULL,
      [placa]          [varchar](7)    NOT NULL,
      [veiculo]        [varchar](70)   NULL,

      CONSTRAINT PK_Cliente       PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT UQ_Cliente_CPF   UNIQUE ([cpf]),
      CONSTRAINT UQ_Cliente_Email UNIQUE ([email]),
      CONSTRAINT CK_Cliente_CPF   CHECK (LEN([cpf]) = 11),
      CONSTRAINT CK_Cliente_Email CHECK ([email] LIKE '%@%'),
      CONSTRAINT CK_Cliente_Placa CHECK (LEN([placa]) > 0)
  );
  GO

  CREATE TABLE [dbo].[Veiculo] (
      [nome]      [nvarchar](100) NOT NULL,
      [placa]     [varchar](7)    NOT NULL,
      [cor]       [nvarchar](50)  NOT NULL,
      [clienteId] [bigint]        NOT NULL,

      CONSTRAINT PK_Veiculo         PRIMARY KEY CLUSTERED ([placa] ASC),
      CONSTRAINT FK_Veiculo_Cliente FOREIGN KEY ([clienteId])
          REFERENCES [dbo].[Cliente] ([id]) ON DELETE CASCADE,
      CONSTRAINT CK_Veiculo_Placa   CHECK (LEN([placa]) > 0)
  );
  GO

  CREATE TABLE [dbo].[Endereco] (
      [id]          [int]           IDENTITY(1,1) NOT NULL,
      [cep]         [varchar](8)    NOT NULL,
      [logradouro]  [nvarchar](200) NOT NULL,
      [numero]      [nvarchar](10)  NOT NULL,
      [complemento] [nvarchar](100) NULL,
      [bairro]      [nvarchar](100) NOT NULL,
      [cidade]      [nvarchar](100) NOT NULL,
      [estado]      [varchar](2)    NOT NULL,
      [clienteId]   [bigint]        NOT NULL,

      CONSTRAINT PK_Endereco         PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT FK_Endereco_Cliente FOREIGN KEY ([clienteId])
          REFERENCES [dbo].[Cliente] ([id]) ON DELETE CASCADE,
      CONSTRAINT CK_Endereco_CEP    CHECK (LEN([cep]) = 8),
      CONSTRAINT CK_Endereco_Estado CHECK (LEN([estado]) = 2)
  );
  GO

  -- =============================================
  -- 3. TABELAS DE ESTACIONAMENTO
  -- =============================================

  -- Dados cadastrais e de localização do estacionamento.
  -- Contagem de vagas foi separada para VagasEstacionamento.
  --
  -- ENDEREÇO: a partir do ALTER 04, os campos estruturados (cep, logradouro,
  -- numero, complemento, bairro, cidade, uf) são a fonte de verdade.
  -- A coluna [endereco] (texto livre) é MANTIDA como espelho derivado para
  -- compatibilidade com consumidores existentes (app mobile, listagens) e
  -- é preenchida automaticamente pelo backend a partir das partes detalhadas.
  CREATE TABLE [dbo].[Estacionamento] (
      [id]                  [int]            IDENTITY(1,1) NOT NULL,
      [nome]                [nvarchar](100)  NOT NULL,
      [cnpj]                [nvarchar](14)   NOT NULL,
      [razaoSocial]         [nvarchar](200)  NOT NULL,
      [nomeFantasia]        [nvarchar](200)  NULL,
      [avaliacaoMedia]      [decimal](3,2)   NOT NULL DEFAULT 0.0,
      [latitude]            [decimal](10,8)  NOT NULL,
      [longitude]           [decimal](11,8)  NOT NULL,
      [endereco]            [nvarchar](300)  NOT NULL,    -- texto livre (espelho derivado)
      [precoHora]           [decimal](10,2)  NOT NULL,
      [horarioAbertura]     [time]           NULL,
      [horarioFechamento]   [time]           NULL,
      [fotoPrincipal]       [nvarchar](500)  NULL,
      [descricao]           [nvarchar](1000) NULL,
      [ativo]               [bit]            NOT NULL DEFAULT 1,
      [pixKey]              [nvarchar](200)  NULL,
      [permiteReserva]      [bit]            NOT NULL DEFAULT 0,

      -- Endereço estruturado (ALTER 04)
      [cep]                 [varchar](8)     NULL,
      [logradouro]          [nvarchar](200)  NULL,
      [numero]              [varchar](10)    NULL,
      [complemento]         [nvarchar](100)  NULL,
      [bairro]              [nvarchar](100)  NULL,
      [cidade]              [nvarchar](100)  NULL,
      [uf]                  [char](2)        NULL,

      -- Plano de assinatura (ALTER 07)
      [plano]               [varchar](10)    NULL,
      [planoInicio]         [datetime2]      NULL,
      [planoFim]            [datetime2]      NULL,
      [planoCobranca]       [varchar](10)    NULL,

      CONSTRAINT PK_Estacionamento              PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT UQ_Estacionamento_cnpj         UNIQUE ([cnpj]),
      CONSTRAINT CK_Estacionamento_Latitude     CHECK ([latitude]  >= -90  AND [latitude]  <= 90),
      CONSTRAINT CK_Estacionamento_Longitude    CHECK ([longitude] >= -180 AND [longitude] <= 180),
      CONSTRAINT CK_Estacionamento_Avaliacao    CHECK ([avaliacaoMedia] >= 0 AND [avaliacaoMedia] <= 5),
      CONSTRAINT CK_Estacionamento_PrecoHora    CHECK ([precoHora] >= 0),
      CONSTRAINT CK_Estacionamento_CNPJ         CHECK (LEN([cnpj]) = 14),
      CONSTRAINT CK_Estacionamento_Plano        CHECK ([plano] IS NULL OR [plano] IN ('BASIC','STANDARD','PREMIUM')),
      CONSTRAINT CK_Estacionamento_PlanoCobranca CHECK ([planoCobranca] IS NULL OR [planoCobranca] IN ('MENSAL','ANUAL','TRIAL'))
  );
  GO

  -- Separação de responsabilidade: contagem operacional de vagas.
  -- Atualizada automaticamente pelo trigger TR_Sessao_AtualizaVagas.
  CREATE TABLE [dbo].[VagasEstacionamento] (
      [id]                  [int] IDENTITY(1,1) NOT NULL,
      [estacionamentoId]    [int] NOT NULL,
      [qtdVagasTotais]      [int] NOT NULL,
      [qtdVagasDisponiveis] [int] NOT NULL DEFAULT 0,
      [qtdVagasReservaveis] [int] NOT NULL DEFAULT 0,
      [qtdVagasReservadas]  [int] NOT NULL DEFAULT 0,

      CONSTRAINT PK_VagasEstacionamento       PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT UQ_Vagas_Estacionamento      UNIQUE ([estacionamentoId]),
      CONSTRAINT FK_Vagas_Estacionamento      FOREIGN KEY ([estacionamentoId])
          REFERENCES [dbo].[Estacionamento] ([id]) ON DELETE CASCADE,
      CONSTRAINT CK_Vagas_Disponiveis         CHECK ([qtdVagasDisponiveis] >= 0),
      CONSTRAINT CK_Vagas_Totais              CHECK ([qtdVagasTotais] >= 0),
      CONSTRAINT CK_Vagas_Limite              CHECK ([qtdVagasDisponiveis] <= [qtdVagasTotais]),
      CONSTRAINT CK_Vagas_Reservadas          CHECK ([qtdVagasReservadas] >= 0),
      CONSTRAINT CK_Vagas_Reservadas_Limite   CHECK ([qtdVagasReservadas] <= [qtdVagasReservaveis])
  );
  GO

  -- clienteId usa BIGINT para consistência com a entidade JPA (Long)
  -- Coluna [placa] consolidada do ALTER 03 (consumida pelos painéis Operação/Financeiro
  -- e populada na entrada via app a partir do request).
  CREATE TABLE [dbo].[SessaoEstacionamento] (
      [id]               [bigint]        IDENTITY(1,1) NOT NULL,
      [clienteId]        [bigint]        NULL, -- NULL para entradas anonimas geradas pelo kiosk
      [estacionamentoId] [int]           NOT NULL,
      [horaEntrada]      [datetime2]     NOT NULL,
      [inicioReservaPrevisto] [datetime2] NULL,
      [horaSaida]        [datetime2]     NULL,
      [horaPagamento]    [datetime2]     NULL,
      [valorPago]        [decimal](10,2) NULL, -- valor pago/faturado; no kiosk inicia com precoHora minimo
      [status]           [nvarchar](10)  NOT NULL,
      [qrCode]           [varchar](64)   NULL,
      [reservado]        [bit]           NOT NULL DEFAULT 0,
      [placa]            [varchar](7)    NULL,

      CONSTRAINT PK_SessaoEstacionamento  PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT FK_Sessao_Cliente        FOREIGN KEY ([clienteId])
          REFERENCES [dbo].[Cliente] ([id]),
      CONSTRAINT FK_Sessao_Estacionamento FOREIGN KEY ([estacionamentoId])
          REFERENCES [dbo].[Estacionamento] ([id]),
      CONSTRAINT CK_Sessao_Status         CHECK ([status] IN ('ATIVA','ENCERRADA','CANCELADA')),
      CONSTRAINT CK_Sessao_ValorPago      CHECK ([valorPago] IS NULL OR [valorPago] >= 0)
  );
  GO

  CREATE TABLE [dbo].[Vaga] (
      [id]               [int]          IDENTITY(1,1) NOT NULL,
      [estacionamentoId] [int]          NOT NULL,
      [numero]           [nvarchar](10) NOT NULL,
      [tipo]             [nvarchar](50) NOT NULL DEFAULT 'Regular',
      [ocupada]          [bit]          NOT NULL DEFAULT 0,

      CONSTRAINT PK_Vaga                PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT FK_Vaga_Estacionamento FOREIGN KEY ([estacionamentoId])
          REFERENCES [dbo].[Estacionamento] ([id]) ON DELETE CASCADE,
      CONSTRAINT UQ_Vaga_Numero         UNIQUE ([estacionamentoId], [numero])
  );
  GO

  CREATE TABLE [dbo].[Avaliacao] (
      [id]               [int]          IDENTITY(1,1) NOT NULL,
      [estacionamentoId] [int]          NOT NULL,
      [clienteId]        [bigint]       NOT NULL,
      [nota]             [int]          NOT NULL,
      [comentario]       [varchar](500) NULL,
      [dataAvaliacao]    [datetime2]    NOT NULL,

      CONSTRAINT PK_Avaliacao                   PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT CK_Avaliacao_Nota              CHECK ([nota] BETWEEN 1 AND 5),
      CONSTRAINT FK_Avaliacao_Estacionamento    FOREIGN KEY ([estacionamentoId])
          REFERENCES [dbo].[Estacionamento] ([id]),
      CONSTRAINT FK_Avaliacao_Cliente           FOREIGN KEY ([clienteId])
          REFERENCES [dbo].[Cliente] ([id])
  );
  GO

  CREATE TABLE [dbo].[FormaPagamento] (
      [id]            [int]           IDENTITY(1,1) NOT NULL,
      [clienteId]     [bigint]        NOT NULL,
      [tipoPagamento] [nvarchar](50)  NOT NULL,
      [numeroCartao]  [varchar](4)    NULL,   -- apenas os últimos 4 dígitos
      [nomeCartao]    [nvarchar](100) NULL,
      [validade]      [varchar](7)    NULL,
      [bandeira]      [nvarchar](50)  NULL,

      CONSTRAINT PK_FormaPagamento         PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT FK_FormaPagamento_Cliente FOREIGN KEY ([clienteId])
          REFERENCES [dbo].[Cliente] ([id]) ON DELETE CASCADE
  );
  GO

  -- =============================================
  -- 4. TABELAS DE ADMINISTRAÇÃO
  -- =============================================

  -- AdmEstacionamento: login do painel WEB (admin-dashboard.html etc.)
  -- Colunas [email] e [telefone] consolidadas do ALTER 02.
  CREATE TABLE [dbo].[AdmEstacionamento] (
      [id]               [int]           IDENTITY(1,1) NOT NULL,
      [estacionamentoId] [int]           NOT NULL,
      [usuario]          [varchar](50)   NOT NULL,
      [senhaHash]        [nvarchar](255) NOT NULL,
      [nomeCompleto]     [nvarchar](100) NOT NULL,
      [ativo]            [bit]           NOT NULL DEFAULT 1,
      [email]            [nvarchar](200) NULL,
      [telefone]         [varchar](20)   NULL,
      [cpf]              [nvarchar](11)  NOT NULL,
      [dataNascimento]   [date]          NULL,

      CONSTRAINT PK_AdmEstacionamento        PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT UQ_Adm_Usuario              UNIQUE ([usuario]),
      CONSTRAINT UQ_Adm_CPF                  UNIQUE ([cpf]),
      CONSTRAINT CK_Adm_CPF                  CHECK (LEN([cpf]) = 11),
      CONSTRAINT FK_Adm_Estacionamento       FOREIGN KEY ([estacionamentoId])
          REFERENCES [dbo].[Estacionamento] ([id])
  );
  GO

  -- OperadorEstacionamento (ALTER 05 + 06): login do KIOSK (ParadaCertaManager.html)
  -- Separa o operador de balcão do administrador web.
  -- Cada estacionamento pode ter N operadores; o admin web cadastra/gerencia.
  -- LGPD (ALTER 06): cpf, email, telefone e endereço completos dos operadores.
  CREATE TABLE [dbo].[OperadorEstacionamento] (
      [id]               [int]           IDENTITY(1,1) NOT NULL,
      [estacionamentoId] [int]           NOT NULL,
      [nome]             [nvarchar](100) NOT NULL,
      [usuario]          [nvarchar](50)  NOT NULL,
      [senhaHash]        [nvarchar](255) NOT NULL,
      [ativo]            [bit]           NOT NULL CONSTRAINT DF_OperadorEstac_ativo DEFAULT 1,
      [criadoEm]         [datetime2]     NOT NULL CONSTRAINT DF_OperadorEstac_criadoEm DEFAULT SYSDATETIME(),

      -- Dados pessoais LGPD (ALTER 06)
      [cpf]              [varchar](11)   NULL,
      [email]            [nvarchar](200) NULL,
      [telefone]         [varchar](20)   NULL,

      -- Endereço estruturado (ALTER 06)
      [cep]              [varchar](8)    NULL,
      [logradouro]       [nvarchar](200) NULL,
      [numero]           [varchar](10)   NULL,
      [complemento]      [nvarchar](100) NULL,
      [bairro]           [nvarchar](100) NULL,
      [cidade]           [nvarchar](100) NULL,
      [uf]               [char](2)       NULL,

      CONSTRAINT PK_OperadorEstac PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT UQ_OperadorEstac_estac_usuario UNIQUE ([estacionamentoId], [usuario]),
      CONSTRAINT FK_OperadorEstac_Estacionamento
          FOREIGN KEY ([estacionamentoId])
          REFERENCES [dbo].[Estacionamento] ([id]),
      CONSTRAINT CK_OperadorEstac_CPF   CHECK ([cpf]   IS NULL OR LEN([cpf]) = 11),
      CONSTRAINT CK_OperadorEstac_Email CHECK ([email] IS NULL OR [email] LIKE '%@%.%')
  );
  GO

  -- EstacionamentoFoto (ALTER 08): multiplas fotos por estacionamento
  -- com magic bytes + Google Vision validados pelo backend antes da insercao.
  -- Limite por plano (validado em runtime): BASIC=3, STANDARD=3, PREMIUM=5.
  CREATE TABLE [dbo].[EstacionamentoFoto] (
      [id]               [int]           IDENTITY(1,1) NOT NULL,
      [estacionamentoId] [int]           NOT NULL,
      [caminho]          [nvarchar](500) NOT NULL,
      [nomeOriginal]     [nvarchar](255) NULL,
      [tipoMime]         [varchar](50)   NOT NULL,
      [tamanhoBytes]     [bigint]        NOT NULL,
      [principal]        [bit]           NOT NULL CONSTRAINT DF_EstacFoto_principal DEFAULT 0,
      [ordem]            [int]           NOT NULL CONSTRAINT DF_EstacFoto_ordem     DEFAULT 0,
      [criadoEm]         [datetime2]     NOT NULL CONSTRAINT DF_EstacFoto_criadoEm  DEFAULT SYSDATETIME(),

      CONSTRAINT PK_EstacFoto PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT FK_EstacFoto_Estacionamento FOREIGN KEY ([estacionamentoId])
          REFERENCES [dbo].[Estacionamento] ([id]) ON DELETE CASCADE,
      CONSTRAINT CK_EstacFoto_Mime    CHECK ([tipoMime] IN ('image/jpeg','image/png','image/webp')),
      CONSTRAINT CK_EstacFoto_Tamanho CHECK ([tamanhoBytes] > 0)
  );
  GO

  -- AssinaturaPlanoPagamento (ALTER 11): recibos de ativação/renovação de plano.
  -- LGPD/PCI-DSS: armazena somente os ULTIMOS 4 DIGITOS do cartão; CVV e numero
  -- completo NUNCA são gravados.
  CREATE TABLE [dbo].[AssinaturaPlanoPagamento] (
      [id]               [bigint]        IDENTITY(1,1) NOT NULL,
      [estacionamentoId] [int]           NOT NULL,
      [plano]            [varchar](10)   NOT NULL,
      [cobranca]         [varchar](10)   NOT NULL,
      [valor]            [decimal](10,2) NOT NULL,
      [status]           [varchar](15)   NOT NULL,
      [dataPagamento]    [datetime2]     NOT NULL,
      [ultimos4]         [varchar](4)    NULL,
      [bandeira]         [nvarchar](50)  NULL,
      [nomeCartao]       [nvarchar](100) NULL,

      CONSTRAINT PK_AssinaturaPlanoPagamento PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT FK_AssinaturaPagto_Estacionamento FOREIGN KEY ([estacionamentoId])
          REFERENCES [dbo].[Estacionamento] ([id]),
      CONSTRAINT CK_AssinaturaPagto_Plano    CHECK ([plano]    IN ('BASIC','STANDARD','PREMIUM')),
      CONSTRAINT CK_AssinaturaPagto_Cobranca CHECK ([cobranca] IN ('MENSAL','ANUAL','TRIAL')),
      CONSTRAINT CK_AssinaturaPagto_Status   CHECK ([status]   IN ('APROVADO','RECUSADO','PENDENTE')),
      CONSTRAINT CK_AssinaturaPagto_Ultimos4 CHECK ([ultimos4] IS NULL OR LEN([ultimos4]) = 4),
      CONSTRAINT CK_AssinaturaPagto_Valor    CHECK ([valor] >= 0)
  );
  GO

  CREATE TABLE [dbo].[QrCodeEntrada] (
      [id]               [bigint]        IDENTITY(1,1) NOT NULL,
      [token]            [varchar](64)   NOT NULL,
      [estacionamentoId] [int]           NOT NULL,
      [geradoPor]        [int]           NOT NULL, -- operador do kiosk
      [geradoEm]         [datetime2]     NOT NULL DEFAULT SYSDATETIME(),
      [expiradoEm]       [datetime2]     NOT NULL,
      [status]           [varchar](15)   NOT NULL DEFAULT 'DISPONIVEL',

      CONSTRAINT PK_QrCodeEntrada           PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT UQ_QrCode_Token            UNIQUE ([token]),
      CONSTRAINT CK_QrCode_Status           CHECK ([status] IN ('DISPONIVEL','UTILIZADO','EXPIRADO')),
      CONSTRAINT FK_QrCode_Estacionamento   FOREIGN KEY ([estacionamentoId])
          REFERENCES [dbo].[Estacionamento] ([id]),
      CONSTRAINT FK_QrCode_Operador        FOREIGN KEY ([geradoPor])
          REFERENCES [dbo].[OperadorEstacionamento] ([id])
  );
  GO

  -- =============================================
  -- 5. ÍNDICES
  -- =============================================

  CREATE NONCLUSTERED INDEX IX_Avaliacao_Estacionamento
      ON [dbo].[Avaliacao] ([estacionamentoId]);
  GO
  CREATE NONCLUSTERED INDEX IX_Vaga_Estacionamento
      ON [dbo].[Vaga] ([estacionamentoId]);
  GO
  CREATE NONCLUSTERED INDEX IX_Endereco_ClienteId
      ON [dbo].[Endereco] ([clienteId]);
  GO
  CREATE NONCLUSTERED INDEX IX_Estacionamento_Localizacao
      ON [dbo].[Estacionamento] ([latitude], [longitude]);
  GO
  CREATE NONCLUSTERED INDEX IX_Sessao_ClienteStatus
      ON [dbo].[SessaoEstacionamento] ([clienteId], [status]);
  GO
  CREATE UNIQUE NONCLUSTERED INDEX UQ_Sessao_QrCode
      ON [dbo].[SessaoEstacionamento] ([qrCode])
      WHERE [qrCode] IS NOT NULL;
  GO
  CREATE NONCLUSTERED INDEX IX_QrCode_Token
      ON [dbo].[QrCodeEntrada] ([token]);
  GO
  CREATE NONCLUSTERED INDEX IX_QrCode_Status
      ON [dbo].[QrCodeEntrada] ([estacionamentoId], [status]);
  GO
  CREATE NONCLUSTERED INDEX IX_Vagas_EstacionamentoId
      ON [dbo].[VagasEstacionamento] ([estacionamentoId]);
  GO

  -- Índice único parcial em AdmEstacionamento.email (ALTER 02)
  -- Ignora registros NULL para não quebrar com dados antigos.
  CREATE UNIQUE NONCLUSTERED INDEX UQ_Adm_Email
      ON [dbo].[AdmEstacionamento] ([email])
      WHERE [email] IS NOT NULL;
  GO

  -- Índice de apoio em OperadorEstacionamento (ALTER 05)
  CREATE NONCLUSTERED INDEX IX_OperadorEstac_estacionamentoId
      ON [dbo].[OperadorEstacionamento] ([estacionamentoId])
      INCLUDE ([ativo]);
  GO

  -- Índice único parcial em OperadorEstacionamento.cpf (ALTER 06)
  CREATE UNIQUE NONCLUSTERED INDEX UQ_OperadorEstac_cpf
      ON [dbo].[OperadorEstacionamento] ([cpf])
      WHERE [cpf] IS NOT NULL;
  GO

  -- Índice de apoio em EstacionamentoFoto (ALTER 08)
  CREATE NONCLUSTERED INDEX IX_EstacFoto_estacionamentoId
      ON [dbo].[EstacionamentoFoto] ([estacionamentoId])
      INCLUDE ([principal], [ordem]);
  GO

  -- Índice de apoio em AssinaturaPlanoPagamento (ALTER 11)
  CREATE NONCLUSTERED INDEX IX_AssinaturaPagto_Estacionamento
      ON [dbo].[AssinaturaPlanoPagamento] ([estacionamentoId], [dataPagamento] DESC);
  GO

  -- =============================================
  -- 6. TRIGGERS
  -- =============================================

  CREATE OR ALTER TRIGGER TR_Avaliacao_AtualizaMedia
  ON [dbo].[Avaliacao]
  AFTER INSERT, UPDATE, DELETE
  AS
  BEGIN
      SET NOCOUNT ON;
      UPDATE e
      SET e.avaliacaoMedia = ISNULL((
          SELECT AVG(CAST(a.nota AS DECIMAL(3,2)))
          FROM Avaliacao a
          WHERE a.estacionamentoId = e.id
      ), 0.0)
      FROM Estacionamento e
      WHERE e.id IN (
          SELECT DISTINCT estacionamentoId FROM inserted
          UNION
          SELECT DISTINCT estacionamentoId FROM deleted
      );
  END;
  GO

  -- Atualiza VagasEstacionamento quando sessões mudam de status.
  -- Estacionamento não armazena mais contagens — responsabilidade é desta tabela.
  CREATE OR ALTER TRIGGER TR_Sessao_AtualizaVagas
  ON [dbo].[SessaoEstacionamento]
  AFTER INSERT, UPDATE, DELETE
  AS
  BEGIN
      SET NOCOUNT ON;
      UPDATE v
      SET v.qtdVagasDisponiveis = v.qtdVagasTotais - ISNULL((
              SELECT COUNT(*) FROM SessaoEstacionamento s
              WHERE s.estacionamentoId = v.estacionamentoId AND s.status = 'ATIVA'
          ), 0),
          v.qtdVagasReservadas = ISNULL((
              SELECT COUNT(*) FROM SessaoEstacionamento s
              WHERE s.estacionamentoId = v.estacionamentoId AND s.status = 'ATIVA' AND s.reservado = 1
          ), 0)
      FROM VagasEstacionamento v
      WHERE v.estacionamentoId IN (
          SELECT DISTINCT estacionamentoId FROM inserted
          UNION
          SELECT DISTINCT estacionamentoId FROM deleted
      );
  END;
  GO

  -- =============================================
  -- 7. STORED PROCEDURES
  -- =============================================

  CREATE OR ALTER PROCEDURE sp_RegistrarEntrada
      @qrCode           VARCHAR(64),
      @clienteId        BIGINT,
      @estacionamentoId INT,
      @sessaoId         BIGINT    OUTPUT,
      @horaEntrada      DATETIME2 OUTPUT
  AS
  BEGIN
      SET NOCOUNT ON;
      BEGIN TRY
          BEGIN TRANSACTION;

          -- 1. Idempotência
          IF EXISTS (
              SELECT 1 FROM SessaoEstacionamento WITH (UPDLOCK, HOLDLOCK)
              WHERE qrCode = @qrCode
          )
          BEGIN
              SELECT @sessaoId    = id,
                     @horaEntrada = horaEntrada
              FROM   SessaoEstacionamento
              WHERE  qrCode = @qrCode;
              COMMIT;
              RETURN;
          END

          -- 2. Valida QR Code
          DECLARE @qrStatus    VARCHAR(15);
          DECLARE @qrExpiradoEm DATETIME2;

          SELECT @qrStatus     = status,
                 @qrExpiradoEm = expiradoEm
          FROM   QrCodeEntrada WITH (UPDLOCK, HOLDLOCK)
          WHERE  token = @qrCode;

          IF @qrStatus IS NULL
          BEGIN
              ROLLBACK;
              THROW 50004, 'QR Code não encontrado', 1;
          END

          IF @qrStatus != 'DISPONIVEL'
          BEGIN
              ROLLBACK;
              THROW 50003, 'QR Code já utilizado ou inválido', 1;
          END

          IF SYSDATETIME() > @qrExpiradoEm
          BEGIN
              UPDATE QrCodeEntrada SET status = 'EXPIRADO' WHERE token = @qrCode;
              ROLLBACK;
              THROW 50005, 'QR Code expirado. Solicite um novo ao operador', 1;
          END

          -- 3. Verifica disponibilidade em VagasEstacionamento (lock evita overbooking)
          DECLARE @disponiveis INT;
          SELECT @disponiveis = qtdVagasDisponiveis
          FROM   VagasEstacionamento WITH (UPDLOCK, HOLDLOCK)
          WHERE  estacionamentoId = @estacionamentoId;

          IF @disponiveis IS NULL OR @disponiveis <= 0
          BEGIN
              ROLLBACK;
              THROW 50001, 'Estacionamento lotado', 1;
          END

          -- 4. Registra a entrada
          SET @horaEntrada = SYSDATETIME();

          INSERT INTO SessaoEstacionamento
              (qrCode, clienteId, estacionamentoId, horaEntrada, status)
          VALUES
              (@qrCode, @clienteId, @estacionamentoId, @horaEntrada, 'ATIVA');

          SET @sessaoId = SCOPE_IDENTITY();

          -- 5. Marca o QR como utilizado
          UPDATE QrCodeEntrada
          SET    status = 'UTILIZADO'
          WHERE  token  = @qrCode;

          COMMIT;
      END TRY
      BEGIN CATCH
          IF @@TRANCOUNT > 0 ROLLBACK;
          THROW;
      END CATCH
  END;
  GO

  CREATE OR ALTER PROCEDURE sp_RegistrarPagamento
      @qrCode    VARCHAR(64),
      @valorPago DECIMAL(10,2),
      @horaSaida DATETIME2 OUTPUT
  AS
  BEGIN
      SET NOCOUNT ON;
      BEGIN TRY
          BEGIN TRANSACTION;

          SET @horaSaida = SYSDATETIME();

          UPDATE SessaoEstacionamento
          SET    status        = 'ENCERRADA',
                 horaPagamento = @horaSaida,
                 horaSaida     = @horaSaida,
                 valorPago     = @valorPago
          WHERE  qrCode = @qrCode
            AND  status = 'ATIVA';

          IF @@ROWCOUNT = 0
          BEGIN
              ROLLBACK;
              THROW 50002, 'QR Code inválido ou sessão já encerrada', 1;
          END

          COMMIT;
      END TRY
      BEGIN CATCH
          IF @@TRANCOUNT > 0 ROLLBACK;
          THROW;
      END CATCH
  END;
  GO


  -- =============================================
  -- 9. RECONCILIAÇÃO INICIAL DE VAGAS
  -- =============================================

  UPDATE v
  SET v.qtdVagasDisponiveis = v.qtdVagasTotais - ISNULL((
          SELECT COUNT(*) FROM SessaoEstacionamento s
          WHERE s.estacionamentoId = v.estacionamentoId AND s.status = 'ATIVA'
      ), 0),
      v.qtdVagasReservadas = ISNULL((
          SELECT COUNT(*) FROM SessaoEstacionamento s
          WHERE s.estacionamentoId = v.estacionamentoId AND s.status = 'ATIVA' AND s.reservado = 1
      ), 0)
  FROM VagasEstacionamento v;
  GO

  -- =============================================
  -- 10. POPULAR COM DADOS DE EXEMPLO
  -- =============================================

  -- Seeds entram em BASIC com trial de 30 dias (ALTER 07).
  DECLARE @agoraSeed DATETIME2 = SYSDATETIME();
  DECLARE @fimSeed   DATETIME2 = DATEADD(DAY, 30, @agoraSeed);

  INSERT INTO [dbo].[Estacionamento]
  ([nome], [cnpj], [razaoSocial], [nomeFantasia], [avaliacaoMedia], [latitude], [longitude],
   [endereco], [precoHora], [horarioAbertura], [horarioFechamento], [ativo], [pixKey],
   [permiteReserva],
   [cep], [logradouro], [numero], [complemento], [bairro], [cidade], [uf],
   [plano], [planoInicio], [planoFim], [planoCobranca])
  VALUES
  ('Estacionamento Center Park', '90000000000184', 'Estacionamento Center Park LTDA', 'Estacionamento Center Park', 0.0, -23.55052000, -46.63330800,
   'Av. Paulista, 1000 - Bela Vista, São Paulo - SP', 15.00, '06:00:00', '22:00:00', 1, 'centerpark@pix.com', 1,
   '01310100', 'Avenida Paulista', '1000', NULL, 'Bela Vista', 'São Paulo', 'SP',
   'BASIC', @agoraSeed, @fimSeed, 'TRIAL'),

  ('Parking Paulista', '90000000000265', 'Parking Paulista LTDA', 'Parking Paulista', 0.0, -23.56141400, -46.65617800,
   'Rua Augusta, 2000 - Consolação, São Paulo - SP', 12.00, '00:00:00', '23:59:59', 1, 'parkingpaulista@pix.com', 1,
   '01304001', 'Rua Augusta', '2000', NULL, 'Consolação', 'São Paulo', 'SP',
   'BASIC', @agoraSeed, @fimSeed, 'TRIAL'),

  ('Vila Madalena Estacionamento', '90000000000346', 'Vila Madalena Estacionamento LTDA', 'Vila Madalena Estacionamento', 0.0, -23.54538000, -46.69023000,
   'Rua Harmonia, 500 - Vila Madalena, São Paulo - SP', 10.00, '08:00:00', '20:00:00', 1, 'vilamadalena@pix.com', 0,
   '05435000', 'Rua Harmonia', '500', NULL, 'Vila Madalena', 'São Paulo', 'SP',
   'BASIC', @agoraSeed, @fimSeed, 'TRIAL'),

  ('Ibirapuera Park Auto', '90000000000427', 'Ibirapuera Park Auto LTDA', 'Ibirapuera Park Auto', 0.0, -23.58741600, -46.65763400,
   'Av. Pedro Álvares Cabral - Vila Mariana, São Paulo - SP', 18.00, '06:00:00', '23:00:00', 1, 'ibirapuera@pix.com', 0,
   '04094050', 'Avenida Pedro Álvares Cabral', 'S/N', NULL, 'Vila Mariana', 'São Paulo', 'SP',
   'BASIC', @agoraSeed, @fimSeed, 'TRIAL'),

  ('Jardins Premium', '90000000000508', 'Jardins Premium LTDA', 'Jardins Premium', 0.0, -23.56194800, -46.67254100,
   'Rua Oscar Freire, 800 - Jardins, São Paulo - SP', 25.00, '07:00:00', '22:00:00', 1, 'jardinspremium@pix.com', 1,
   '01426001', 'Rua Oscar Freire', '800', NULL, 'Jardins', 'São Paulo', 'SP',
   'BASIC', @agoraSeed, @fimSeed, 'TRIAL'),

  ('Estacionamento Faria Lima', '90000000000699', 'Estacionamento Faria Lima LTDA', 'Estacionamento Faria Lima', 0.0, -23.57652000, -46.68893000,
   'Av. Brigadeiro Faria Lima, 2000 - Itaim Bibi, São Paulo - SP', 20.00, '06:00:00', '00:00:00', 1, 'farialima@pix.com', 1,
   '01452000', 'Avenida Brigadeiro Faria Lima', '2000', NULL, 'Itaim Bibi', 'São Paulo', 'SP',
   'BASIC', @agoraSeed, @fimSeed, 'TRIAL'),

  ('Pinheiros Auto Park', '90000000000770', 'Pinheiros Auto Park LTDA', 'Pinheiros Auto Park', 0.0, -23.56235000, -46.68412000,
   'Rua dos Pinheiros, 300 - Pinheiros, São Paulo - SP', 14.00, '08:00:00', '22:00:00', 1, 'pinheiros@pix.com', 0,
   '05422000', 'Rua dos Pinheiros', '300', NULL, 'Pinheiros', 'São Paulo', 'SP',
   'BASIC', @agoraSeed, @fimSeed, 'TRIAL'),

  ('República Center', '90000000000850', 'República Center LTDA', 'República Center', 0.0, -23.54317000, -46.64282000,
   'Praça da República, 100 - República, São Paulo - SP', 8.00, '00:00:00', '23:59:59', 1, 'republica@pix.com', 0,
   '01045000', 'Praça da República', '100', NULL, 'República', 'São Paulo', 'SP',
   'BASIC', @agoraSeed, @fimSeed, 'TRIAL');
  GO

  -- VagasEstacionamento: IDs 1-8 gerados acima pelo IDENTITY
  INSERT INTO [dbo].[VagasEstacionamento]
  ([estacionamentoId], [qtdVagasTotais], [qtdVagasDisponiveis], [qtdVagasReservaveis], [qtdVagasReservadas])
  VALUES
  (1, 100, 100, 10, 0),
  (2,  80,  80,  8, 0),
  (3,  60,  60,  0, 0),
  (4, 150, 150,  0, 0),
  (5,  50,  50, 15, 0),
  (6, 120, 120, 20, 0),
  (7,  70,  70,  0, 0),
  (8,  90,  90,  0, 0);
  GO

  -- Usuário administrador WEB de exemplo (senha: admin123)
  INSERT INTO [dbo].[AdmEstacionamento]
  ([estacionamentoId], [usuario], [senhaHash], [nomeCompleto], [email], [telefone], [cpf], [dataNascimento])
  VALUES
  (1, 'operador1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   'João Operador', 'admin@paradacerta.com', '11999990000', '52998224725', NULL);
  GO

  -- Operador de KIOSK de exemplo (senha: admin123) — ALTER 05 + 06 (dados pessoais LGPD).
  -- CPF ficticio valido (algoritmo verifica), TCC apenas — substituir em producao.
  INSERT INTO [dbo].[OperadorEstacionamento]
  ([estacionamentoId], [nome], [usuario], [senhaHash], [ativo],
   [cpf], [email], [telefone],
   [cep], [logradouro], [numero], [complemento], [bairro], [cidade], [uf])
  VALUES
  (1, 'Operador Demo', 'operador1_kiosk', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1,
   '52998224725', 'operador.demo@paradacerta.com', '11999990001',
   '01310100', 'Avenida Paulista', '1000', NULL, 'Bela Vista', 'São Paulo', 'SP');
  GO
 

  -- =============================================
  -- 11. MASSA COMPLETA DE DADOS PARA EXECUCAO DA APLICACAO
  -- =============================================
  -- Credenciais demo:
  --   Admin web principal: admin@paradacerta.com / admin123
  --   Kiosk principal:     operador1_kiosk / admin123 / estacionamento ID 1
  --   Motoristas:          e-mails/CPFs abaixo / motorista123
  --
  -- CPFs/CNPJs/e-mails abaixo sao ficticios para TCC.

  DECLARE @senhaAdminDemo NVARCHAR(255) = '$2a$10$DKMYxpVv5HBAAcgFC74Vs.fp/6JCphSRrTciClrXpYNsNL1ZmuDUC';
  DECLARE @senhaMotoristaDemo NVARCHAR(255) = '$2a$10$bIgbxOVENKRYp7V4OyLbMeTTRkHPrKYttrU2vnWCaToqm0kq3lK9O';
  DECLARE @agoraDemo DATETIME2 = SYSDATETIME();

  UPDATE [dbo].[AdmEstacionamento]
     SET [senhaHash] = @senhaAdminDemo,
         [usuario] = 'admin_center',
         [nomeCompleto] = 'Administrador Center Park',
         [email] = 'admin@paradacerta.com',
         [telefone] = '11999990000',
         [dataNascimento] = '1985-04-12'
   WHERE [id] = 1;

  UPDATE [dbo].[OperadorEstacionamento]
     SET [senhaHash] = @senhaAdminDemo,
         [nome] = 'Operador Center Park',
         [usuario] = 'operador1_kiosk',
         [email] = 'operador.center@paradacerta.com'
   WHERE [id] = 1;

  UPDATE [dbo].[Estacionamento]
     SET [descricao] = CASE [id]
          WHEN 1 THEN 'Estacionamento central com reserva pelo app, kiosk de entrada, Pix e relatorios operacionais.'
          WHEN 2 THEN 'Estacionamento 24 horas proximo a Avenida Paulista, com vagas para reserva e painel financeiro.'
          WHEN 3 THEN 'Estacionamento de bairro para rotatividade local, sem reservas antecipadas.'
          WHEN 4 THEN 'Operacao grande proxima ao Ibirapuera, ideal para fluxo de eventos e entrada via QR Code.'
          WHEN 5 THEN 'Unidade premium nos Jardins com destaque no mapa, relatorios regionais e avaliacao elevada.'
          WHEN 6 THEN 'Estacionamento corporativo na Faria Lima com reservas, Pix e alta demanda em horario comercial.'
          WHEN 7 THEN 'Unidade em Pinheiros com foco em estadias curtas e recorrencia diaria.'
          ELSE 'Estacionamento no centro com funcionamento 24 horas e bom volume de sessoes.'
         END,
         [fotoPrincipal] = CONCAT('uploads/estacionamento/', [id], '/principal-demo.webp'),
         [plano] = CASE WHEN [id] IN (5,6) THEN 'PREMIUM'
                        WHEN [id] IN (2,4) THEN 'STANDARD'
                        ELSE [plano] END,
         [planoCobranca] = CASE WHEN [id] IN (2,4,5,6) THEN 'MENSAL' ELSE [planoCobranca] END,
         [planoInicio] = CASE WHEN [id] IN (2,4,5,6) THEN DATEADD(DAY, -10, @agoraDemo) ELSE [planoInicio] END,
         [planoFim] = CASE WHEN [id] IN (2,4,5,6) THEN DATEADD(DAY, 50, @agoraDemo) ELSE [planoFim] END
   WHERE [id] BETWEEN 1 AND 8;
  GO

  DECLARE @senhaAdminDemo NVARCHAR(255) = '$2a$10$DKMYxpVv5HBAAcgFC74Vs.fp/6JCphSRrTciClrXpYNsNL1ZmuDUC';

  INSERT INTO [dbo].[AdmEstacionamento]
  ([estacionamentoId], [usuario], [senhaHash], [nomeCompleto], [ativo], [email], [telefone], [cpf], [dataNascimento])
  VALUES
  (2, 'admin_paulista',   @senhaAdminDemo, 'Marina Azevedo', 1, 'admin.paulista@paradacerta.com',   '11970000002', '39053344705', '1990-08-21'),
  (3, 'admin_vila',       @senhaAdminDemo, 'Lucas Martins',  1, 'admin.vila@paradacerta.com',       '11970000003', '11144477735', '1988-02-10'),
  (4, 'admin_ibirapuera', @senhaAdminDemo, 'Renata Lopes',   1, 'admin.ibirapuera@paradacerta.com', '11970000004', '12345678909', '1982-11-05'),
  (5, 'admin_jardins',    @senhaAdminDemo, 'Bruno Cardoso',  1, 'admin.jardins@paradacerta.com',    '11970000005', '93541134780', '1993-06-18'),
  (6, 'admin_faria',      @senhaAdminDemo, 'Camila Rocha',   1, 'admin.faria@paradacerta.com',      '11970000006', '26875543006', '1987-09-14'),
  (7, 'admin_pinheiros',  @senhaAdminDemo, 'Paulo Nogueira', 1, 'admin.pinheiros@paradacerta.com',  '11970000007', '09798765432', '1991-03-25'),
  (8, 'admin_republica',  @senhaAdminDemo, 'Ana Ribeiro',    1, 'admin.republica@paradacerta.com',  '11970000008', '27865743085', '1989-12-01');
  GO

  DECLARE @senhaAdminDemo NVARCHAR(255) = '$2a$10$DKMYxpVv5HBAAcgFC74Vs.fp/6JCphSRrTciClrXpYNsNL1ZmuDUC';

  INSERT INTO [dbo].[OperadorEstacionamento]
  ([estacionamentoId], [nome], [usuario], [senhaHash], [ativo],
   [cpf], [email], [telefone], [cep], [logradouro], [numero], [complemento], [bairro], [cidade], [uf])
  VALUES
  (2, 'Operador Paulista',   'operador2_kiosk', @senhaAdminDemo, 1, '54017896549', 'operador.paulista@paradacerta.com',   '11980000002', '01304001', 'Rua Augusta', '2000', NULL, 'Consolacao',    'Sao Paulo', 'SP'),
  (3, 'Operador Vila',       'operador3_kiosk', @senhaAdminDemo, 1, '34244419888', 'operador.vila@paradacerta.com',       '11980000003', '05435000', 'Rua Harmonia', '500',  NULL, 'Vila Madalena','Sao Paulo', 'SP'),
  (4, 'Operador Ibirapuera', 'operador4_kiosk', @senhaAdminDemo, 1, '87164783026', 'operador.ibirapuera@paradacerta.com', '11980000004', '04094050', 'Av Pedro Alvares Cabral', 'S/N', NULL, 'Vila Mariana', 'Sao Paulo', 'SP'),
  (5, 'Operador Jardins',    'operador5_kiosk', @senhaAdminDemo, 1, '66148296004', 'operador.jardins@paradacerta.com',    '11980000005', '01426001', 'Rua Oscar Freire', '800', NULL, 'Jardins',       'Sao Paulo', 'SP'),
  (6, 'Operador Faria Lima', 'operador6_kiosk', @senhaAdminDemo, 1, '75709548039', 'operador.faria@paradacerta.com',      '11980000006', '01452000', 'Av Brigadeiro Faria Lima', '2000', NULL, 'Itaim Bibi', 'Sao Paulo', 'SP'),
  (7, 'Operador Pinheiros',  'operador7_kiosk', @senhaAdminDemo, 1, '22513458074', 'operador.pinheiros@paradacerta.com',  '11980000007', '05422000', 'Rua dos Pinheiros', '300', NULL, 'Pinheiros',    'Sao Paulo', 'SP'),
  (8, 'Operador Republica',  'operador8_kiosk', @senhaAdminDemo, 1, '13126765007', 'operador.republica@paradacerta.com',  '11980000008', '01045000', 'Praca da Republica', '100', NULL, 'Republica',  'Sao Paulo', 'SP');
  GO

  INSERT INTO [dbo].[EstacionamentoFoto]
  ([estacionamentoId], [caminho], [nomeOriginal], [tipoMime], [tamanhoBytes], [principal], [ordem])
  VALUES
  (1, 'uploads/estacionamento/1/principal-demo.webp', 'center-principal.webp', 'image/webp', 184000, 1, 1),
  (1, 'uploads/estacionamento/1/fachada-demo.webp',   'center-fachada.webp',   'image/webp', 176000, 0, 2),
  (1, 'uploads/estacionamento/1/vagas-demo.webp',     'center-vagas.webp',     'image/webp', 162000, 0, 3),
  (2, 'uploads/estacionamento/2/principal-demo.webp', 'paulista-principal.webp', 'image/webp', 181000, 1, 1),
  (2, 'uploads/estacionamento/2/entrada-demo.webp',   'paulista-entrada.webp',   'image/webp', 151000, 0, 2),
  (3, 'uploads/estacionamento/3/principal-demo.webp', 'vila-principal.webp', 'image/webp', 145000, 1, 1),
  (4, 'uploads/estacionamento/4/principal-demo.webp', 'ibirapuera-principal.webp', 'image/webp', 190000, 1, 1),
  (5, 'uploads/estacionamento/5/principal-demo.webp', 'jardins-principal.webp', 'image/webp', 201000, 1, 1),
  (5, 'uploads/estacionamento/5/lobby-demo.webp',     'jardins-lobby.webp',     'image/webp', 198000, 0, 2),
  (5, 'uploads/estacionamento/5/vagas-demo.webp',     'jardins-vagas.webp',     'image/webp', 175000, 0, 3),
  (5, 'uploads/estacionamento/5/coberto-demo.webp',   'jardins-coberto.webp',   'image/webp', 165000, 0, 4),
  (5, 'uploads/estacionamento/5/saida-demo.webp',     'jardins-saida.webp',     'image/webp', 158000, 0, 5),
  (6, 'uploads/estacionamento/6/principal-demo.webp', 'faria-principal.webp', 'image/webp', 192000, 1, 1),
  (7, 'uploads/estacionamento/7/principal-demo.webp', 'pinheiros-principal.webp', 'image/webp', 158000, 1, 1),
  (8, 'uploads/estacionamento/8/principal-demo.webp', 'republica-principal.webp', 'image/webp', 153000, 1, 1);
  GO

  DECLARE @senhaMotoristaDemo NVARCHAR(255) = '$2a$10$bIgbxOVENKRYp7V4OyLbMeTTRkHPrKYttrU2vnWCaToqm0kq3lK9O';

  INSERT INTO [dbo].[Cliente]
  ([nome], [cpf], [email], [senha], [dataNascimento], [numeroCelular], [placa], [veiculo])
  VALUES
  ('Carlos Silva',    '52998224725', 'carlos.motorista@paradacerta.com',   @senhaMotoristaDemo, '1994-05-10', '11990000001', 'ABC1D23', 'Volkswagen Gol'),
  ('Fernanda Souza',  '39053344705', 'fernanda.motorista@paradacerta.com', @senhaMotoristaDemo, '1991-08-22', '11990000002', 'DEF4G56', 'Honda Fit'),
  ('Rafael Oliveira', '11144477735', 'rafael.motorista@paradacerta.com',   @senhaMotoristaDemo, '1988-01-19', '11990000003', 'GHI7J89', 'Toyota Corolla'),
  ('Juliana Lima',    '12345678909', 'juliana.motorista@paradacerta.com',  @senhaMotoristaDemo, '1996-12-03', '11990000004', 'JKL2M34', 'Hyundai HB20'),
  ('Marcos Pereira',  '93541134780', 'marcos.motorista@paradacerta.com',   @senhaMotoristaDemo, '1985-07-15', '11990000005', 'MNO5P67', 'Chevrolet Onix'),
  ('Beatriz Santos',  '26875543006', 'beatriz.motorista@paradacerta.com',  @senhaMotoristaDemo, '1998-03-27', '11990000006', 'QRS8T90', 'Fiat Argo');

  INSERT INTO [dbo].[Veiculo] ([nome], [placa], [cor], [clienteId])
  VALUES
  ('Volkswagen Gol', 'ABC1D23', 'Prata', 1),
  ('Honda Fit', 'DEF4G56', 'Branco', 2),
  ('Toyota Corolla', 'GHI7J89', 'Preto', 3),
  ('Hyundai HB20', 'JKL2M34', 'Vermelho', 4),
  ('Chevrolet Onix', 'MNO5P67', 'Azul', 5),
  ('Fiat Argo', 'QRS8T90', 'Cinza', 6),
  ('Jeep Renegade', 'TUV1W23', 'Preto', 1),
  ('Nissan Kicks', 'XYZ4A56', 'Branco', 2);

  INSERT INTO [dbo].[Endereco]
  ([cep], [logradouro], [numero], [complemento], [bairro], [cidade], [estado], [clienteId])
  VALUES
  ('01310100', 'Avenida Paulista', '900', NULL, 'Bela Vista', 'Sao Paulo', 'SP', 1),
  ('05422000', 'Rua dos Pinheiros', '120', 'Apto 44', 'Pinheiros', 'Sao Paulo', 'SP', 2),
  ('04094050', 'Avenida Pedro Alvares Cabral', '55', NULL, 'Vila Mariana', 'Sao Paulo', 'SP', 3),
  ('01452000', 'Avenida Brigadeiro Faria Lima', '1450', NULL, 'Itaim Bibi', 'Sao Paulo', 'SP', 4),
  ('01045000', 'Praca da Republica', '30', 'Sala 12', 'Republica', 'Sao Paulo', 'SP', 5),
  ('05435000', 'Rua Harmonia', '80', NULL, 'Vila Madalena', 'Sao Paulo', 'SP', 6);
  GO

  INSERT INTO [dbo].[FormaPagamento]
  ([clienteId], [tipoPagamento], [numeroCartao], [nomeCartao], [validade], [bandeira])
  VALUES
  (1, 'CARTAO_CREDITO', '1111', 'CARLOS SILVA', '12/2028', 'VISA'),
  (2, 'CARTAO_CREDITO', '4444', 'FERNANDA SOUZA', '10/2027', 'MASTERCARD'),
  (3, 'CARTAO_DEBITO', '0005', 'RAFAEL OLIVEIRA', '08/2029', 'ELO'),
  (4, 'PIX', NULL, 'JULIANA LIMA', NULL, 'PIX'),
  (5, 'CARTAO_CREDITO', '1004', 'MARCOS PEREIRA', '01/2030', 'MASTERCARD'),
  (6, 'CARTAO_CREDITO', '3009', 'BEATRIZ SANTOS', '06/2028', 'VISA');
  GO

  INSERT INTO [dbo].[AssinaturaPlanoPagamento]
  ([estacionamentoId], [plano], [cobranca], [valor], [status], [dataPagamento], [ultimos4], [bandeira], [nomeCartao])
  VALUES
  (2, 'STANDARD', 'MENSAL', 149.90, 'APROVADO', DATEADD(DAY, -10, SYSDATETIME()), '4444', 'MASTERCARD', 'PARKING PAULISTA LTDA'),
  (4, 'STANDARD', 'MENSAL', 149.90, 'APROVADO', DATEADD(DAY, -9,  SYSDATETIME()), '1111', 'VISA', 'IBIRAPUERA PARK AUTO LTDA'),
  (5, 'PREMIUM',  'MENSAL', 399.90, 'APROVADO', DATEADD(DAY, -8,  SYSDATETIME()), '1004', 'MASTERCARD', 'JARDINS PREMIUM LTDA'),
  (6, 'PREMIUM',  'MENSAL', 399.90, 'APROVADO', DATEADD(DAY, -7,  SYSDATETIME()), '3009', 'VISA', 'ESTACIONAMENTO FARIA LIMA LTDA');
  GO

  INSERT INTO [dbo].[QrCodeEntrada]
  ([token], [estacionamentoId], [geradoPor], [geradoEm], [expiradoEm], [status])
  VALUES
  ('DEMO-QR-DISPONIVEL-0001', 1, 1, DATEADD(MINUTE, -5,  SYSDATETIME()), DATEADD(MINUTE, 25, SYSDATETIME()), 'DISPONIVEL'),
  ('DEMO-QR-UTILIZADO-0001',  1, 1, DATEADD(HOUR,   -3,  SYSDATETIME()), DATEADD(HOUR,   -2, SYSDATETIME()), 'UTILIZADO'),
  ('DEMO-QR-EXPIRADO-0001',   2, 2, DATEADD(HOUR,   -2,  SYSDATETIME()), DATEADD(HOUR,   -1, SYSDATETIME()), 'EXPIRADO'),
  ('DEMO-QR-DISPONIVEL-0002', 5, 5, DATEADD(MINUTE, -10, SYSDATETIME()), DATEADD(MINUTE, 20, SYSDATETIME()), 'DISPONIVEL');
  GO

  INSERT INTO [dbo].[SessaoEstacionamento]
  ([clienteId], [estacionamentoId], [horaEntrada], [inicioReservaPrevisto], [horaSaida], [horaPagamento], [valorPago], [status], [qrCode], [reservado], [placa])
  VALUES
  (1, 1, DATEADD(HOUR, -5, SYSDATETIME()), NULL, DATEADD(HOUR, -3, SYSDATETIME()), DATEADD(HOUR, -3, SYSDATETIME()), 30.00, 'ENCERRADA', 'DEMO-SESSAO-APP-0001', 0, 'ABC1D23'),
  (2, 2, DATEADD(HOUR, -4, SYSDATETIME()), NULL, DATEADD(HOUR, -2, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME()), 18.00, 'ENCERRADA', 'DEMO-SESSAO-APP-0002', 0, 'DEF4G56'),
  (3, 5, DATEADD(HOUR, -6, SYSDATETIME()), NULL, DATEADD(HOUR, -2, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME()), 100.00, 'ENCERRADA', 'DEMO-SESSAO-APP-0003', 0, 'GHI7J89'),
  (4, 3, DATEADD(MINUTE, -45, SYSDATETIME()), NULL, NULL, NULL, NULL, 'ATIVA', 'DEMO-SESSAO-ATIVA-0001', 0, 'JKL2M34'),
  (5, 6, DATEADD(MINUTE, -20, SYSDATETIME()), DATEADD(MINUTE, -20, SYSDATETIME()), NULL, DATEADD(MINUTE, -20, SYSDATETIME()), 20.00, 'ATIVA', 'DEMO-RESERVA-ATIVA-0001', 1, 'MNO5P67'),
  (6, 1, DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, -1, SYSDATETIME()), DATEADD(HOUR, 1, DATEADD(DAY, -1, SYSDATETIME())), DATEADD(DAY, -1, SYSDATETIME()), 0.00, 'CANCELADA', 'DEMO-RESERVA-CANCELADA-0001', 1, 'QRS8T90'),
  (NULL, 1, DATEADD(HOUR, -3, SYSDATETIME()), NULL, DATEADD(HOUR, -1, SYSDATETIME()), DATEADD(HOUR, -1, SYSDATETIME()), 30.00, 'ENCERRADA', 'DEMO-QR-UTILIZADO-0001', 0, NULL),
  (1, 5, DATEADD(DAY, -2, SYSDATETIME()), NULL, DATEADD(HOUR, 2, DATEADD(DAY, -2, SYSDATETIME())), DATEADD(DAY, -2, SYSDATETIME()), 50.00, 'ENCERRADA', 'DEMO-SESSAO-HIST-0001', 0, 'TUV1W23'),
  (2, 6, DATEADD(DAY, -3, SYSDATETIME()), NULL, DATEADD(HOUR, 3, DATEADD(DAY, -3, SYSDATETIME())), DATEADD(DAY, -3, SYSDATETIME()), 60.00, 'ENCERRADA', 'DEMO-SESSAO-HIST-0002', 0, 'XYZ4A56'),
  (3, 4, DATEADD(DAY, -4, SYSDATETIME()), NULL, DATEADD(HOUR, 1, DATEADD(DAY, -4, SYSDATETIME())), DATEADD(DAY, -4, SYSDATETIME()), 18.00, 'ENCERRADA', 'DEMO-SESSAO-HIST-0003', 0, 'GHI7J89'),
  (4, 8, DATEADD(DAY, -5, SYSDATETIME()), NULL, DATEADD(HOUR, 4, DATEADD(DAY, -5, SYSDATETIME())), DATEADD(DAY, -5, SYSDATETIME()), 32.00, 'ENCERRADA', 'DEMO-SESSAO-HIST-0004', 0, 'JKL2M34');
  GO

  INSERT INTO [dbo].[Avaliacao]
  ([estacionamentoId], [clienteId], [nota], [comentario], [dataAvaliacao])
  VALUES
  (1, 1, 5, 'Entrada rapida e bom atendimento.', DATEADD(DAY, -5, SYSDATETIME())),
  (1, 2, 4, 'Boa localizacao e preco justo.', DATEADD(DAY, -4, SYSDATETIME())),
  (2, 3, 4, 'Funcionou bem pelo app.', DATEADD(DAY, -4, SYSDATETIME())),
  (3, 4, 3, 'Vagas simples, mas resolveu.', DATEADD(DAY, -3, SYSDATETIME())),
  (4, 5, 5, 'Otimo para eventos no Ibirapuera.', DATEADD(DAY, -3, SYSDATETIME())),
  (5, 6, 5, 'Experiencia premium e facil de pagar.', DATEADD(DAY, -2, SYSDATETIME())),
  (5, 1, 4, 'Vagas cobertas e equipe atenciosa.', DATEADD(DAY, -2, SYSDATETIME())),
  (6, 2, 5, 'Reserva funcionou perfeitamente.', DATEADD(DAY, -1, SYSDATETIME())),
  (7, 3, 4, 'Bom para estadia curta.', DATEADD(DAY, -1, SYSDATETIME())),
  (8, 4, 3, 'Boa opcao no centro.', SYSDATETIME());
  GO

  ;WITH n AS (
      SELECT TOP (160) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS i
      FROM sys.all_objects
  )
  INSERT INTO [dbo].[SessaoEstacionamento]
  ([clienteId], [estacionamentoId], [horaEntrada], [inicioReservaPrevisto], [horaSaida], [horaPagamento], [valorPago], [status], [qrCode], [reservado], [placa])
  SELECT
      CASE WHEN i % 6 = 0 THEN NULL ELSE ((i - 1) % 6) + 1 END,
      ((i - 1) % 8) + 1,
      DATEADD(MINUTE, -1 * (30 + (i * 17)), DATEADD(DAY, -1 * (i % 28), SYSDATETIME())),
      CASE WHEN i % 11 = 0 THEN DATEADD(MINUTE, -1 * (30 + (i * 17)), DATEADD(DAY, -1 * (i % 28), SYSDATETIME())) ELSE NULL END,
      DATEADD(MINUTE, 60 + ((i % 6) * 30), DATEADD(MINUTE, -1 * (30 + (i * 17)), DATEADD(DAY, -1 * (i % 28), SYSDATETIME()))),
      DATEADD(MINUTE, 60 + ((i % 6) * 30), DATEADD(MINUTE, -1 * (30 + (i * 17)), DATEADD(DAY, -1 * (i % 28), SYSDATETIME()))),
      CAST(CASE ((i - 1) % 8) + 1
          WHEN 1 THEN 15.00 WHEN 2 THEN 12.00 WHEN 3 THEN 10.00 WHEN 4 THEN 18.00
          WHEN 5 THEN 25.00 WHEN 6 THEN 20.00 WHEN 7 THEN 14.00 ELSE 8.00
      END * (1 + (i % 4) * 0.5) AS DECIMAL(10,2)),
      'ENCERRADA',
      CONCAT('DEMO-HEAT-MASTER-', RIGHT(CONCAT('0000', i), 4)),
      CASE WHEN i % 11 = 0 THEN 1 ELSE 0 END,
      CASE ((i - 1) % 6) + 1
          WHEN 1 THEN 'ABC1D23'
          WHEN 2 THEN 'DEF4G56'
          WHEN 3 THEN 'GHI7J89'
          WHEN 4 THEN 'JKL2M34'
          WHEN 5 THEN 'MNO5P67'
          ELSE 'QRS8T90'
      END
  FROM n;
  GO

  UPDATE v
  SET v.qtdVagasDisponiveis = v.qtdVagasTotais - ISNULL((
          SELECT COUNT(*) FROM SessaoEstacionamento s
          WHERE s.estacionamentoId = v.estacionamentoId AND s.status = 'ATIVA'
      ), 0),
      v.qtdVagasReservadas = ISNULL((
          SELECT COUNT(*) FROM SessaoEstacionamento s
          WHERE s.estacionamentoId = v.estacionamentoId AND s.status = 'ATIVA' AND s.reservado = 1
      ), 0)
  FROM VagasEstacionamento v;
  GO
