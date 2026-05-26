-- =====================================================================
-- DEMO: Mapa de Calor Operacional (PREMIUM) - dados sinteticos v2
-- ---------------------------------------------------------------------
-- Objetivo:
--   1) Detectar AUTOMATICAMENTE o seu estacionamento (o mais recente
--      cadastrado com um AdmEstacionamento associado).
--   2) Garantir que ele esteja PREMIUM ativo.
--   3) Inserir 7 estacionamentos-peer ficticios NA MESMA CIDADE do seu,
--      espalhados em volta das suas coordenadas (offset ~0.5 a 4 km).
--   4) Popular SessaoEstacionamento para alvo + peers com entradas em
--      datas aleatorias nos ultimos 30 dias e intensidades calibradas
--      para gerar o gradiente arco-iris (azul ate vermelho).
--
-- Por que isso eh necessario:
--   O endpoint /api/operacao/{id}/mapa-calor filtra por LOWER(cidade) =
--   LOWER(do estacionamento logado). Se sua cidade nao tiver peers
--   ativos com lat/lng nao-zero E com sessoes no periodo, voce so ve
--   o seu proprio ponto. Este script resolve isso para o ambiente DEMO.
--
-- Idempotencia:
--   Todas as inserts demo sao marcadas com:
--     - Estacionamentos peer:   cnpj LIKE 'DEMO%'
--     - Sessoes demo:           qrCode LIKE 'DEMO-HEAT-%'
--   Re-rodar o script limpa tudo antes de regerar.
--
-- Como limpar manualmente:
--   DELETE FROM dbo.SessaoEstacionamento WHERE qrCode LIKE 'DEMO-HEAT-%';
--   DELETE FROM dbo.Estacionamento      WHERE cnpj   LIKE 'DEMO%';
--   (VagasEstacionamento eh removida em CASCADE)
-- =====================================================================

SET NOCOUNT ON;
USE [paradacerta];
GO

PRINT '';
PRINT '==================== DIAGNOSTICO INICIAL ====================';

SELECT
    e.id                                       AS estId,
    LEFT(e.nome, 35)                           AS nome,
    e.cidade,
    e.uf,
    e.bairro,
    e.latitude,
    e.longitude,
    e.ativo,
    e.plano,
    (SELECT COUNT(*) FROM dbo.SessaoEstacionamento s
     WHERE s.estacionamentoId = e.id
       AND s.status <> 'CANCELADA')            AS sessoesValidas
FROM dbo.Estacionamento e
ORDER BY e.cidade, e.id;
GO

-- =====================================================================
-- 1) Auto-deteccao do estacionamento alvo
-- =====================================================================
DECLARE @meuEstacionamentoId INT;

SELECT TOP 1 @meuEstacionamentoId = e.id
FROM dbo.Estacionamento e
INNER JOIN dbo.AdmEstacionamento a ON a.estacionamentoId = e.id
WHERE e.ativo = 1
ORDER BY e.id DESC;  -- o mais recente eh provavelmente o "seu"

IF @meuEstacionamentoId IS NULL
BEGIN
    RAISERROR('Nenhum estacionamento ativo com AdmEstacionamento foi encontrado. Cadastre-se primeiro por cadastro.html.', 16, 1);
    RETURN;
END

DECLARE @cidade  NVARCHAR(100);
DECLARE @uf      VARCHAR(2);
DECLARE @latBase DECIMAL(10,8);
DECLARE @lngBase DECIMAL(11,8);

SELECT
    @cidade  = cidade,
    @uf      = uf,
    @latBase = latitude,
    @lngBase = longitude
FROM dbo.Estacionamento WHERE id = @meuEstacionamentoId;

-- Fallback: se o alvo nao tem cidade/coords, usa Sao Paulo centro
IF @cidade IS NULL OR LTRIM(RTRIM(@cidade)) = ''
BEGIN
    SET @cidade = 'São Paulo';
    PRINT 'AVISO: estacionamento alvo sem cidade. Usando "Sao Paulo" como fallback.';
END
IF @uf IS NULL OR LTRIM(RTRIM(@uf)) = ''
BEGIN
    SET @uf = 'SP';
END
IF @latBase IS NULL OR @latBase = 0 OR @lngBase IS NULL OR @lngBase = 0
BEGIN
    SET @latBase = -23.5505000;
    SET @lngBase = -46.6333000;
    PRINT 'AVISO: lat/lng do alvo eh zero/null. Usando centro de Sao Paulo como ancora.';
END

PRINT CONCAT('Alvo detectado: ID=', @meuEstacionamentoId, ' | ', @cidade, '/', @uf,
             ' | lat=', @latBase, ' lng=', @lngBase);

-- =====================================================================
-- 2) Limpeza idempotente
-- =====================================================================
DELETE FROM dbo.SessaoEstacionamento WHERE qrCode LIKE 'DEMO-HEAT-%';
PRINT CONCAT('Sessoes-demo limpas: ', @@ROWCOUNT);

-- Limpa peer estacionamentos demo (cascade tira VagasEstacionamento)
DELETE FROM dbo.Estacionamento WHERE cnpj LIKE 'DEMO%';
PRINT CONCAT('Peer estacionamentos demo limpos: ', @@ROWCOUNT);

-- =====================================================================
-- 3) Marca alvo como PREMIUM ativo
-- =====================================================================
UPDATE dbo.Estacionamento
SET plano         = 'PREMIUM',
    planoInicio   = DATEADD(DAY, -10, SYSDATETIME()),
    planoFim      = DATEADD(DAY,  30, SYSDATETIME()),
    planoCobranca = 'MENSAL'
WHERE id = @meuEstacionamentoId;

PRINT CONCAT('Estacionamento alvo (ID=', @meuEstacionamentoId, ') marcado como PREMIUM ativo.');

-- =====================================================================
-- 4) Insere 7 peers DEMO na MESMA cidade, com offset radial em torno
--    do alvo. Cada peer tem uma "intensidade alvo" que determina
--    quantas sessoes serao geradas no proximo passo.
-- =====================================================================
DECLARE @bairros TABLE (
    idx              INT IDENTITY(1,1),
    bairro           NVARCHAR(100),
    dLat             DECIMAL(10,8),  -- offset latitude (~ +-0.04 grau ~ 4 km)
    dLng             DECIMAL(11,8),  -- offset longitude
    intensidadeAlvo  INT             -- entradas a gerar (max = quente)
);

INSERT INTO @bairros (bairro, dLat, dLng, intensidadeAlvo) VALUES
    ('Centro Comercial',         0.0050,  0.0120, 180),  -- PICO (vermelho)
    ('Zona Leste Demo',          0.0250, -0.0180, 130),  -- laranja
    ('Zona Norte Demo',         -0.0220,  0.0200, 110),  -- amarelo
    ('Vila Comercial Demo',      0.0150,  0.0300,  75),  -- verde-amarelo
    ('Distrito Industrial Demo',-0.0300, -0.0250,  55),  -- verde-ciano
    ('Periferia Sul Demo',       0.0400, -0.0400,  28),  -- azul-ciano
    ('Bairro Residencial Demo', -0.0180, -0.0350,  18);  -- azul (frio)

INSERT INTO dbo.Estacionamento (
    nome, cnpj, razaoSocial, nomeFantasia, avaliacaoMedia,
    latitude, longitude, endereco, precoHora,
    horarioAbertura, horarioFechamento, ativo, pixKey, permiteReserva,
    cep, logradouro, numero, complemento, bairro, cidade, uf,
    plano, planoInicio, planoFim, planoCobranca
)
SELECT
    CONCAT('Estacionamento Demo ', b.idx),
    CONCAT('DEMO',
           RIGHT('0000000000', 10 - LEN(CAST(b.idx AS VARCHAR(2)))),
           b.idx),                                                          -- 14 chars unicos
    CONCAT('Estacionamento Demo ', b.idx, ' LTDA'),
    CONCAT('Demo ', b.idx),
    0.0,
    @latBase + b.dLat,
    @lngBase + b.dLng,
    CONCAT('Endereco demo ', b.idx, ' - ', @cidade, ' - ', @uf),
    15.00, '06:00:00', '22:00:00', 1, NULL, 0,
    '00000000', 'Rua Demonstracao', CAST(b.idx AS VARCHAR(3)), NULL,
    b.bairro, @cidade, @uf,
    'BASIC', SYSDATETIME(), DATEADD(DAY, 30, SYSDATETIME()), 'TRIAL'
FROM @bairros b;

PRINT CONCAT('Peers demo inseridos: ', @@ROWCOUNT);

-- VagasEstacionamento eh UNIQUE(estacionamentoId); cria para os novos peers
INSERT INTO dbo.VagasEstacionamento
    (estacionamentoId, qtdVagasTotais, qtdVagasDisponiveis, qtdVagasReservaveis, qtdVagasReservadas)
SELECT id, 50, 50, 0, 0
FROM dbo.Estacionamento
WHERE cnpj LIKE 'DEMO%';

-- =====================================================================
-- 5) Monta tabela final de geracao de sessoes:
--    - alvo (entradas medias)
--    - peers REAIS na mesma cidade (entradas aleatorias 30..130)
--    - peers DEMO (intensidades calibradas)
-- =====================================================================
DECLARE @geracao TABLE (estacionamentoId INT, qtdEntradas INT);

-- Alvo: 95 entradas (fica em tom medio-alto pra contraste)
INSERT INTO @geracao VALUES (@meuEstacionamentoId, 95);

-- Peers REAIS (nao-demo) ativos na mesma cidade, com lat/lng nao-zero
INSERT INTO @geracao
SELECT e.id, 30 + (ABS(CHECKSUM(NEWID())) % 100)
FROM dbo.Estacionamento e
WHERE e.ativo = 1
  AND LOWER(e.cidade) = LOWER(@cidade)
  AND e.id <> @meuEstacionamentoId
  AND e.cnpj NOT LIKE 'DEMO%'
  AND (e.latitude <> 0 OR e.longitude <> 0);

-- Peers DEMO recem-criados (com intensidades calibradas para arco-iris)
INSERT INTO @geracao
SELECT e.id, b.intensidadeAlvo
FROM dbo.Estacionamento e
JOIN @bairros b ON e.bairro = b.bairro
WHERE e.cnpj LIKE 'DEMO%';

-- =====================================================================
-- 6) Gera as sessoes ENCERRADAS, espalhadas nos ultimos 30 dias
-- =====================================================================
;WITH numeros AS (
    SELECT TOP (1000)
        ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects a
    CROSS JOIN sys.all_objects b
)
INSERT INTO dbo.SessaoEstacionamento
    (clienteId, estacionamentoId, horaEntrada, horaSaida, horaPagamento,
     valorPago, status, qrCode, reservado, placa)
SELECT
    NULL,
    g.estacionamentoId,
    DATEADD(MINUTE,
            -1 * (ABS(CHECKSUM(NEWID())) % (30 * 24 * 60)),
            SYSDATETIME()),
    NULL, NULL, NULL,
    'ENCERRADA',
    CONCAT('DEMO-HEAT-', g.estacionamentoId, '-', n.n),
    0,
    NULL
FROM @geracao g
JOIN numeros n ON n.n <= g.qtdEntradas;

PRINT CONCAT('Sessoes-demo geradas: ', @@ROWCOUNT);

-- =====================================================================
-- 7) Resumo final - confere por estacionamento na cidade alvo
-- =====================================================================
PRINT '';
PRINT '==================== RESUMO FINAL ====================';

SELECT
    e.id                                       AS estId,
    LEFT(e.nome, 35)                           AS nome,
    e.bairro,
    e.cidade,
    e.latitude,
    e.longitude,
    e.plano,
    e.ativo,
    (SELECT COUNT(*) FROM dbo.SessaoEstacionamento s
     WHERE s.estacionamentoId = e.id
       AND s.qrCode LIKE 'DEMO-HEAT-%')        AS sessoesDemoGeradas
FROM dbo.Estacionamento e
WHERE LOWER(e.cidade) = LOWER(@cidade)
ORDER BY sessoesDemoGeradas DESC;

PRINT '';
PRINT '======================================================';
PRINT 'OK! Agora faca login com o admin do estacionamento alvo,';
PRINT 'acesse "Painel de Operacao" (reserva.html) e role ate o';
PRINT 'Mapa de calor operacional. O gradiente arco-iris deve';
PRINT 'aparecer dentro do contorno da sua cidade (IBGE).';
PRINT 'Periodos sugeridos: "Mes" ou "Todos".';
PRINT '======================================================';
GO
