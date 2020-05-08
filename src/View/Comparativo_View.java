package View;

import Auxiliar.Valor;
import ChunkList.ChunkList;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Comparativo_View {

    private final List<Valor> simplesNacionalImpostos;
    private final BigDecimal totalSimplesNacional;

    private final List<Valor> lucroPresumidoImpostos;
    private final BigDecimal totalLucroPresumido;
    
    private final List<Valor> lucroRealImpostos;
    private final BigDecimal totalLucroReal;
    
    private String titulo = "";

    private List<XSSFCellStyle> styles = new ArrayList<>();
    private final int STYLE_TITULO_TRIBUTACAO = 0;
    private final int STYLE_TITULO_IMPOSTO = 1;
    private final int STYLE_VALOR_IMPOSTO = 2;

    public Comparativo_View(List<Valor> simplesNacionalImpostos, BigDecimal totalSimplesNacional, List<Valor> lucroPresumidoImpostos, BigDecimal totalLucroPresumido, List<Valor> lucroRealImpostos, BigDecimal totalLucroReal) {
        this.simplesNacionalImpostos = simplesNacionalImpostos;
        this.totalSimplesNacional = totalSimplesNacional;
        this.lucroPresumidoImpostos = lucroPresumidoImpostos;
        this.totalLucroPresumido = totalLucroPresumido;
        this.lucroRealImpostos = lucroRealImpostos;
        this.totalLucroReal = totalLucroReal;
    }
    
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    
    public String criarArquivoComparativo(String localSalvar) {
        String r = "";
        try {
            File arquivo = new File("Comparativo.xlsx");

            if (arquivo.exists()) {
                //Abrir Workbook comparativo
                XSSFWorkbook wk = new XSSFWorkbook(arquivo);
                XSSFSheet sheet = wk.getSheetAt(0);

                //Completar totais
                r = completarTotais(sheet);
                if (r.equals("")) {
                    
                    //Cria estilos
                    r = criarEstilos(wk);
                    if (r.equals("")) {
                        //Adicionar Impostos (3X)
                        r = inserirImpostos(sheet);
                        if (r.equals("")) {
                            //Define titulo
                            if(!titulo.equals("")){
                                sheet.getRow(1).getCell(5).setCellValue(titulo);
                            }
                            
                            //Salva na pasta escolhida (Por enquanto no desktop)
                            salvarNovoArquivo(wk,localSalvar);
                        }
                    }
                }

            } else {
                r = "O arquivo template do comparativo não existe na pasta raiz do programa!";
            }
        } catch (Exception e) {
            r = "Erro ao criar arquivo comparativo: " + e;
        }

        return r;
    }

    private String criarEstilos(XSSFWorkbook wk) {
        XSSFFont font_bold = wk.createFont();
        XSSFFont font_normal = wk.createFont();

        font_bold.setBold(true);
        font_bold.setFontHeight(12);
        font_normal.setFontHeight(12);

        for (int i = 0; i < 3; i++) {
            styles.add(wk.createCellStyle());
            styles.get(i).setAlignment(HorizontalAlignment.CENTER);
            styles.get(i).setVerticalAlignment(VerticalAlignment.CENTER);
            styles.get(i).setWrapText(true);
        }

        //define fontes
        styles.get(STYLE_TITULO_IMPOSTO).setFont(font_bold);
        styles.get(STYLE_TITULO_TRIBUTACAO).setFont(font_bold);
        styles.get(STYLE_VALOR_IMPOSTO).setFont(font_normal);

        //define r$
        styles.get(STYLE_VALOR_IMPOSTO).setDataFormat((short) 8);
        
        //Titulo Imposto
        styles.get(STYLE_TITULO_IMPOSTO).setBorderBottom(BorderStyle.THIN);
        
        //Titulo Tributação
        styles.get(STYLE_TITULO_TRIBUTACAO).setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        styles.get(STYLE_TITULO_TRIBUTACAO).setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return "";
    }

    private String inserirImpostos(XSSFSheet sheet) {
        String r = "";
        try {
            //Insere impostos
            r = inserirImpostoTributacao("SIMPLES NACIONAL", sheet, simplesNacionalImpostos);
            if (r.equals("")) {
                r = inserirImpostoTributacao("LUCRO PRESUMIDO", sheet, lucroPresumidoImpostos);
                if (r.equals("")) {
                    r = inserirImpostoTributacao("LUCRO REAL", sheet, lucroRealImpostos);
                }
            }

        } catch (Exception e) {
            r = "Erro ao inserir impostos no comparativo: " + e;
        }

        return r;
    }
    
    private List<Valor> pegaListaSemZeros(List<Valor> listaImpostos){
        List<Valor> newList =  new ArrayList<>();
        for (Valor listaImposto : listaImpostos) {
            if(listaImposto.getBigDecimal().compareTo(BigDecimal.ZERO) != 0){
                newList.add(listaImposto);
            }
        }
        
        return newList;
    }

    private String inserirImpostoTributacao(String titulo, XSSFSheet sheet, List<Valor> listaImpostos) {
        String r = "";
        try {
            //insere linha em branco acima
            if (insertRowAtLastRow(sheet)) {

                //Coloca titulo
                insertRowAtLastRow(sheet); //insere linha para titulo
                Row row = sheet.getRow(sheet.getLastRowNum()); //Pega linha
                row.getCell(3).setCellValue(titulo); //insere titulo
                row.getCell(3).setCellStyle(styles.get(STYLE_TITULO_TRIBUTACAO));
                sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 3, 12)); //merge cells
                //Aplciar estilos depois ou durante a inserção
                //Divide em grupos
                listaImpostos =  pegaListaSemZeros(listaImpostos);
                List<List<Valor>> gruposImpostos = ChunkList.chunk(listaImpostos, 3);

                //Percorre os grupos
                for (List<Valor> grupoImposto : gruposImpostos) {
                    //insere linhas
                    insertRowAtLastRow(sheet); //linha em branco
                    insertRowAtLastRow(sheet); //titulo impostos
                    insertRowAtLastRow(sheet); //valor imposto

                    int lastRow = sheet.getLastRowNum();

                    //Percorre colunas
                    int col = -4;
                    for (Valor imposto : grupoImposto) {
                        col = col + 5;

                        //insere
                        sheet.getRow(lastRow - 1).getCell(col).setCellValue(imposto.getApelido());
                        sheet.getRow(lastRow).getCell(col).setCellValue(imposto.getBigDecimal().doubleValue());
                        
                        //mescla
                        sheet.addMergedRegion(new CellRangeAddress(lastRow - 1, lastRow - 1, col, col + 3));
                        sheet.addMergedRegion(new CellRangeAddress(lastRow, lastRow, col, col + 3));

                        for (int i = col; i < col+4 ; i++) {
                            sheet.getRow(lastRow - 1).getCell(i).setCellStyle(styles.get(STYLE_TITULO_IMPOSTO));
                            sheet.getRow(lastRow).getCell(i).setCellStyle(styles.get(STYLE_VALOR_IMPOSTO));
                        }
                    }
                }
            } else {
                r = "Erro ao adicionar nova linha na sheet dos comparativos.";
            }
        } catch (Exception e) {
            r = "Erro ao inserir os impostos da tributação " + titulo + ": " + e;
        }

        return r;
    }

    private boolean insertRowAtLastRow(XSSFSheet sheet) {
        try {
            sheet.createRow(sheet.getLastRowNum() + 1);
            Row row = sheet.getRow(sheet.getLastRowNum());
            for (int i = 0; i < 15; i++) {
                row.createCell(i);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String salvarNovoArquivo(XSSFWorkbook wk, String localSalvar) {
        File novoArquivo = new File(localSalvar + "\\Comparativo Opções Tributárias.xlsx");

        JExcel.JExcel.saveWorkbookAs(novoArquivo, wk);

        return "";
    }

    private String completarTotais(XSSFSheet sheet) {
        //arruma valores
        BigDecimal simples = totalSimplesNacional;
        BigDecimal lucroPresumido = totalLucroPresumido;
        BigDecimal lucroReal = totalLucroReal;

        sheet.getRow(7).getCell(1).setCellValue(simples.doubleValue());
        sheet.getRow(7).getCell(6).setCellValue(lucroPresumido.doubleValue());
        sheet.getRow(7).getCell(11).setCellValue(lucroReal.doubleValue());
        return "";
    }

}
