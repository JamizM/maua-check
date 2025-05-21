package com.maua.check.mauacheck.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Aluno {
    //id temporario
    //atributos do aluno, horario e imageUrl, são mutaveis, porem exite, nome, ra e curso
    @Id
    public int id;

    @NotNull
    public String horario;


    @NotNull
    public String imageUrl;
}
