package Model;

import Auxiliar.Valor;
import JExcel.JExcel;
import Model.Entity.Faturamento;
import Model.Entity.SimplesNacional;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Template_Model {

    private File arquivo;

    private int ano_1 = 0;
    private int ano_2 = 0;

    private List<Faturamento> faturamentos = new ArrayList<>();
    private List<Valor> parametros = new ArrayList<>();
    private List<SimplesNacional> tabelasSimplesNacional = new ArrayList<>();

    private List<Valor> simplesNacionalImpostos = new ArrayList<>();
    private BigDecimal totalSimplesNacional = new BigDecimal(0);

    private List<Valor> lucroPresumidoImpostos = new ArrayList<>();
    private BigDecimal totalLucroPresumido = new BigDecimal(0);

    private List<Valor> lucroRealImpostos = new ArrayList<>();
    private BigDecimal totalLucroReal = new BigDecimal(0);

    /**
     * Define arquivo e novo objeto.
     */
    public Template_Model(File arquivo) {
        this.arquivo = arquivo;
    }

    /**
     * Abre o arquivo passado no construtor e cria lista de faturamento e lista
     * de parametros.
     *
     * @return Nada se estiver tudo ok, se não, retorna o erro.
     */
    public String buscarValores() {
        String r = "";

        try {
            //Abre arquivo
            XSSFWorkbook wk = new XSSFWorkbook(arquivo);

            //Faturamento
            r = montarLista(wk, "Faturamento");
            if (r.equals("")) {
                r = montarLista(wk, "Parametros");
                if (r.equals("")) {
                    r = montarLista(wk, "Simples Nacional");
                    if (r.equals("")) {
                        r = calcularSimplesNacional();
                        if (r.equals("")) {
                            r = calcularLucroPresumido();
                            if (r.equals("")) {
                                r = calcularLucroReal();
                            }
                        }
                    }
                }
            }

            wk.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return r;
    }

    /**
     * Exibe o nome do mês conforme números fornecidos.
     *
     * @return para 202001 irá retornar JAN
     */
    private String mesAbreviado(Integer anoMesInt) {
        try {
            String anoMes = anoMesInt.toString();

            String[] mesesAbreviados = new String[]{
                "JAN", "JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
            };
            int mes = Integer.valueOf(anoMes.substring(4, 6));
            return mesesAbreviados[mes];
        } catch (Exception e) {
            return "JAN";
        }
    }

    /**
     * Se todos os primeiros 12 faturamentos tiver faturamento total maior que
     * 0, completa valores dos impostos faltantes usando o simples nacional.
     */
    private String calcularSimplesNacional() {
        //PARA VERIFICAÇÃO:
//        StringBuilder textoVerificação =  new StringBuilder("IMPOSTO SIMPLES NACIONAL: ");

        //Verifica se todos meses tiveram valores maiores que 0 no total
        long qtd = faturamentos.stream().filter(f -> f.getFaturamentoTotal().compareTo(BigDecimal.ZERO) == 1).count();
        if (qtd >= 24) {
            //Percorre todos os meses do segundo ano
            for (Faturamento faturamento : faturamentos) {
                if (faturamento.getAno() == ano_2) {
                    int mes = faturamento.getMes();
                    int mesAnterior = mes - (mes == 1 ? -11 : 1);
                    int anoComparado = ano_2 - (mes == 1 ? 1 : 0);

                    /*Pega periodos*/
                    int periodoInicial = Integer.valueOf(ano_1 + (mes < 10 ? "0" : "") + mes);
                    int periodoFinal = Integer.valueOf(anoComparado + (mesAnterior < 10 ? "0" : "") + mesAnterior);

                    List<BigDecimal> fats12 = Faturamento.getSumFaturamentosMensais(faturamentos, periodoInicial, periodoFinal);

                    //Faz calculo de todos anexos
                    BigDecimal valorSimples = faturamento.getValorSimples(fats12, tabelasSimplesNacional);

                    //Define nos valores, o valor 0 (imposto)
                    faturamento.getValores().get(0).setString(valorSimples.toString());

                    //Para verificação
//                    textoVerificação.append("\n");
//                    textoVerificação.append("Mês: ");
//                    textoVerificação.append(mes);
//                    textoVerificação.append("   -   Imposto: ");
//                    textoVerificação.append(faturamento.getValores().get(0).getValor().getBigDecimal().setScale(2,RoundingMode.HALF_EVEN).toString());
                }
            }

            //Faz soma dos impostos
            Integer inicio = Integer.valueOf(ano_2 + "01");
            Integer fim = Integer.valueOf(ano_2 + "12");

            for (Faturamento faturamento : faturamentos) {
                if (faturamento.getAnoMes() >= inicio && faturamento.getAnoMes() <= fim) {
                    simplesNacionalImpostos.add(new Valor(faturamento.getValores().get(0).getString(), "Simples " + mesAbreviado(faturamento.getAnoMes())));
                    totalSimplesNacional = totalSimplesNacional.add(faturamento.getValores().get(0).getBigDecimal());
                }
            }

            //Para verificação
//            View.View.render(textoVerificação.toString());
            return "";
        } else {
            return "Todos os meses devem ter faturamento total maior que 0. Caso o mês ainda não exista, faça uma projeção.";
        }
    }

    /**
     * Calcula valores Lucro presumido trimestral, mensal e anual
     */
    private String calcularLucroPresumido() {
        String r = "";

        try {

            //Pega todos os parâmetros que irá precisar
            //IR e CS
            BigDecimal ir_comercio = getParametroValor("LP - IR COMERCIO").getBigDecimal();
            BigDecimal ir_servico = getParametroValor("LP - IR SERVICO").getBigDecimal();
            BigDecimal cs_comercio = getParametroValor("LP - CS COMERCIO").getBigDecimal();
            BigDecimal cs_servico = getParametroValor("LP - CS SERVICO").getBigDecimal();

            //Adicional
            BigDecimal adicionalDescontar = getParametroValor("ADICIONAL DESCONTAR").getBigDecimal();
            BigDecimal adicionalPorcentagem = getParametroValor("ADICIONAL PORCENTAGEM").getBigDecimal();
            BigDecimal adicionalPorcentagemComercio = getParametroValor("LP - ADICIONAL PORCENTAGEM COMERCIO").getBigDecimal();
            BigDecimal adicionalPorcentagemServico = getParametroValor("LP - ADICIONAL PORCENTAGEM SERVICO").getBigDecimal();

            //Pis, cofins e ISS
            BigDecimal pis = getParametroValor("LP - PIS").getBigDecimal();
            BigDecimal cofins = getParametroValor("LP - COFINS").getBigDecimal();
            BigDecimal issServico = getParametroValor("LP - ISS S/ SERVIÇO").getBigDecimal();

            //Inss patronal
            BigDecimal inssPatronal = getParametroValor("INSS PATRONAL ANUAL").getBigDecimal();

            //Calcula impostos de trimestre
            int mesInicial = -2;
            int mesFinal = 0;

            //4 Vezes adiciona 3 meses ao mes incial e final
            for (int i = 1; i <= 4; i++) {
                mesInicial = mesInicial + 3;
                mesFinal = mesFinal + 3;

                Integer periodoInicial = Integer.valueOf(ano_2 + (mesInicial < 10 ? "0" : "") + mesInicial);
                Integer periodoFinal = Integer.valueOf(ano_2 + (mesFinal < 10 ? "0" : "") + mesFinal);
                String periodo = mesAbreviado(periodoInicial) + " até " + mesAbreviado(periodoFinal);

                List<BigDecimal> comercioValoresMesesTrimestre = Faturamento
                        .getSumFaturamentosMensais(new Integer[]{0, 1}, faturamentos, periodoInicial, periodoFinal);
                List<BigDecimal> servicoValoresMesesTrimestre = Faturamento
                        .getSumFaturamentosMensais(new Integer[]{0, 3, 4, 5}, faturamentos, periodoInicial, periodoFinal);

                BigDecimal comercioValorTrimestral = comercioValoresMesesTrimestre.get(0);
                BigDecimal servicoValorTrimestral = servicoValoresMesesTrimestre.get(0);

                BigDecimal total_3T = comercioValorTrimestral.add(servicoValorTrimestral); //comercio + servico

                //IR e CS de comercio(I) e Serviço(III, IV, V)
                Valor valor_ir_comercio = new Valor(comercioValorTrimestral.multiply(ir_comercio).toString(), "IR Comércio " + periodo);
                Valor valor_ir_servico = new Valor(servicoValorTrimestral.multiply(ir_servico).toString(), "IR Serviço " + periodo);

                Valor valor_cs_comercio = new Valor(comercioValorTrimestral.multiply(cs_comercio).toString(), "CS Comércio " + periodo);
                Valor valor_cs_servico = new Valor(servicoValorTrimestral.multiply(cs_servico).toString(), "CS Serviço " + periodo);

                Valor valor_adicional_servico = new Valor("0.0", "Adicional Serviço" + periodo);
                Valor valor_adicional_comercio = new Valor("0.0", "Adicional Comércio" + periodo);
                
                //Adicional porcentagem
                if (servicoValorTrimestral.compareTo(BigDecimal.ZERO) != 0) {
                    valor_adicional_servico = new Valor(total_3T.multiply(adicionalPorcentagemServico)
                            .subtract(adicionalDescontar)
                            .multiply(adicionalPorcentagem).toString(),
                            "Adicional Serviço " + periodo);
                    if (valor_adicional_servico.getBigDecimal().compareTo(BigDecimal.ZERO) == -1) {
                        valor_adicional_servico.setString(new BigDecimal("0").toString());
                    }
                }

                if (comercioValorTrimestral.compareTo(BigDecimal.ZERO) != 0) {
                    valor_adicional_comercio = new Valor(total_3T.multiply(adicionalPorcentagemComercio)
                            .subtract(adicionalDescontar)
                            .multiply(adicionalPorcentagem).toString(),
                            "Adicional Comércio " + periodo);
                    if (valor_adicional_comercio.getBigDecimal().compareTo(BigDecimal.ZERO) == -1) {
                        valor_adicional_comercio.setString(new BigDecimal("0").toString());
                    }
                }

                //Adiciona impostos nas listas
                lucroPresumidoImpostos.add(valor_ir_comercio);
                lucroPresumidoImpostos.add(valor_ir_servico);
                lucroPresumidoImpostos.add(valor_cs_comercio);
                lucroPresumidoImpostos.add(valor_cs_servico);
                lucroPresumidoImpostos.add(valor_adicional_comercio);
                lucroPresumidoImpostos.add(valor_adicional_servico);
            }

            //Calcula impostos mensais
            for (int i = 1; i <= 12; i++) {
                int mes = i;

                Integer periodo = Integer.valueOf(ano_2 + (mes < 10 ? "0" : "") + mes);
                String periodoStr = "Mês " + mesAbreviado(periodo);

                //PIS e COFINS
                List<BigDecimal> valorTotalMes = Faturamento.getSumFaturamentosMensais(faturamentos, periodo, periodo);
                lucroPresumidoImpostos.add(new Valor(valorTotalMes.get(0).multiply(pis).toString(), "PIS " + periodoStr));
                lucroPresumidoImpostos.add(new Valor(valorTotalMes.get(0).multiply(cofins).toString(), "COFINS " + periodoStr));

                //ISS sobre Serviço (Somente III,IV e V)
                List<BigDecimal> servicoTotalMes = Faturamento.getSumFaturamentosMensais(new Integer[]{3, 4, 5}, faturamentos, periodo, periodo);
                lucroPresumidoImpostos.add(new Valor(servicoTotalMes.get(0).multiply(issServico).toString(), "ISS S/ Serviço " + periodoStr));
            }

            //Adiciona inss patronal aos impostos
            lucroPresumidoImpostos.add(new Valor(inssPatronal.toString(), "INSS Patronal"));

            //Soma todos impostos  + ISS Patronal
            //Soma todos impostos
            for (Valor lucroPresumidoImposto : lucroPresumidoImpostos) {
                totalLucroPresumido = totalLucroPresumido.add(lucroPresumidoImposto.getBigDecimal());
            }

            r = "";
        } catch (Exception e) {
            r = "Ocorreu um erro ao calcular o lucro presumido: " + e;
        }
        return r;
    }

    /**
     * Calcula valores do lucro real anual e trimestral
     */
    private String calcularLucroReal() {
        String r = "";

        try {

            //Faturamento anual
            BigDecimal faturamentoAnual = Faturamento.getSumFaturamentosMensais(
                    faturamentos, Integer.valueOf(ano_2 + "01"), Integer.valueOf(ano_2 + "12")
            ).get(0);

            //Pega todos os parâmetros que irá precisar
            //IR e CS
            BigDecimal ir = getParametroValor("LR - IR").getBigDecimal();
            BigDecimal cs = getParametroValor("LR - CS").getBigDecimal();

            //Adicional
            BigDecimal adicionalDescontar = getParametroValor("ADICIONAL DESCONTAR").getBigDecimal();
            BigDecimal adicionalPorcentagem = getParametroValor("ADICIONAL PORCENTAGEM").getBigDecimal();

            //Pis, cofins e ISS
            BigDecimal pis = getParametroValor("LR - PIS").getBigDecimal();
            BigDecimal cofins = getParametroValor("LR - COFINS").getBigDecimal();

            //Lucro ou prejuizo
            List<Valor> lucro_prejuizos = getParametrosValor("LR - LUCRO/PREJUIZO ");

            //DESCONTAR PIS E COFINS
            List<Valor> desconto_pis_cofins = getParametrosValor("LR - BASE ");

            //Inss patronal
            Valor inssPatronal = getParametroValor("INSS PATRONAL ANUAL");
            lucroRealImpostos.add(inssPatronal);

            //Calcular PIS e Cofins
//            Valor valor_pis = new Valor(pis.multiply(faturamentoAnual).toString(), "PIS S/ Faturamento");
//            Valor valor_cofins = new Valor(cofins.multiply(faturamentoAnual).toString(), "COFINS S/ Faturamento");
//
//            lucroRealImpostos.add(valor_pis);
//            lucroRealImpostos.add(valor_cofins);

            desconto_pis_cofins.forEach((desconto_pis_cofin) -> {
                String nomeBase = desconto_pis_cofin.getApelido().toLowerCase();
                BigDecimal sinal = new BigDecimal(nomeBase.contains("lr - base cr")?"-1":"1");
                
                String nomeImposto = desconto_pis_cofin.getApelido().replaceAll("-", " ");
                lucroRealImpostos.add(new Valor(
                        pis.multiply(desconto_pis_cofin.getBigDecimal()).multiply(sinal).toString(),
                        "PIS S/ " + nomeImposto
                ));
                lucroRealImpostos.add(new Valor(
                        cofins.multiply(desconto_pis_cofin.getBigDecimal()).multiply(sinal).toString(),
                        "COFINS S/ " + nomeImposto
                ));
            });

            //Calcula IR e CS Lucros
            lucro_prejuizos.forEach((lucro_prejuizo) -> {
                if (lucro_prejuizo.getBigDecimal().compareTo(BigDecimal.ZERO) == 1) {
                    lucroRealImpostos.add(
                            new Valor(
                                    lucro_prejuizo.getBigDecimal().multiply(ir).toString(),
                                    "IR - " + lucro_prejuizo.getApelido()
                            )
                    );
                    lucroRealImpostos.add(
                            new Valor(
                                    lucro_prejuizo.getBigDecimal().multiply(cs).toString(),
                                    "CS - " + lucro_prejuizo.getApelido()
                            )
                    );

                    BigDecimal adicional = lucro_prejuizo.getBigDecimal().subtract(adicionalDescontar);
                    adicional = adicional.multiply(adicionalPorcentagem);
                    if (adicional.compareTo(BigDecimal.ZERO) != -1) {
                        lucroRealImpostos.add(
                                new Valor(
                                        adicional.toString(),
                                        "Adicional - " + lucro_prejuizo.getApelido()
                                )
                        );
                    }
                }
            });

            //Soma todos impostos
            lucroRealImpostos.forEach((imposto) -> {
                totalLucroReal = totalLucroReal.add(imposto.getBigDecimal());
            });

            r = "";
        } catch (Exception e) {
            r = "Ocorreu um erro ao calcular o lucro real: " + e;
        }
        return r;
    }

    /**
     * Monta lista da aba Simples Nacional.
     */
    private String montarListaSimplesNacional(XSSFSheet sheet) {
        //Percorre todas linhas
        for (int i = 0; i < sheet.getLastRowNum() + 1; i++) {
            try {
                XSSFRow row = sheet.getRow(i);
                Valor anexo = new Valor(JExcel.getStringCell(row.getCell(0)).replaceAll(",0", ""));
                Valor nome = new Valor(JExcel.getStringCell(row.getCell(1)));
                Valor de = new Valor(JExcel.getStringCell(row.getCell(2)).replaceAll(",", "\\."));
                Valor ate = new Valor(JExcel.getStringCell(row.getCell(3)).replaceAll(",", "\\."));
                Valor aliquota = new Valor(JExcel.getStringCell(row.getCell(4)).replaceAll(",", "\\."));
                Valor desconto = new Valor(JExcel.getStringCell(row.getCell(5)).replaceAll(",", "\\."));

                if (!anexo.getInteger().equals((Integer) 0)) {
                    tabelasSimplesNacional.add(
                            new SimplesNacional(
                                    anexo.getInteger(),
                                    nome.getString(),
                                    de.getBigDecimal(),
                                    ate.getBigDecimal(),
                                    aliquota.getBigDecimal(),
                                    desconto.getBigDecimal()
                            )
                    );
                }
            } catch (Exception e) {
            }
        }
        return "";
    }

    /**
     * Monta lista java da aba escolhida
     *
     * @return o valor da função de montar lista da lista selecionada
     */
    private String montarLista(XSSFWorkbook wk, String nomeAba) {
        String r = "";

        try {
            XSSFSheet sheet = wk.getSheet(nomeAba);
            try {
                switch (nomeAba) {
                    case "Faturamento":
                        r = montarListaFaturamento(sheet);
                        break;
                    case "Parametros":
                        r = montarListaParametros(sheet);
                        break;
                    case "Simples Nacional":
                        r = montarListaSimplesNacional(sheet);
                        break;
                    default:
                        r = "Função para lista " + nomeAba + " não encontrada!";
                        break;
                }
            } catch (Exception e) {
                r = "Erro ao buscar dados do " + nomeAba + ": " + e;
            }
        } catch (Exception e) {
            r = "Problema ao encontrar aba '" + nomeAba + "'";
        }

        return r;
    }

    /**
     * Monta lista de faturamentos e define o primeiro e segundo ano.
     *
     * @return Nada se estiver tudo ok, se não retorna o erro.
     */
    private String montarListaFaturamento(XSSFSheet sheet) {
        /*Define Anos*/
        ano_1 = (int) sheet.getRow(1).getCell(2).getNumericCellValue();
        ano_2 = (int) sheet.getRow(18).getCell(2).getNumericCellValue();

        /* Montar faturamentos ano 1 */
        adicionaFaturamentosTabela(sheet, ano_1, 4, 16);
        adicionaFaturamentosTabela(sheet, ano_2, 21, 33);

        return "";
    }

    /**
     * Adiciona os faturamentos do ano informado entre as duas linhas passadas.
     *
     * @param sheet Aba do Excel.
     * @param ano Ano
     *
     */
    private void adicionaFaturamentosTabela(XSSFSheet sheet, int ano, int primeiraLinha, int ultimaLinha) {
        for (int i = primeiraLinha; i < ultimaLinha; i++) {
            int mes = i - (primeiraLinha - 1);
            List<Valor> valores = new ArrayList<>();

            valores.add(new Valor("0", "Simples"));
            valores.add(new Valor("0", "I - Comércio"));
            valores.add(new Valor("0", "II - Fabricas"));
            valores.add(new Valor("0", "III - Servicos"));
            valores.add(new Valor("0", "IV - Servicos"));
            valores.add(new Valor("0", "V - Servicos"));

            //Pega valores
            try {
                XSSFRow row = sheet.getRow(i);

                //Percorre as 6 colunas
                for (int j = 2; j < 8; j++) {
                    try {
                        valores.get(j - 2).setString(String.valueOf(row.getCell(j).getNumericCellValue()));
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }

            //adiciona
            faturamentos.add(new Faturamento(mes, ano, valores));
        }
    }

    /**
     * Monta lista de parametros
     *
     * @return Nada se estiver tudo ok, se não retorna o erro.
     */
    private String montarListaParametros(XSSFSheet sheet) {
        for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {
            try {
                Row row = sheet.getRow(i);
                //VALOR - APELIDO
                parametros.add(new Valor(JExcel.getStringCell(row.getCell(1)), JExcel.getStringCell(row.getCell(0))));
            } catch (Exception e) {
            }
        }

        return "";
    }

    /**
     * Pega a classe valor
     */
    private Valor getParametroValor(String nomeParametro) {
        Valor r = null;
        for (Valor parametro : parametros) {
            if (parametro.getApelido().equals(nomeParametro)) {
                r = parametro;
                break;
            }
        }

        return r;
    }
    
    /**
     *  Pega nome comparativo ( parametro 'NOME EMPRESA')
     */
    public String getNomeComparativo(){
        return getParametroValor("NOME EMPRESA").getString();
    }

    /**
     * Pega uma lista de parametros que contenham uma palavra no nome
     */
    private List<Valor> getParametrosValor(String nomeParametro) {
        List<Valor> r = new ArrayList<>();

        for (Valor parametro : parametros) {
            if (parametro.getApelido().contains(nomeParametro)) {
                r.add(parametro);
            }
        }

        return r;
    }

    private void setParametroValor(String nomeParametro, String novoValorStr) {
        for (Valor parametro : parametros) {
            if (parametro.getApelido().equals(nomeParametro)) {
                parametro.setString(novoValorStr);
                break;
            }
        }
    }

    /**
     * Verifica se o arquivo escolhido foi previamente criado por este programa.
     *
     * @return TRUE se o arquivo foi gerado por este programa e FALSE se não
     * foi.
     */
    public static boolean éAutentico(File arquivo) {
        boolean b = false;
        String senhaSheets = "olokinhomeu";
        try {
            XSSFWorkbook wk = new XSSFWorkbook(arquivo);
            XSSFSheet sheet;

            //Verifica aba very hidden autenticada
            sheet = wk.getSheet("Auth");
            System.out.println(sheet.getRow(0).getCell(0).getStringCellValue());

            //Verifica numero de sheets
            if (wk.getNumberOfSheets() == 4) {
                //Verifica se sheet faturamento ainda está protegida e com password
                sheet = wk.getSheet("Faturamento");
                if (sheet != null) {
                    if (sheet.validateSheetPassword(senhaSheets)) {
                        //Verifica se sheet Parametros ainda está protegida e com password
                        sheet = wk.getSheet("Parametros");
                        if (sheet != null) {
                            if (sheet.validateSheetPassword(senhaSheets)) {
                                //Verifica aba simples nacional
                                sheet = wk.getSheet("Simples Nacional");
                                if (sheet != null) {
                                    b = sheet.validateSheetPassword(senhaSheets);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

        return b;
    }

    public List<Valor> getSimplesNacionalImpostos() {
        return simplesNacionalImpostos;
    }

    public BigDecimal getTotalSimplesNacional() {
        return totalSimplesNacional;
    }

    public List<Valor> getLucroPresumidoImpostos() {
        return lucroPresumidoImpostos;
    }

    public BigDecimal getTotalLucroPresumido() {
        return totalLucroPresumido;
    }

    public List<Valor> getLucroRealImpostos() {
        return lucroRealImpostos;
    }

    public BigDecimal getTotalLucroReal() {
        return totalLucroReal;
    }
}
