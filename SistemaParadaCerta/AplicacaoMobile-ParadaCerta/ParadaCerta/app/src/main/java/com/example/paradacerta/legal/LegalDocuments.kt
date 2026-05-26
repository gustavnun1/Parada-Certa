package com.example.paradacerta.legal

object LegalDocuments {
    const val TERMOS_DE_USO = """
TERMOS DE USO - PARADA CERTA

Versao 1.0 - Ultima atualizacao: 21/05/2026

O Parada Certa e uma plataforma integrada para consulta, reserva, gestao e operacao de estacionamentos privados. Estes Termos sao unicos para aplicativo mobile, plataforma web, kiosk/totem, APIs e demais recursos do projeto.

Ao criar conta, acessar, reservar, administrar estacionamento, contratar plano, operar o kiosk/totem ou usar qualquer funcionalidade, o usuario aceita estes Termos e a Politica de Privacidade.

Perfis de usuario: motorista, administrador de estacionamento, operador, usuario do kiosk/totem e plataforma Parada Certa.

Cadastro e conta: o usuario deve fornecer dados verdadeiros, manter senha sob sigilo, nao compartilhar acesso, manter dados atualizados e usar CPF/CNPJ validos. Contas podem ser suspensas por fraude, uso indevido ou dados falsos.

Estacionamentos: o administrador deve manter CNPJ, razao social, nome fantasia, endereco, precos, horarios e vagas corretos. O estacionamento responde pelas informacoes exibidas.

Veiculos: o motorista deve cadastrar veiculos reais, com placa, marca, modelo e cor corretos. Dados incorretos podem impedir reserva, entrada ou pagamento.

Reservas: dependem de disponibilidade. O motorista deve comparecer no horario, e o estacionamento deve respeitar reservas confirmadas, salvo situacoes justificadas.

Cancelamento de reserva: se o motorista cancelar, sera devolvido somente 15% do valor ao usuario, e os 85% restantes serao destinados ao estacionamento. O usuario deve ser informado antes da confirmacao. Se o administrador ou estacionamento cancelar, o motorista sera notificado, inclusive por e-mail quando disponivel.

Entrada, saida e QR Code: a entrada pode ser registrada por QR Code, kiosk/totem ou sistema. A saida encerra a permanencia. Fraudes ou tentativas de burlar QR Code podem gerar bloqueio.

Pagamento de estadia: a cobranca minima e de 1 hora. Apos 1 hora, o tempo e arredondado para cima em blocos de 30 minutos. Exemplos: 1h10 cobra 1h30; 3h40 cobra 4h. O valor considera o preco por hora definido pelo estacionamento.

Planos e pagamentos: planos Basic, Standard e Premium podem limitar recursos. Pagamentos por cartao ou Pix podem existir ou ser simulados no contexto academico. O Parada Certa nao deve armazenar CVV nem numero completo do cartao.

Avaliacoes: comentarios ofensivos, falsos, discriminatorios ou abusivos podem ser removidos. O usuario e responsavel pelo que publica.

Dados operacionais: o estacionamento concorda que dados nao sensiveis, publicos, operacionais, agregados ou anonimizados poderao ser usados ou compartilhados com outros estacionamentos para relatorios, estatisticas, comparacao regional, analise de demanda, inteligencia de mercado e melhoria da plataforma. Isso nao inclui dados pessoais sensiveis, login, senhas, CPF de administradores, dados pessoais de motoristas ou operadores, documentos privados, dados financeiros completos ou informacoes sigilosas.

Uso proibido: usar dados falsos, acessar conta de terceiros, burlar reservas, manipular precos ou vagas, fraudar pagamentos, explorar falhas tecnicas, copiar ou explorar indevidamente a plataforma e inserir conteudo ofensivo.

Responsabilidades: motoristas devem informar dados corretos, selecionar veiculo correto, cumprir horarios, pagar valores devidos e respeitar regras do estacionamento. Administradores devem manter informacoes corretas, honrar reservas, informar cancelamentos, usar dados dos motoristas apenas para operacao e manter operadores autorizados.

A plataforma disponibiliza o sistema, emprega esforcos razoaveis de seguranca e disponibilidade, corrige falhas quando identificadas e processa dados conforme a Politica de Privacidade. O Parada Certa nao e proprietario dos estacionamentos e nao garante disponibilidade ininterrupta.

Estes Termos sao regidos pela legislacao brasileira e podem ser atualizados. A versao atual fica disponivel nos canais oficiais do projeto.

Este documento foi elaborado para fins academicos no contexto do Trabalho de Conclusao de Curso do projeto Parada Certa. A plataforma descrita representa uma operacao academica/simulada, sem finalidade comercial real neste momento.
"""

    const val POLITICA_PRIVACIDADE = """
POLITICA DE PRIVACIDADE - PARADA CERTA

Versao 1.0 - Ultima atualizacao: 21/05/2026

O Parada Certa e uma plataforma integrada para consulta, reserva, gestao e operacao de estacionamentos privados, com uso por aplicativo mobile, plataforma web, kiosk/totem, backend/API e banco de dados. Esta Politica e unica para todo o ecossistema.

Usuarios abrangidos: motoristas, administradores de estacionamentos, operadores, usuarios do kiosk/totem, usuarios do sistema web e visitantes de paginas publicas.

Dados de motoristas: nome, CPF, data de nascimento, e-mail, telefone, senha protegida, dados de login, dados de veiculo, placa, marca, modelo, cor, historico de reservas, entradas e saidas, pagamentos, valores, avaliacoes, comentarios e preferencias.

Dados de administradores e estacionamentos: nome do responsavel, CPF, data de nascimento, e-mail, telefone, usuario, senha protegida, CNPJ, razao social, nome fantasia, endereco, coordenadas, chave Pix, planos, vagas, precos, horarios, fotos, relatorios e indicadores.

Dados de operadores: nome, documento quando aplicavel, e-mail, telefone, dados de acesso, estacionamento vinculado e permissoes de operacao.

Dados de kiosk/totem: reserva ou entrada, veiculo, placa, horarios de entrada e saida, identificador da estadia, QR Code e dados necessarios para pagamento ou operacao.

Pagamentos: plano contratado, valor, status, data, ultimos 4 digitos do cartao e bandeira quando identificada. O Parada Certa nao deve armazenar numero completo do cartao nem CVV.

Dados sensiveis: o sistema nao tem como objetivo coletar origem racial, religiao, opiniao politica, saude, biometria ou orientacao sexual. Dados sensiveis enviados indevidamente em campos livres poderao ser tratados apenas para operacao, seguranca, moderacao ou removidos.

Finalidades: criar e manter contas, autenticar usuarios, permitir reservas, gerenciar vagas, registrar entradas e saidas, calcular pagamentos, processar planos, enviar notificacoes e e-mails de cancelamento, exibir relatorios, melhorar seguranca, prevenir fraude, cumprir obrigacoes legais, melhorar experiencia, permitir avaliacoes, prestar suporte e gerar dados estatisticos e operacionais.

Bases legais: execucao de contrato, cumprimento de obrigacao legal ou regulatoria, legitimo interesse, consentimento quando aplicavel, exercicio regular de direitos e prevencao a fraude e seguranca.

Compartilhamento: dados podem ser compartilhados entre motorista e estacionamento quando necessario para reserva, entrada, saida, pagamento ou atendimento; com operadores autorizados; servicos de e-mail; provedores de pagamento futuros; hospedagem; mapas/geolocalizacao; e autoridades publicas quando exigido por lei. O Parada Certa nao vende dados pessoais.

Dados nao sensiveis entre estacionamentos: a plataforma podera divulgar apenas dados nao sensiveis, publicos, operacionais, agregados ou anonimizados, como nome fantasia publico, regiao, bairro ou cidade, faixa de preco, quantidade aproximada de vagas, horarios publicos, indicadores agregados de ocupacao e demanda e dados estatisticos. Isso nao inclui CPF de administradores, dados pessoais de motoristas ou operadores, login, senhas, dados financeiros completos, numero completo de cartao, CVV, documentos privados, informacoes sigilosas ou dados sensiveis.

Geolocalizacao e mapas: localizacao, endereco e coordenadas podem ser usados para buscar estacionamentos proximos, calcular distancia, exibir mapa, validar endereco e apoiar rotas.

Cookies e armazenamento local: sessao, login, preferencias, tokens, cache, AsyncStorage/localStorage e dados temporarios podem ser usados para funcionamento e seguranca.

Seguranca: a plataforma adota medidas razoaveis, incluindo senhas com BCrypt para administradores e operadores, controle de acesso, mascaramento de CPF em listagens, nao armazenamento de CVV ou cartao completo, validacoes no backend, minimizacao de dados e principios da LGPD.

Retencao e exclusao: dados sao mantidos enquanto a conta estiver ativa e pelo periodo necessario a obrigacoes legais, auditoria, seguranca ou historico de transacoes. O usuario pode solicitar acesso, correcao, exclusao ou anonimizacao quando aplicavel.

Direitos LGPD: confirmacao de tratamento, acesso, correcao, anonimizacao, bloqueio, eliminacao, portabilidade quando aplicavel, informacao sobre compartilhamento, revogacao de consentimento e revisao de decisoes automatizadas quando aplicavel.

Criancas e adolescentes: a plataforma e destinada a usuarios com capacidade legal para contratar ou reservar servicos de estacionamento. O uso por menores deve ocorrer com autorizacao e responsabilidade dos responsaveis legais.

Contato: [inserir e-mail de contato do projeto], [inserir canal de suporte] e [inserir responsavel pelo tratamento de dados, se aplicavel].

Este documento foi elaborado para fins academicos no contexto do Trabalho de Conclusao de Curso do projeto Parada Certa. A plataforma descrita representa uma operacao academica/simulada, sem finalidade comercial real neste momento.
"""
}
