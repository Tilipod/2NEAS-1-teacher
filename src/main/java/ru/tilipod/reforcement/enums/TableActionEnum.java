package ru.tilipod.reforcement.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TableActionEnum {
    UP(0, 0, -1),
    RIGHT(1, 1, 0),
    DOWN(2, 0, 1),
    LEFT(3, -1, 0);

    private final int index;

    private final int changeX;

    private final int changeY;

    TableActionEnum(int index, int changeX, int changeY) {
        this.index = index;
        this.changeX = changeX;
        this.changeY = changeY;
    }

    public static TableActionEnum getByIndex(int index) {
        return Arrays.stream(TableActionEnum.values())
                .filter(v -> v.getIndex() == index)
                .findFirst()
                .orElseThrow(() -> new ClassCastException(String.format("Индекс %d не поддерживается для QLearning", index)));
    }
}
