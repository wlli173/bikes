package br.edu.ifc.bikes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString
@Entity
@Table(name = "locacoes")
@EntityListeners(AuditingEntityListener.class)
public class Locacao implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_bicicleta", nullable = false)
    private Bicicleta bicicleta;

    @Column(name = "data_retirada", nullable = false)
    private LocalDateTime dataRetirada;

    @Column(name = "data_prevista_devolucao", nullable = false)
    private LocalDateTime dataPrevistaDevolucao;

    @Column(name = "data_devolucao")
    private LocalDateTime dataDevolucao;

    //por segurança só os 4 últimos dígitos do cartão são persistidos
    @Column(name = "cartao_final", nullable = false, length = 4)
    private String cartaoFinal;

    @Column(name = "cartao_titular", nullable = false, length = 100)
    private String cartaoTitular;

    @Column(name = "valor", precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ATIVA;

    @Column(name = "observacao", length = 255)
    private String observacao;

    @CreatedDate
    private LocalDateTime dataCriacao;
    @LastModifiedDate
    private LocalDateTime dataModificacao;
    @CreatedBy
    private String criadoPor;
    @LastModifiedBy
    private String modificadoPor;

    public enum Status {
        ATIVA, FINALIZADA
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        Locacao locacao = (Locacao) obj;
        return Objects.equals(id, locacao.id);
    }
}
