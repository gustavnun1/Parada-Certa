IF COL_LENGTH('Estacionamento', 'cep') IS NULL
    ALTER TABLE Estacionamento ADD cep VARCHAR(8) NULL;

IF COL_LENGTH('Estacionamento', 'logradouro') IS NULL
    ALTER TABLE Estacionamento ADD logradouro VARCHAR(200) NULL;

IF COL_LENGTH('Estacionamento', 'numero') IS NULL
    ALTER TABLE Estacionamento ADD numero VARCHAR(10) NULL;

IF COL_LENGTH('Estacionamento', 'complemento') IS NULL
    ALTER TABLE Estacionamento ADD complemento VARCHAR(100) NULL;

IF COL_LENGTH('Estacionamento', 'bairro') IS NULL
    ALTER TABLE Estacionamento ADD bairro VARCHAR(100) NULL;

IF COL_LENGTH('Estacionamento', 'cidade') IS NULL
    ALTER TABLE Estacionamento ADD cidade VARCHAR(100) NULL;

IF COL_LENGTH('Estacionamento', 'uf') IS NULL
    ALTER TABLE Estacionamento ADD uf CHAR(2) NULL;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_Estacionamento_Cep'
      AND object_id = OBJECT_ID('Estacionamento')
)
    CREATE INDEX IX_Estacionamento_Cep
    ON Estacionamento (cep)
    WHERE cep IS NOT NULL;
