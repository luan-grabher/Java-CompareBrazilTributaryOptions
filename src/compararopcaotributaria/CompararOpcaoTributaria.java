package compararopcaotributaria;

import Control.BaixarTemplates_Control;
import Control.CompararOpcaoTributaria_Control;
import View.StatusProgram;
import View.View;
import javax.swing.JFrame;

public class CompararOpcaoTributaria {
    private static final String[] yesNo = {"Sim","Não"};
    private static final String[] routes = {"Baixar Templates Excel","Iniciar Comparação"};

    public static void main(String[] args) {
        try{
            //Define status
            JFrame frameStatus = new StatusProgram();
            frameStatus.setVisible(true);
            
            runRoute(View.chooseOption("Escolha o que fazer", "O que você deseja fazer?", routes));
        }catch(Exception e){
            e.printStackTrace();
        }
        
        View.render("Programa finalizado!");
        
        System.exit(0);
    }
    
    public static void runRoute(int route){
        Object control;
        switch(route){
            case 1:
                control = new CompararOpcaoTributaria_Control();
                break;
            default:
                control = new BaixarTemplates_Control();
                if(View.chooseOption("Iniciar Comparação?", "Deseja iniciar a comparação agora?", yesNo) == 0){
                    runRoute(1);
                }
                break;
        }
    }
    
}
