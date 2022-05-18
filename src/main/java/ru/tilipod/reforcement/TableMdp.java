package ru.tilipod.reforcement;

import lombok.Data;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;
import org.json.JSONObject;
import ru.tilipod.reforcement.enums.TableActionEnum;
import ru.tilipod.util.Constants;
import ru.tilipod.util.RandomUtil;

import java.util.Arrays;

@Data
public class TableMdp implements MDP<TableState, Integer, DiscreteSpace> {

    private double[][][] map;

    private int countStates;

    private int maxStep;

    private TableState tableState;

    private DiscreteSpace actionSpace;

    private ObservationSpace<TableState> observationSpace;

    public TableMdp(double[][][] map, int maxStep, int countStates) {
        this.map = map;
        this.maxStep = maxStep;
        this.actionSpace = new DiscreteSpace(Constants.DEFAULT_COUNT_ACTIONS);
        this.observationSpace = new ArrayObservationSpace<>(new int[] {Constants.DEFAULT_COUNT_ACTIONS});
        this.countStates = countStates;
        this.map[countStates - 1][countStates - 1] = new double[]{0, 0, 0, 0};
    }

    @Override
    public TableState reset() {
        return tableState = new TableState(0, 0, map[0][0]);
    }

    @Override
    public void close() {

    }

    private boolean isOut(int index) {
        TableActionEnum action = TableActionEnum.getByIndex(index);
        return tableState.getX() + action.getChangeX() < 0
               || tableState.getY() + action.getChangeY() < 0;
    }

    @Override
    public StepReply<TableState> step(Integer integer) {
        double reward = -Double.MAX_VALUE;
        int index = -1;
        double[] values = tableState.getValues();

        // Ищем лучшее действие по вознаграждению
        for (int i = 0; i < Constants.DEFAULT_COUNT_ACTIONS; i++) {
            if (Double.compare(values[i], reward) > 0 && !isOut(i)) {
                index = i;
                reward = values[i];
            }
        }

        TableActionEnum action = TableActionEnum.getByIndex(index);
        int newX = tableState.getX() + action.getChangeX(), newY = tableState.getY() + action.getChangeY();
        tableState = new TableState(newX, newY, map[newX][newY]);
        return new StepReply<>(tableState, reward, isDone(), new JSONObject("{}"));
    }

    @Override
    public boolean isDone() {
        return Arrays.stream(tableState.getValues()).allMatch(v -> Math.abs(v) <= 0.00001);
    }

    @Override
    public MDP<TableState, Integer, DiscreteSpace> newInstance() {
        return new TableMdp(RandomUtil.randomArray3(Constants.DEFAULT_COUNT_STATES, Constants.DEFAULT_COUNT_STATES, Constants.DEFAULT_COUNT_ACTIONS),
                Constants.DEFAULT_MAX_STEP_FOR_RL, Constants.DEFAULT_COUNT_STATES);
    }
}
