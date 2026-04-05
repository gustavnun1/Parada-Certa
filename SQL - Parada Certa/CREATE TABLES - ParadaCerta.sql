USE [ParadaCerta]
GO

/****** Object:  Table [dbo].[Cliente]    Script Date: 05/04/2026 17:06:12 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[Cliente](
	[nome] [nvarchar](200) NOT NULL,
	[cpf] [varchar](11) NOT NULL,
	[email] [nvarchar](200) NOT NULL,
	[senha] [nvarchar](255) NOT NULL,
	[dataNascimento] [date] NOT NULL,
	[numeroCelular] [varchar](15) NULL,
	[placa] [varchar](7) NOT NULL,
	[veiculo] [varchar](7) NULL,
PRIMARY KEY CLUSTERED 
(
	[cpf] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[Cliente]  WITH CHECK ADD  CONSTRAINT [CK_Cliente_CPF] CHECK  ((len([cpf])=(11)))
GO

ALTER TABLE [dbo].[Cliente] CHECK CONSTRAINT [CK_Cliente_CPF]
GO

ALTER TABLE [dbo].[Cliente]  WITH CHECK ADD  CONSTRAINT [CK_Cliente_Email] CHECK  (([email] like '%@%'))
GO

ALTER TABLE [dbo].[Cliente] CHECK CONSTRAINT [CK_Cliente_Email]
GO

ALTER TABLE [dbo].[Cliente]  WITH CHECK ADD  CONSTRAINT [CK_Cliente_Placa] CHECK  ((len([placa])>(0)))
GO

ALTER TABLE [dbo].[Cliente] CHECK CONSTRAINT [CK_Cliente_Placa]
GO


USE [ParadaCerta]
GO

/****** Object:  Table [dbo].[Endereco]    Script Date: 05/04/2026 17:06:43 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[Endereco](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[cep] [varchar](8) NOT NULL,
	[logradouro] [nvarchar](200) NOT NULL,
	[numero] [nvarchar](10) NOT NULL,
	[complemento] [nvarchar](100) NULL,
	[bairro] [nvarchar](100) NOT NULL,
	[cidade] [nvarchar](100) NOT NULL,
	[estado] [varchar](2) NOT NULL,
	[cpfCliente] [varchar](11) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[Endereco]  WITH CHECK ADD  CONSTRAINT [FK_Endereco_Cliente] FOREIGN KEY([cpfCliente])
REFERENCES [dbo].[Cliente] ([cpf])
ON DELETE CASCADE
GO

ALTER TABLE [dbo].[Endereco] CHECK CONSTRAINT [FK_Endereco_Cliente]
GO

ALTER TABLE [dbo].[Endereco]  WITH CHECK ADD  CONSTRAINT [CK_Endereco_CEP] CHECK  ((len([cep])=(8)))
GO

ALTER TABLE [dbo].[Endereco] CHECK CONSTRAINT [CK_Endereco_CEP]
GO

ALTER TABLE [dbo].[Endereco]  WITH CHECK ADD  CONSTRAINT [CK_Endereco_Estado] CHECK  ((len([estado])=(2)))
GO

ALTER TABLE [dbo].[Endereco] CHECK CONSTRAINT [CK_Endereco_Estado]
GO


USE [ParadaCerta]
GO

/****** Object:  Table [dbo].[Veiculo]    Script Date: 05/04/2026 17:07:03 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[Veiculo](
	[nome] [nvarchar](100) NOT NULL,
	[placa] [varchar](7) NOT NULL,
	[cor] [nvarchar](50) NOT NULL,
	[responsavel] [varchar](11) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[placa] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[Veiculo]  WITH CHECK ADD  CONSTRAINT [FK_Veiculo_Cliente] FOREIGN KEY([responsavel])
REFERENCES [dbo].[Cliente] ([cpf])
ON DELETE CASCADE
GO

ALTER TABLE [dbo].[Veiculo] CHECK CONSTRAINT [FK_Veiculo_Cliente]
GO

ALTER TABLE [dbo].[Veiculo]  WITH CHECK ADD  CONSTRAINT [CK_Veiculo_Placa] CHECK  ((len([placa])>(0)))
GO

ALTER TABLE [dbo].[Veiculo] CHECK CONSTRAINT [CK_Veiculo_Placa]
GO



CREATE TABLE Gerente (
    IdGerente INT IDENTITY(1,1) PRIMARY KEY,
    Nome VARCHAR(100) NOT NULL,
    CPF VARCHAR(11) NOT NULL,
    Email VARCHAR(100) NOT NULL,
    Senha VARCHAR(100) NOT NULL,
    Telefone VARCHAR(20) NOT NULL,
    EnderecoId INT NOT NULL
);


CREATE TABLE Estacionamento (
    IdEstacionamento INT IDENTITY(1,1) PRIMARY KEY,
    Nome VARCHAR(100) NOT NULL,
    CNPJ VARCHAR(14) NOT NULL,
    EnderecoId INT NOT NULL,
    Latitude DECIMAL(10,8) NOT NULL,
    Longitude DECIMAL(11,8) NOT NULL,
    HorarioAbertura TIME NOT NULL,
    HorarioFechamento TIME NOT NULL,
    QuantidadeVagas INT NOT NULL,
    ValorHora DECIMAL(10,2) NOT NULL,
    GerenteId INT NOT NULL,

    FOREIGN KEY (GerenteId) REFERENCES Gerente(IdGerente)
);

CREATE TABLE Vaga (
    IdVaga INT IDENTITY(1,1) PRIMARY KEY,
    EstacionamentoId INT NOT NULL,
    Codigo VARCHAR(50) NOT NULL,
    Ocupada BIT NOT NULL,

    FOREIGN KEY (EstacionamentoId) REFERENCES Estacionamento(IdEstacionamento)
);

CREATE TABLE Avaliacao (
    IdAvaliacao INT IDENTITY(1,1) PRIMARY KEY,
    ClienteCPF VARCHAR(11) NOT NULL,
    Nota INT NOT NULL,
    Comentario VARCHAR(255),
    DataAvaliacao DATETIME NOT NULL,

    FOREIGN KEY (ClienteCPF) REFERENCES Cliente(cpf)
);

CREATE TABLE FormaPagamento (
    IdFormaPagamento INT IDENTITY(1,1) PRIMARY KEY,
    ClienteCPF VARCHAR(11) NOT NULL,
    TipoPagamento VARCHAR(50) NOT NULL,

    FOREIGN KEY (ClienteCPF) REFERENCES Cliente(cpf)
);