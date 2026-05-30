-- =============================================================
-- ParadaCerta - Migracao incremental: confirmacao de reserva
-- por QR Code + controle de sessao ativa.
--
-- Aplica:
--   1) Novas colunas em SessaoEstacionamento:
--        - dataHoraConfirmacao  (quando o QR foi escaneado)
--        - valorFinalCalculado  (total recalculado no finalizar)
--        - valorRestanteCobrado (diferenca paga no finalizar)
--   2) Expansao do CHECK de status para incluir:
--        - AGUARDANDO_CONFIRMACAO
--        - EM_USO
--
-- Script idempotente: pode ser executado mais de uma vez.
-- =============================================================

USE [ParadaCerta];
GO

-- -------------------------------------------------------------
-- 1) Colunas novas
-- -------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[dbo].[SessaoEstacionamento]')
      AND name = N'dataHoraConfirmacao'
)
BEGIN
    ALTER TABLE [dbo].[SessaoEstacionamento]
        ADD [dataHoraConfirmacao] [datetime2] NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[dbo].[SessaoEstacionamento]')
      AND name = N'valorFinalCalculado'
)
BEGIN
    ALTER TABLE [dbo].[SessaoEstacionamento]
        ADD [valorFinalCalculado] [decimal](10,2) NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[dbo].[SessaoEstacionamento]')
      AND name = N'valorRestanteCobrado'
)
BEGIN
    ALTER TABLE [dbo].[SessaoEstacionamento]
        ADD [valorRestanteCobrado] [decimal](10,2) NULL;
END
GO

-- -------------------------------------------------------------
-- 2) Expandir CHECK de status para os novos valores
--    (drop do CK antigo + recria com lista expandida)
-- -------------------------------------------------------------
IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'[dbo].[SessaoEstacionamento]')
      AND name = N'CK_Sessao_Status'
)
BEGIN
    ALTER TABLE [dbo].[SessaoEstacionamento]
        DROP CONSTRAINT CK_Sessao_Status;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'[dbo].[SessaoEstacionamento]')
      AND name = N'CK_Sessao_Status'
)
BEGIN
    ALTER TABLE [dbo].[SessaoEstacionamento]
        ADD CONSTRAINT CK_Sessao_Status CHECK (
            [status] IN (
                'ATIVA',
                'ENCERRADA',
                'CANCELADA',
                'AGUARDANDO_CONFIRMACAO',
                'EM_USO'
            )
        );
END
GO

-- -------------------------------------------------------------
-- 3) Aumentar o length da coluna status (era 10, AGUARDANDO_CONFIRMACAO tem 22)
-- -------------------------------------------------------------
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[dbo].[SessaoEstacionamento]')
      AND name = N'status'
      AND max_length < 30
)
BEGIN
    -- Para alterar tipo de coluna que tem CHECK constraint dependente,
    -- removemos a constraint primeiro, alteramos o tipo e recriamos.
    IF EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'[dbo].[SessaoEstacionamento]')
          AND name = N'CK_Sessao_Status'
    )
    BEGIN
        ALTER TABLE [dbo].[SessaoEstacionamento]
            DROP CONSTRAINT CK_Sessao_Status;
    END

    ALTER TABLE [dbo].[SessaoEstacionamento]
        ALTER COLUMN [status] [nvarchar](30) NOT NULL;

    ALTER TABLE [dbo].[SessaoEstacionamento]
        ADD CONSTRAINT CK_Sessao_Status CHECK (
            [status] IN (
                'ATIVA',
                'ENCERRADA',
                'CANCELADA',
                'AGUARDANDO_CONFIRMACAO',
                'EM_USO'
            )
        );
END
GO
