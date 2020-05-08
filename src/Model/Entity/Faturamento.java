package Model.Entity;

import Auxiliar.Valor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Faturamento {

    private final int mes;
    private final int ano;
    private final int anoMes;
    private BigDecimal faturamentoTotal = new BigDecimal(0);
    private List<Valor> valores;
    
    private static final BigDecimal arred =  new BigDecimal("10000");

    public Faturamento(int mes, int ano, List<Valor> valores) {
        this.mes = mes;
        this.ano = ano;
        this.valores = valores;
        this.anoMes = Integer.valueOf(ano + (mes < 10 ? "0" : "") + mes);

        setFaturamentoTotal();
    }

    public int getAnoMes() {
        return anoMes;
    }

    public static List<BigDecimal> getSumFaturamentosMensais(List<Faturamento> faturamentos, int periodoInicio, int periodoFim) {
        return getSumFaturamentosMensais(new Integer[]{0, 1, 2, 3, 4, 5}, faturamentos, periodoInicio, periodoFim);
    }

    public static List<BigDecimal> getSumFaturamentosMensais(Integer[] anexos, List<Faturamento> faturamentos, int periodoInicio, int periodoFim) {
        List<BigDecimal> totais12 = new ArrayList<>();

        //Popular totais
        for (Integer anexo : anexos) {
            totais12.add(new BigDecimal("0"));
        }

        //Percorrer meses do periodo
        for (Faturamento fatMes : faturamentos) {
            if (fatMes.getAnoMes() >= periodoInicio && fatMes.getAnoMes() <= periodoFim) {
                //adicionar os valores de cada anexo
                for (int i = 0; i < anexos.length; i++) {
                    Integer anexo = anexos[i];
                    if(anexo != 0){
                        try {
                            BigDecimal valorAnexoColuna = fatMes.getValores().get(anexo).getBigDecimal();
                            //Adiciona o valor da coluna do anexo nos totais do anexo e no total
                            if (anexos[0] == 0) {
                                totais12.set(0, totais12.get(0).add(valorAnexoColuna));
                            }
                            totais12.set(i, totais12.get(i).add(valorAnexoColuna));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return totais12;
    }

    private void setFaturamentoTotal() {
        for (int i = 1; i < valores.size(); i++) {
            Valor valor = valores.get(i);
            faturamentoTotal = faturamentoTotal.add(valor.getBigDecimal());
        }
    }

    public BigDecimal getFaturamentoTotal() {
        return faturamentoTotal;
    }

    public int getMes() {
        return mes;
    }

    public int getAno() {
        return ano;
    }

    public List<Valor> getValores() {
        return valores;
    }

    public BigDecimal getValorSimples(List<BigDecimal> fats12, List<SimplesNacional> tabelasSimplesNacional) {
        BigDecimal valorSimples = new BigDecimal(0);

        BigDecimal faturamentoTotalAnexos = fats12.get(0);
        /*Percorre de 1 a 5*/
        for (int i = 1; i <= 5; i++) {
            //Se tiver valor naquele mês no anexo
            if (valores.get(i).getBigDecimal().compareTo(BigDecimal.ZERO) > 0) {
                /*//Se tiver valor naquele anexo nos ultimos 12 meses
            if (fats12.get(i).compareTo(BigDecimal.ZERO) > 0) {*/

 /*Busca simples nacional do nro do anexo e enquadramento no valor*/
                SimplesNacional anexoSimples = getSimplesAnexo(tabelasSimplesNacional, i, faturamentoTotalAnexos);

                //Se tiver encontrado o anexo
                if (anexoSimples != null) {
                    try {
                        BigDecimal aliquotaParcial = anexoSimples.getAliquota();
                        BigDecimal valorAliquotaAnexo = faturamentoTotalAnexos.multiply(aliquotaParcial);
                        valorAliquotaAnexo = valorAliquotaAnexo.subtract(anexoSimples.getDescontar());
                        valorAliquotaAnexo = valorAliquotaAnexo.divide(faturamentoTotalAnexos, 6, RoundingMode.HALF_EVEN);

                        valorSimples = valorSimples.add(valores.get(i).getBigDecimal().multiply(valorAliquotaAnexo));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return valorSimples;
    }

    private SimplesNacional getSimplesAnexo(List<SimplesNacional> tabelasSimplesNacional, int nroAnexo, BigDecimal fat) {
        SimplesNacional anexoSimples = null;
        for (SimplesNacional simplesNacional : tabelasSimplesNacional) {
            if (simplesNacional.getNroAnexo() == nroAnexo
                    && fat.compareTo(simplesNacional.getValorInicial()) >= 0
                    && fat.compareTo(simplesNacional.getValorFinal()) <= 0) {
                anexoSimples = simplesNacional;
                break;
            }
        }
        return anexoSimples;
    }

}
