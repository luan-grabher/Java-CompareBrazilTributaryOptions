
package Testes;

import Control.CompararOpcaoTributaria_Control;
import java.io.File;
import View.StatusProgram;
import javax.swing.JFrame;

public class Teste {

    public static void main(String[] args) {
        testeGeral();
        
        System.exit(0);
    }
    
    private static void testeGeral(){
        File localSalvar =  new File("C:/Users/Ti01/Desktop");
        File arquivoTemplate =  new File("C:/Users/Ti01/Desktop/Template Comparar Opcao Tributaria 2.xlsx");
        
        StatusProgram frame = new StatusProgram();
        frame.setVisible(true);
        
        CompararOpcaoTributaria_Control controle = new CompararOpcaoTributaria_Control(arquivoTemplate, localSalvar);
    }
    
}
