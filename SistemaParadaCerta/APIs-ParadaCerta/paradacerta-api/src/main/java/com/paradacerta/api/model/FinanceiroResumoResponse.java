package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Cards do dashboard financeiro do administrador. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceiroResumoResponse {
    private BigDecimal totalRecebido;       // soma dos valorPago no período
    private BigDecimal totalTaxas;          // taxa da plataforma aplicada
    private BigDecimal totalLiquido;        // totalRecebido - totalTaxas
    private long pagamentosHoje;            // quantidade de sessões pagas hoje
    private BigDecimal ticketMedio;         // totalRecebido / pagamentos no período
    private BigDecimal maiorPagamento;      // maior valorPago do período
    private long pendentes;                 // sessões ATIVAS (ainda devem)
    private BigDecimal taxaPlataformaPercentual; // ex: 0.05
    private String periodo;                 // "hoje" / "semana" / "mes" / "todos"
}
