package com.packt.game.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.friendface.FriendFaceAPI;
import com.packt.game.MyGdxGame;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FriendFaceAPI friendFaceAPI = new FriendFaceAPI();
		ScoreHandlerAndroid scoreHandlerAndroid = new ScoreHandlerAndroid(friendFaceAPI);

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(new MyGdxGame(scoreHandlerAndroid), config);
	}
}
