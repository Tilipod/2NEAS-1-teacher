package ru.tilipod.util;

import java.util.Random;

public class RandomUtil {

    public static double[][][] randomArray3(int m, int n, int k) {
        Random random = new Random(System.currentTimeMillis());
        double[][][] result = new double[m][n][k];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int l = 0; l < k; l++) {
                    result[i][j][l] = random.nextDouble();
                }
            }
        }

        return result;
    }
}
