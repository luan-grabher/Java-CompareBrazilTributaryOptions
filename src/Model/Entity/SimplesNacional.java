package Model.Entity;

import java.math.BigDecimal;

public class SimplesNacional {
    private final Integer nroAnexo;
    private final String nome;
    private final BigDecimal valorInicial;
    private final BigDecimal valorFinal;
    private final BigDecimal aliquota;
    private final BigDecimal descontar;

    public SimplesNacional(Integer nroAnexo, String nome, BigDecimal valorInicial, BigDecimal valorFinal, BigDecimal aliquota, BigDecimal descontar) {
        this.nroAnexo = nroAnexo;
        this.nome = nome;
        this.valorInicial = valorInicial;
        this.valorFinal = valorFinal;
        this.aliquota = aliquota;
        this.descontar = descontar;
    }

    public Integer getNroAnexo() {
        return nroAnexo;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getValorInicial() {
        return valorInicial;
    }

    public BigDecimal getValorFinal() {
        return valorFinal;
    }

    public BigDecimal getAliquota() {
        return aliquota;
    }

    public BigDecimal getDescontar() {
        return descontar;
    }
    
    
    
}
