package ru.tilipod.reforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.deeplearning4j.rl4j.space.Encodable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableState implements Encodable {

    private int x;

    private int y;

    private double[] values;

    @Override
    public double[] toArray() {
        return values;
    }
}
