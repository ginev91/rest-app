package org.example.main.model.enums;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Macros {
    private int protein;
    private int fat;
    private int carbs;

    public void setFats(int i) {
        this.fat = i;
    }
}