package Control;

import Model.Template_Model;
import View.Comparativo_View;
import View.StatusProgram;
import View.View;
import java.io.File;

public class CompararOpcaoTributaria_Control {

    private File arquivoTemplate;
    private File pastaSalvar;

    private boolean executarFuncao(String retornoFuncao) {
        if ("".equals(retornoFuncao)) {
            return true;
        } else {
            View.render(retornoFuncao, "error");
            return false;
        }
    }

    public CompararOpcaoTributaria_Control() {
        //Seleciona arquivo
        View.render("Escolha o arquivo de template preenchido...");
        arquivoTemplate = Selector.Arquivo.selecionar("C:/Users/Ti01/Desktop", "Template Prenchido", "xlsx");
        //Verifica arquivo
        StatusProgram.texto.setText("Verificando arquivo...");
        if (Selector.Arquivo.verifica(arquivoTemplate.getAbsolutePath(), "xlsx")) {

            //Pasta onde irá salvar
            View.render("Escolha a pasta onde será salvo o comparativo...");
            pastaSalvar = Selector.Pasta.selecionar();
            //Verifica arquivo
            StatusProgram.texto.setText("Verificando pasta...");
            if (Selector.Pasta.verifica(arquivoTemplate.getAbsolutePath())) {
                executar();
            }
        }
    }

    public CompararOpcaoTributaria_Control(File arquivoTemplate, File pastaSalvar) {
        this.arquivoTemplate = arquivoTemplate;
        this.pastaSalvar = pastaSalvar;
        
        executar();
    }

    private void executar() {
        //Autentica Arquivo
        StatusProgram.texto.setText("Autenticando arquivo...");
        if (Template_Model.éAutentico(arquivoTemplate)) {
            //Modelo Template
            StatusProgram.texto.setText("Buscando informações do arquivo...");
            Template_Model template_modelo = new Template_Model(arquivoTemplate);
            if (executarFuncao(template_modelo.buscarValores())) {
                StatusProgram.texto.setText("Criando comparativo...");
                Comparativo_View comparativo = new Comparativo_View(
                        template_modelo.getSimplesNacionalImpostos(),
                        template_modelo.getTotalSimplesNacional(),
                        template_modelo.getLucroPresumidoImpostos(),
                        template_modelo.getTotalLucroPresumido(),
                        template_modelo.getLucroRealImpostos(),
                        template_modelo.getTotalLucroReal()
                );
                
                //Definir titulo
                String titulo = template_modelo.getNomeComparativo();
                comparativo.setTitulo(titulo);
                
                if (executarFuncao(comparativo.criarArquivoComparativo(pastaSalvar.getAbsolutePath()))) {
                    View.render("Programa terminado! Verifique a pasta:\n" + pastaSalvar.getAbsolutePath(), "sucess");
                }
            }
        } else {
            View.render("O arquivo não é autêntico! Por favor, reinicie o programa e escolha 'Baixar Template' para obter o template autenticado.");
        }
    }

}
