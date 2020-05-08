package Control;
import View.View;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BaixarTemplates_Control {

    public BaixarTemplates_Control() {
        View.render("Escolha onde você deseja salvar o template...");
        File path = Selector.Pasta.selecionar();
        
        //Se for uma pasta válida
        if(Selector.Pasta.verifica(path.getAbsolutePath())){
            File template = new File("Template.xlsx");
            if(template.exists()){
                
                try{
                    File destino =  new File(path.getAbsolutePath() + "/Template Comparar Opcao Tributaria.xlsx");
                    Files.copy(template.toPath(), destino.toPath(),StandardCopyOption.REPLACE_EXISTING);
                    View.render("Sucesso! Template salvo em:\n" + path.getAbsolutePath());
                }catch(Exception e){
                    View.render("Erro ao criar template na pasta " + path.getAbsolutePath()  +"\nVocê está com um template aberto?");
                }
            }else{
                View.render("Erro interno:\nTemplate não encontrado na pasta do programa!");
            }
        }
    }
    
}
