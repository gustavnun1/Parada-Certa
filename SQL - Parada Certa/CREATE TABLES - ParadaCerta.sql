 -- =============================================
  -- SCRIPT COMPLETO - BANCO PARADA CERTA
  -- Execução única, sem ALTER TABLE
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

  IF OBJECT_ID('dbo.Avaliacao',            'U') IS NOT NULL DROP TABLE dbo.Avaliacao;
  IF OBJECT_ID('dbo.Vaga',                 'U') IS NOT NULL DROP TABLE dbo.Vaga;
  IF OBJECT_ID('dbo.FormaPagamento',       'U') IS NOT NULL DROP TABLE dbo.FormaPagamento;
  IF OBJECT_ID('dbo.SessaoEstacionamento', 'U') IS NOT NULL DROP TABLE dbo.SessaoEstacionamento;
  IF OBJECT_ID('dbo.Estacionamento',       'U') IS NOT NULL DROP TABLE dbo.Estacionamento;
  IF OBJECT_ID('dbo.Endereco',             'U') IS NOT NULL DROP TABLE dbo.Endereco;
  IF OBJECT_ID('dbo.Veiculo',              'U') IS NOT NULL DROP TABLE dbo.Veiculo;
  IF OBJECT_ID('dbo.Cliente',              'U') IS NOT NULL DROP TABLE dbo.Cliente;
  IF OBJECT_ID('dbo.Gerente',              'U') IS NOT NULL DROP TABLE dbo.Gerente;
  GO

  -- =============================================
  -- 2. TABELAS PRINCIPAIS (MOTORISTA)
  -- =============================================

  CREATE TABLE [dbo].[Cliente] (
      [id]             [int]           IDENTITY(1,1) NOT NULL,
      [nome]           [nvarchar](200) NOT NULL,
      [cpf]            [varchar](11)   NOT NULL,
      [email]          [nvarchar](200) NOT NULL,
      [senha]          [nvarchar](255) NOT NULL,
      [dataNascimento] [date]          NOT NULL,
      [numeroCelular]  [varchar](15)   NULL,
      [placa]          [varchar](7)    NOT NULL,
      [veiculo]        [varchar](70)   NULL,
      [premium]        [bit]           NOT NULL DEFAULT 0,

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
      [clienteId] [int]           NOT NULL,

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
      [clienteId]   [int]           NOT NULL,

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

  CREATE TABLE [dbo].[Estacionamento] (
      [id]                  [int]            IDENTITY(1,1) NOT NULL,
      [nome]                [nvarchar](100)  NOT NULL,
      [qtdVagasTotais]      [int]            NOT NULL,
      [qtdVagasDisponiveis] [int]            NOT NULL DEFAULT 0,
      [avaliacaoMedia]      [decimal](3,2)   NOT NULL DEFAULT 0.0,
      [latitude]            [decimal](10,8)  NOT NULL,
      [longitude]           [decimal](11,8)  NOT NULL,
      [endereco]            [nvarchar](300)  NOT NULL,
      [precoHora]           [decimal](10,2)  NOT NULL,
      [horarioAbertura]     [time]           NULL,
      [horarioFechamento]   [time]           NULL,
      [fotoPrincipal]       [nvarchar](500)  NULL,
      [descricao]           [nvarchar](1000) NULL,
      [ativo]               [bit]            NOT NULL DEFAULT 1,
      [pixKey]              [nvarchar](200)  NULL,
      [permiteReserva]      [bit]            NOT NULL DEFAULT 0,
      [qtdVagasReservaveis] [int]            NOT NULL DEFAULT 0,

      CONSTRAINT PK_Estacionamento            PRIMARY KEY CLUSTERED ([id] ASC),
      CONSTRAINT CK_Estacionamento_Latitude   CHECK ([latitude]  >= -90  AND [latitude]  <= 90),
      CONSTRAINT CK_Estacionamento_Longitude  CHECK ([longitude] >= -180 AND [longitude] <= 180),
      CONSTRAINT CK_Estacionamento_Avaliacao  CHECK ([avaliacaoMedia] >= 0 AND [avaliacaoMedia] <= 5),
      CONSTRAINT CK_Estacionamento_PrecoHora  CHECK ([precoHora] >= 0),
      CONSTRAINT CK_Estacionamento_Vagas      CHECK ([qtdVagasDisponiveis] <= [qtdVagasTotais])
  );
  GO

  CREATE TABLE [dbo].[SessaoEstacionamento] (
      [id]               [bigint]        IDENTITY(1,1) NOT NULL,
      [clienteId]        [int]           NOT NULL,
      [estacionamentoId] [int]           NOT NULL,
      [horaEntrada]      [datetime2]     NOT NULL,
      [horaSaida]        [datetime2]     NULL,
      [horaPagamento]    [datetime2]     NULL,
      [valorPago]        [decimal](10,2) NULL,
      [status]           [nvarchar](10)  NOT NULL,
      [qrCode]           [varchar](64)   NULL,
      [reservado]        [bit]           NOT NULL DEFAULT 0,

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
      [clienteId]        [int]          NOT NULL,
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
      [clienteId]     [int]           NOT NULL,
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
  -- 4. ÍNDICES
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

  -- =============================================
  -- 5. TRIGGERS
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

  CREATE OR ALTER TRIGGER TR_Sessao_AtualizaVagas
  ON [dbo].[SessaoEstacionamento]
  AFTER INSERT, UPDATE, DELETE
  AS
  BEGIN
      SET NOCOUNT ON;
      UPDATE e
      SET e.qtdVagasDisponiveis = e.qtdVagasTotais - ISNULL((
          SELECT COUNT(*)
          FROM SessaoEstacionamento s
          WHERE s.estacionamentoId = e.id AND s.status = 'ATIVA'
      ), 0)
      FROM Estacionamento e
      WHERE e.id IN (
          SELECT DISTINCT estacionamentoId FROM inserted
          UNION
          SELECT DISTINCT estacionamentoId FROM deleted
      );
  END;
  GO

  -- =============================================
  -- 6. STORED PROCEDURES
  -- =============================================

  CREATE OR ALTER PROCEDURE sp_RegistrarEntrada
      @qrCode           VARCHAR(64),
      @clienteId        INT,
      @estacionamentoId INT
  AS
  BEGIN
      SET NOCOUNT ON;
      BEGIN TRY
          BEGIN TRANSACTION;

          IF EXISTS (
              SELECT 1 FROM SessaoEstacionamento WITH (UPDLOCK, HOLDLOCK)
              WHERE qrCode = @qrCode
          )
          BEGIN
              COMMIT;
              RETURN;
          END

          DECLARE @disponiveis INT;
          SELECT @disponiveis = qtdVagasDisponiveis
          FROM Estacionamento WITH (UPDLOCK, HOLDLOCK)
          WHERE id = @estacionamentoId;

          IF @disponiveis <= 0
          BEGIN
              ROLLBACK;
              THROW 50001, 'Estacionamento lotado', 1;
          END

          INSERT INTO SessaoEstacionamento
              (qrCode, clienteId, estacionamentoId, horaEntrada, status)
          VALUES
              (@qrCode, @clienteId, @estacionamentoId, SYSDATETIME(), 'ATIVA');

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
      @valorPago DECIMAL(10,2)
  AS
  BEGIN
      SET NOCOUNT ON;
      BEGIN TRY
          BEGIN TRANSACTION;

          UPDATE SessaoEstacionamento
          SET status        = 'ENCERRADA',
              horaPagamento = SYSDATETIME(),
              horaSaida     = SYSDATETIME(),
              valorPago     = @valorPago
          WHERE qrCode = @qrCode AND status = 'ATIVA';

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
  -- 7. MIGRAÇÃO DE SEGURANÇA: numeroCartao -> varchar(4)
  --    Execute este bloco apenas se o banco já existia com varchar(16).
  --    Se for uma instalação nova, este bloco não faz nada.
  -- =============================================

  IF EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_NAME = 'FormaPagamento'
        AND COLUMN_NAME = 'numeroCartao'
        AND CHARACTER_MAXIMUM_LENGTH > 4
  )
  BEGIN
      UPDATE FormaPagamento
      SET numeroCartao = RIGHT(REPLACE(numeroCartao, ' ', ''), 4)
      WHERE numeroCartao IS NOT NULL AND LEN(REPLACE(numeroCartao, ' ', '')) > 4;

      ALTER TABLE FormaPagamento
          ALTER COLUMN [numeroCartao] [varchar](4) NULL;
  END
  GO

  -- =============================================
  -- 8. RECONCILIAÇÃO INICIAL DE VAGAS
  -- =============================================

  UPDATE e
  SET e.qtdVagasDisponiveis = e.qtdVagasTotais - ISNULL((
      SELECT COUNT(*) FROM SessaoEstacionamento s
      WHERE s.estacionamentoId = e.id AND s.status = 'ATIVA'
  ), 0)
  FROM Estacionamento e;
  GO


-- Administradores dos estacionamentos
CREATE TABLE AdmEstacionamento (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    estacionamentoId INT NOT NULL,
    usuario          VARCHAR(50) NOT NULL,
    senhaHash        NVARCHAR(255) NOT NULL,
    nomeCompleto     NVARCHAR(100) NOT NULL,
    ativo            BIT NOT NULL DEFAULT 1,

    CONSTRAINT UQ_AdmEstacionamento_Usuario UNIQUE (usuario),
    CONSTRAINT FK_Adm_Estacionamento FOREIGN KEY (estacionamentoId)
        REFERENCES [Estacionamento](id)
);

-- QR Codes gerados pelos estacionamentos (controle de uso único)
CREATE TABLE QrCodeEntrada (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    token            VARCHAR(64) NOT NULL,
    estacionamentoId INT NOT NULL,
    geradoPor        INT NOT NULL,
    geradoEm         DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    expiradoEm       DATETIME2 NOT NULL,
    status           VARCHAR(15) NOT NULL DEFAULT 'DISPONIVEL'
                     CHECK (status IN ('DISPONIVEL','UTILIZADO','EXPIRADO')),

    CONSTRAINT UQ_QrCode_Token UNIQUE (token),
    CONSTRAINT FK_QrCode_Estacionamento FOREIGN KEY (estacionamentoId)
        REFERENCES Estacionamento(id),
    CONSTRAINT FK_QrCode_Adm FOREIGN KEY (geradoPor)
        REFERENCES AdmEstacionamento(id)
);

CREATE INDEX IX_QrCode_Token  ON QrCodeEntrada (token);
CREATE INDEX IX_QrCode_Status ON QrCodeEntrada (estacionamentoId, status);

INSERT INTO AdmEstacionamento (estacionamentoId, usuario, senhaHash, nomeCompleto)
VALUES (1, 'operador1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'João Operador');


  -- =============================================
  -- 9. POPULAR COM DADOS DE EXEMPLO
  -- =============================================

  INSERT INTO [dbo].[Estacionamento]
  ([nome], [qtdVagasTotais], [qtdVagasDisponiveis], [avaliacaoMedia], [latitude], [longitude],
   [endereco], [precoHora], [horarioAbertura], [horarioFechamento], [ativo], [pixKey],
   [permiteReserva], [qtdVagasReservaveis])
  VALUES
  ('Estacionamento Center Park', 100, 100, 0.0, -23.55052000, -46.63330800,
   'Av. Paulista, 1000 - Bela Vista, São Paulo - SP', 15.00, '06:00:00', '22:00:00', 1, 'centerpark@pix.com',
   1, 10),

  ('Parking Paulista', 80, 80, 0.0, -23.56141400, -46.65617800,
   'Rua Augusta, 2000 - Consolação, São Paulo - SP', 12.00, '00:00:00', '23:59:59', 1, 'parkingpaulista@pix.com',
   1, 8),

  ('Vila Madalena Estacionamento', 60, 60, 0.0, -23.54538000, -46.69023000,
   'Rua Harmonia, 500 - Vila Madalena, São Paulo - SP', 10.00, '08:00:00', '20:00:00', 1, 'vilamadalena@pix.com',
   0, 0),

  ('Ibirapuera Park Auto', 150, 150, 0.0, -23.58741600, -46.65763400,
   'Av. Pedro Álvares Cabral - Vila Mariana, São Paulo - SP', 18.00, '06:00:00', '23:00:00', 1, 'ibirapuera@pix.com',
   0, 0),

  ('Jardins Premium', 50, 50, 0.0, -23.56194800, -46.67254100,
   'Rua Oscar Freire, 800 - Jardins, São Paulo - SP', 25.00, '07:00:00', '22:00:00', 1, 'jardinspremium@pix.com',
   1, 15),

  ('Estacionamento Faria Lima', 120, 120, 0.0, -23.57652000, -46.68893000,
   'Av. Brigadeiro Faria Lima, 2000 - Itaim Bibi, São Paulo - SP', 20.00, '06:00:00', '00:00:00', 1, 'farialima@pix.com',
   1, 20),

  ('Pinheiros Auto Park', 70, 70, 0.0, -23.56235000, -46.68412000,
   'Rua dos Pinheiros, 300 - Pinheiros, São Paulo - SP', 14.00, '08:00:00', '22:00:00', 1, 'pinheiros@pix.com',
   0, 0),

  ('República Center', 90, 90, 0.0, -23.54317000, -46.64282000,
   'Praça da República, 100 - República, São Paulo - SP', 8.00, '00:00:00', '23:59:59', 1, 'republica@pix.com',
   0, 0);
  GO
