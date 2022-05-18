package ru.tilipod.reforcement;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.rl4j.network.dqn.DQN;

public class DQNWithAllowNetwork<NN extends DQN> extends DQN<NN> {

    public DQNWithAllowNetwork(MultiLayerNetwork mln) {
        super(mln);
    }

    public MultiLayerNetwork getNetwork() {
        return mln;
    }

}
