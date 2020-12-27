package com.packt.game.android;

import com.friendface.FriendFaceAPI;
import com.packt.game.ScoreHandler;

/**
 * Created by James on 17/06/2015.
 */
public class ScoreHandlerAndroid implements ScoreHandler {

    private final FriendFaceAPI friendFaceAPI;

    public ScoreHandlerAndroid(FriendFaceAPI friendFaceAPI) {
        this.friendFaceAPI = friendFaceAPI;
    }

    @Override
    public void postScore(String name, int score) {
        friendFaceAPI.postScore(name, score);
    }
}
