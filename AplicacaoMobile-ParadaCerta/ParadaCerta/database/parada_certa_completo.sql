-- =============================================
-- SCRIPT COMPLETO - BANCO PARADA CERTA
-- Execução única, sem ALTER TABLE
-- =============================================

USE [ParadaCerta]
GO

-- =============================================
-- 1. REMOVER OBJETOS EXISTENTES
-- =============================================

-- Procedures
IF OBJECT_ID('dbo.sp_RegistrarPagamento', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_RegistrarPagamento;
IF OBJECT_ID('dbo.sp_RegistrarEntrada',  'P') IS NOT NULL DROP PROCEDURE dbo.sp_RegistrarEntrada;
GO

-- Triggers
IF OBJECT_ID('TR_Sessao_AtualizaVagas',    'TR') IS NOT NULL DROP TRIGGER TR_Sessao_AtualizaVagas;
IF OBJECT_ID('TR_Vaga_AtualizaDisponiveis','TR') IS NOT NULL DROP TRIGGER TR_Vaga_AtualizaDisponiveis;
IF OBJECT_ID('TR_Avaliacao_AtualizaMedia', 'TR') IS NOT NULL DROP TRIGGER TR_Avaliacao_AtualizaMedia;
GO

-- Tabelas (ordem de dependência de FK)
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
    [nome]           [nvarchar](200) NOT NULL,
    [cpf]            [varchar](11)   NOT NULL,
    [email]          [nvarchar](200) NOT NULL,
    [senha]          [nvarchar](255) NOT NULL,
    [dataNascimento] [date]          NOT NULL,
    [numeroCelular]  [varchar](15)   NULL,
    [placa]          [varchar](7)    NOT NULL,
    [veiculo]        [varchar](7)    NULL,
    [premium]        [bit]           NOT NULL DEFAULT 0,   -- 0 = gratuito, 1 = premium

    CONSTRAINT PK_Cliente        PRIMARY KEY CLUSTERED ([cpf] ASC),
    CONSTRAINT UQ_Cliente_Email  UNIQUE ([email]),
    CONSTRAINT CK_Cliente_CPF   CHECK (LEN([cpf]) = 11),
    CONSTRAINT CK_Cliente_Email CHECK ([email] LIKE '%@%'),
    CONSTRAINT CK_Cliente_Placa CHECK (LEN([placa]) > 0)
);
GO

CREATE TABLE [dbo].[Veiculo] (
    [nome]        [nvarchar](100) NOT NULL,
    [placa]       [varchar](7)    NOT NULL,
    [cor]         [nvarchar](50)  NOT NULL,
    [responsavel] [varchar](11)   NOT NULL,

    CONSTRAINT PK_Veiculo          PRIMARY KEY CLUSTERED ([placa] ASC),
    CONSTRAINT FK_Veiculo_Cliente  FOREIGN KEY ([responsavel])
        REFERENCES [dbo].[Cliente] ([cpf]) ON DELETE CASCADE,
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
    [cpfCliente]  [varchar](11)   NOT NULL,

    CONSTRAINT PK_Endereco         PRIMARY KEY CLUSTERED ([id] ASC),
    CONSTRAINT FK_Endereco_Cliente FOREIGN KEY ([cpfCliente])
        REFERENCES [dbo].[Cliente] ([cpf]) ON DELETE CASCADE,
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

    CONSTRAINT PK_Estacionamento             PRIMARY KEY CLUSTERED ([id] ASC),
    CONSTRAINT CK_Estacionamento_Latitude   CHECK ([latitude]  >= -90  AND [latitude]  <= 90),
    CONSTRAINT CK_Estacionamento_Longitude  CHECK ([longitude] >= -180 AND [longitude] <= 180),
    CONSTRAINT CK_Estacionamento_Avaliacao  CHECK ([avaliacaoMedia] >= 0 AND [avaliacaoMedia] <= 5),
    CONSTRAINT CK_Estacionamento_PrecoHora  CHECK ([precoHora] >= 0),
    CONSTRAINT CK_Estacionamento_Vagas      CHECK ([qtdVagasDisponiveis] <= [qtdVagasTotais])
);
GO

-- qrCode e horaPagamento já incluídos no CREATE (sem ALTER)
CREATE TABLE [dbo].[SessaoEstacionamento] (
    [id]               [bigint]       IDENTITY(1,1) NOT NULL,
    [cpfUsuario]       [varchar](11)  NOT NULL,
    [estacionamentoId] [int]          NOT NULL,
    [horaEntrada]      [datetime2]    NOT NULL,
    [horaSaida]        [datetime2]    NULL,
    [horaPagamento]    [datetime2]    NULL,
    [valorPago]        [decimal](10,2) NULL,
    [status]           [nvarchar](10) NOT NULL,
    [qrCode]           [varchar](64)  NULL,

    CONSTRAINT PK_SessaoEstacionamento   PRIMARY KEY CLUSTERED ([id] ASC),
    CONSTRAINT FK_Sessao_Cliente         FOREIGN KEY ([cpfUsuario])
        REFERENCES [dbo].[Cliente] ([cpf]),
    CONSTRAINT FK_Sessao_Estacionamento  FOREIGN KEY ([estacionamentoId])
        REFERENCES [dbo].[Estacionamento] ([id]),
    CONSTRAINT CK_Sessao_Status          CHECK ([status] IN ('ATIVA', 'ENCERRADA', 'CANCELADA')),
    CONSTRAINT CK_Sessao_ValorPago       CHECK ([valorPago] IS NULL OR [valorPago] >= 0)
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
    [id]               [int]           IDENTITY(1,1) NOT NULL,
    [estacionamentoId] [int]           NOT NULL,
    [clienteCPF]       [varchar](11)   NOT NULL,
    [nota]             [int]           NOT NULL,
    [comentario]       [nvarchar](500) NULL,
    [dataAvaliacao]    [datetime]      NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_Avaliacao                PRIMARY KEY CLUSTERED ([id] ASC),
    CONSTRAINT FK_Avaliacao_Estacionamento FOREIGN KEY ([estacionamentoId])
        REFERENCES [dbo].[Estacionamento] ([id]) ON DELETE CASCADE,
    CONSTRAINT FK_Avaliacao_Cliente        FOREIGN KEY ([clienteCPF])
        REFERENCES [dbo].[Cliente] ([cpf]) ON DELETE CASCADE,
    CONSTRAINT CK_Avaliacao_Nota          CHECK ([nota] >= 1 AND [nota] <= 5)
);
GO

CREATE TABLE [dbo].[FormaPagamento] (
    [id]            [int]           IDENTITY(1,1) NOT NULL,
    [clienteCPF]    [varchar](11)   NOT NULL,
    [tipoPagamento] [nvarchar](50)  NOT NULL,
    [numeroCartao]  [varchar](16)   NULL,
    [nomeCartao]    [nvarchar](100) NULL,
    [validade]      [varchar](7)    NULL,
    [bandeira]      [nvarchar](50)  NULL,

    CONSTRAINT PK_FormaPagamento         PRIMARY KEY CLUSTERED ([id] ASC),
    CONSTRAINT FK_FormaPagamento_Cliente FOREIGN KEY ([clienteCPF])
        REFERENCES [dbo].[Cliente] ([cpf]) ON DELETE CASCADE
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

CREATE NONCLUSTERED INDEX IX_Endereco_Cliente
    ON [dbo].[Endereco] ([cpfCliente]);
GO

CREATE NONCLUSTERED INDEX IX_Estacionamento_Localizacao
    ON [dbo].[Estacionamento] ([latitude], [longitude]);
GO

CREATE NONCLUSTERED INDEX IX_Sessao_CpfStatus
    ON [dbo].[SessaoEstacionamento] ([cpfUsuario], [status]);
GO

-- Garante idempotência: o mesmo QR Code não pode ser processado duas vezes
CREATE UNIQUE NONCLUSTERED INDEX UQ_Sessao_QrCode
    ON [dbo].[SessaoEstacionamento] ([qrCode])
    WHERE [qrCode] IS NOT NULL;
GO

-- =============================================
-- 5. TRIGGERS
-- =============================================

-- Recalcula avaliacaoMedia do estacionamento após insert/update/delete em Avaliacao
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

-- Recalcula qtdVagasDisponiveis com base nas sessões ATIVAS
-- (fonte da verdade: SessaoEstacionamento, não Vaga.ocupada)
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

-- Registra a entrada de um veículo via QR Code
CREATE OR ALTER PROCEDURE sp_RegistrarEntrada
    @qrCode           VARCHAR(64),
    @cpfUsuario       VARCHAR(11),
    @estacionamentoId INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRANSACTION;

        -- Idempotência: QR já processado → ignora silenciosamente
        IF EXISTS (
            SELECT 1 FROM SessaoEstacionamento WITH (UPDLOCK, HOLDLOCK)
            WHERE qrCode = @qrCode
        )
        BEGIN
            COMMIT;
            RETURN;
        END

        -- Verifica disponibilidade com lock para evitar race condition
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
            (qrCode, cpfUsuario, estacionamentoId, horaEntrada, status)
        VALUES
            (@qrCode, @cpfUsuario, @estacionamentoId, SYSDATETIME(), 'ATIVA');
        -- TR_Sessao_AtualizaVagas recalcula qtdVagasDisponiveis automaticamente

        COMMIT;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH
END;
GO

-- Registra o pagamento e encerra a sessão liberando a vaga
CREATE OR ALTER PROCEDURE sp_RegistrarPagamento
    @qrCode    VARCHAR(64),
    @valorPago DECIMAL(10,2)
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRANSACTION;

        UPDATE SessaoEstacionamento
        SET status         = 'ENCERRADA',
            horaPagamento  = SYSDATETIME(),
            horaSaida      = SYSDATETIME(),
            valorPago      = @valorPago
        WHERE qrCode = @qrCode AND status = 'ATIVA';

        IF @@ROWCOUNT = 0
        BEGIN
            ROLLBACK;
            THROW 50002, 'QR Code inválido ou sessão já encerrada', 1;
        END

        COMMIT;
        -- TR_Sessao_AtualizaVagas libera a vaga automaticamente
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH
END;
GO

-- =============================================
-- 7. RECONCILIAÇÃO INICIAL DE VAGAS
-- Garante consistência caso existam dados legados
-- =============================================

UPDATE e
SET e.qtdVagasDisponiveis = e.qtdVagasTotais - ISNULL((
    SELECT COUNT(*) FROM SessaoEstacionamento s
    WHERE s.estacionamentoId = e.id AND s.status = 'ATIVA'
), 0)
FROM Estacionamento e;
GO
