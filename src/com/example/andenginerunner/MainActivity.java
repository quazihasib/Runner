package com.example.andenginerunner;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import org.anddev.andengine.audio.music.Music;
import org.anddev.andengine.audio.music.MusicFactory;
import org.anddev.andengine.audio.sound.Sound;
import org.anddev.andengine.audio.sound.SoundFactory;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.handler.timer.ITimerCallback;
import org.anddev.andengine.engine.handler.timer.TimerHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.LoopEntityModifier;
import org.anddev.andengine.entity.modifier.MoveByModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.CameraScene;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.AutoParallaxBackground;
import org.anddev.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.input.touch.controller.MultiTouch;
import org.anddev.andengine.extension.input.touch.controller.MultiTouchController;
import org.anddev.andengine.extension.input.touch.exception.MultiTouchException;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.IModifier.IModifierListener;

import com.example.andenginerunner.CoolDown;
import com.example.andenginerunner.ProjectilesPool;
import com.example.andenginerunner.TargetsPool;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.Toast;

public class MainActivity extends BaseGameActivity implements
		IOnSceneTouchListener {

	private Camera mCamera;

	// This one is for the font
	private BitmapTextureAtlas mFontTexture;
	private Font mFont;
	private ChangeableText score;

	// this one is for all other textures
	private BitmapTextureAtlas mBitmapTextureAtlas;
	private BitmapTextureAtlas sheetBitmapTextureAtlas;
	private TextureRegion mProjectileTextureRegion;
	private TextureRegion mPausedTextureRegion;
	private TextureRegion mWinTextureRegion;
	private TextureRegion mFailTextureRegion;
	private TiledTextureRegion mTargetTextureRegion;
	private TiledTextureRegion mHeroTextureRegion;

	// the main scene for the game
	private Scene mMainScene;
	public AnimatedSprite hero;

	private BitmapTextureAtlas mAutoParallaxBackgroundTexture;
	private TextureRegion mParallaxLayer;

	// win/fail sprites
	private Sprite winSprite;
	private Sprite failSprite;

	// our object pools
	ProjectilesPool pPool;
	TargetsPool tPool;

	private LinkedList<Sprite> projectileLL;
	private LinkedList<AnimatedSprite> targetLL;
	private LinkedList<Sprite> projectilesToBeAdded;
	private LinkedList<AnimatedSprite> TargetsToBeAdded;
	private Sound shootingSound;
	private Music backgroundMusic;
	private boolean runningFlag = false;
	private boolean pauseFlag = false;
	private CameraScene mPauseScene;
	private CameraScene mResultScene;
	private int hitCount;
	private final int maxScore = 10;

	@Override
	public Engine onLoadEngine() {

		// getting the device's screen size
		final Display display = getWindowManager().getDefaultDisplay();
		int cameraWidth = display.getWidth();
		int cameraHeight = display.getHeight();

		// setting up the camera [AndEngine's camera , not the one you take
		// pictures with]
		mCamera = new Camera(0, 0, cameraWidth, cameraHeight);
		final Engine mEngine = new Engine(new EngineOptions(true,
				ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(
						cameraWidth, cameraHeight), mCamera)
				.setNeedsSound(true).setNeedsMusic(true));

		// enabling MultiTouch if available
		try {
			if (MultiTouch.isSupported(this)) {
				mEngine.setTouchController(new MultiTouchController());
			} else {
				Toast.makeText(
						this,
						"Sorry your device does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)",
						Toast.LENGTH_LONG).show();
			}
		} catch (final MultiTouchException e) {
			Toast.makeText(
					this,
					"Sorry your Android Version does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)",
					Toast.LENGTH_LONG).show();
		}

		return mEngine;
	}

	@Override
	public void onLoadResources() {

		mAutoParallaxBackgroundTexture = new BitmapTextureAtlas(1024, 1024,
				TextureOptions.DEFAULT);

		// prepare a container for the image
		mBitmapTextureAtlas = new BitmapTextureAtlas(512, 512,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		// prepare a container for the font
		mFontTexture = new BitmapTextureAtlas(256, 256,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);

		sheetBitmapTextureAtlas = new BitmapTextureAtlas(2048, 512);

		// setting assets path for easy access
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		// loading the image inside the container

		mParallaxLayer = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mAutoParallaxBackgroundTexture, this,
						"background.png", 0, 0);

		mProjectileTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mBitmapTextureAtlas, this,
						"projectile.png", 64, 0);

		mHeroTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createTiledFromAsset(sheetBitmapTextureAtlas, this,
						"hero.png", 0, 212, 11, 1);

		mTargetTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createTiledFromAsset(sheetBitmapTextureAtlas, this,
						"target.png", 0, 0, 3, 1);

		mPausedTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mBitmapTextureAtlas, this, "paused.png",
						0, 64);
		mWinTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mBitmapTextureAtlas, this, "win.png", 0,
						128);
		mFailTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mBitmapTextureAtlas, this, "fail.png", 0,
						256);

		pPool = new ProjectilesPool(mProjectileTextureRegion);
		tPool = new TargetsPool(mTargetTextureRegion);

		// preparing the font
		mFont = new Font(mFontTexture, Typeface.create(Typeface.DEFAULT,
				Typeface.BOLD), 40, true, Color.BLACK);

		// loading textures in the engine
		mEngine.getTextureManager().loadTexture(mBitmapTextureAtlas);
		mEngine.getTextureManager().loadTexture(mFontTexture);
		mEngine.getTextureManager().loadTexture(sheetBitmapTextureAtlas);
		mEngine.getTextureManager().loadTexture(mAutoParallaxBackgroundTexture);
		mEngine.getFontManager().loadFont(mFont);

		SoundFactory.setAssetBasePath("mfx/");
		try {
			shootingSound = SoundFactory.createSoundFromAsset(
					mEngine.getSoundManager(), this, "pew_pew_lei.wav");
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MusicFactory.setAssetBasePath("mfx/");

		try {
			backgroundMusic = MusicFactory.createMusicFromAsset(
					mEngine.getMusicManager(), this, "background_music.wav");
			backgroundMusic.setLooping(true);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public Scene onLoadScene() {
		mEngine.registerUpdateHandler(new FPSLogger());

		// creating a new scene for the pause menu
		mPauseScene = new CameraScene(mCamera);
		/* Make the label centered on the camera. */
		final int x = (int) (mCamera.getWidth() / 2 - mPausedTextureRegion
				.getWidth() / 2);
		final int y = (int) (mCamera.getHeight() / 2 - mPausedTextureRegion
				.getHeight() / 2);
		final Sprite pausedSprite = new Sprite(x, y, mPausedTextureRegion);
		mPauseScene.attachChild(pausedSprite);
		// makes the scene transparent
		mPauseScene.setBackgroundEnabled(false);

		// the results scene, for win/fail
		mResultScene = new CameraScene(mCamera);
		winSprite = new Sprite(x, y, mWinTextureRegion);
		failSprite = new Sprite(x, y, mFailTextureRegion);
		mResultScene.attachChild(winSprite);
		mResultScene.attachChild(failSprite);
		// makes the scene transparent
		mResultScene.setBackgroundEnabled(false);

		winSprite.setVisible(false);
		failSprite.setVisible(false);

		// set background color
		mMainScene = new Scene();

		// background preperations
		final AutoParallaxBackground autoParallaxBackground = new AutoParallaxBackground(
				0, 0, 0, 10);

		autoParallaxBackground
				.attachParallaxEntity(new ParallaxEntity(-25.0f, new Sprite(0,
						mCamera.getHeight() - this.mParallaxLayer.getHeight(),
						this.mParallaxLayer)));
		mMainScene.setBackground(autoParallaxBackground);
		mMainScene.setOnSceneTouchListener(this);

		// set coordinates for the player
		final int PlayerX = (mHeroTextureRegion.getWidth() / 20);
		final int PlayerY = (int) ((mCamera.getHeight() - mHeroTextureRegion
				.getHeight()) / 2);

		// set the player on the scene
		hero = new AnimatedSprite(PlayerX, PlayerY, mHeroTextureRegion) {
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
					float pTouchAreaLocalX, float pTouchAreaLocalY) {
				this.setPosition(this.getX(),
						pSceneTouchEvent.getY() - this.getHeight() / 2);

				return true;
			}
		};

		mMainScene.registerTouchArea(hero);
		mMainScene.setTouchAreaBindingEnabled(true);

		// initializing variables
		projectileLL = new LinkedList<Sprite>();
		targetLL = new LinkedList<AnimatedSprite>();
		projectilesToBeAdded = new LinkedList<Sprite>();
		TargetsToBeAdded = new LinkedList<AnimatedSprite>();

		// settings score to the value of the max score to make sure it appears
		// correctly on the screen
		score = new ChangeableText(0, 0, mFont, String.valueOf(maxScore));
		// repositioning the score later so we can use the score.getWidth()
		score.setPosition(mCamera.getWidth() - score.getWidth() - 5, 5);

		createSpriteSpawnTimeHandler();
		mMainScene.registerUpdateHandler(detect);

		// starting background music
		backgroundMusic.play();
		// runningFlag = true;

		restart();
		return mMainScene;
	}

	@Override
	public void onLoadComplete() {
	}

	/** TimerHandler for collision detection and cleaning up */
	IUpdateHandler detect = new IUpdateHandler() {
		@Override
		public void reset() {
		}

		@Override
		public void onUpdate(float pSecondsElapsed) {

			Iterator<AnimatedSprite> targets = targetLL.iterator();
			AnimatedSprite _target;
			boolean hit = false;

			// iterating over the targets
			while (targets.hasNext()) {
				_target = targets.next();

				// if target passed the left edge of the screen, then remove it
				// and call a fail
				if (_target.getX() <= -_target.getWidth()) {
					// removeSprite(_target, targets);
					tPool.recyclePoolItem(_target);
					targets.remove();
					// fail();
					break;
				}
				Iterator<Sprite> projectiles = projectileLL.iterator();
				Sprite _projectile;
				// iterating over all the projectiles (bullets)
				while (projectiles.hasNext()) {
					_projectile = projectiles.next();

					// in case the projectile left the screen
					if (_projectile.getX() >= mCamera.getWidth()
							|| _projectile.getY() >= mCamera.getHeight()
									+ _projectile.getHeight()
							|| _projectile.getY() <= -_projectile.getHeight()) {
						pPool.recyclePoolItem(_projectile);
						projectiles.remove();
						continue;
					}

					// if the targets collides with a projectile, remove the
					// projectile and set the hit flag to true
					if (_target.collidesWith(_projectile)) {
						pPool.recyclePoolItem(_projectile);
						projectiles.remove();
						hit = true;
						break;
					}
				}

				// if a projectile hit the target, remove the target, increment
				// the hit count, and update the score
				if (hit) {
					// removeSprite(_target, targets);
					tPool.recyclePoolItem(_target);
					targets.remove();
					hit = false;
					hitCount++;
					score.setText(String.valueOf(hitCount));
				}
			}

			// if max score , then we are done
			if (hitCount >= maxScore) {
				win();
			}

			// a work around to avoid ConcurrentAccessException
			projectileLL.addAll(projectilesToBeAdded);
			projectilesToBeAdded.clear();

			targetLL.addAll(TargetsToBeAdded);
			TargetsToBeAdded.clear();

		}
	};

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {

		// if the user tapped the screen
		if (pSceneTouchEvent.getAction() == TouchEvent.ACTION_DOWN) {
			final float touchX = pSceneTouchEvent.getX();
			final float touchY = pSceneTouchEvent.getY();
			shootProjectile(touchX, touchY);
			return true;
		}
		return false;
	}

	/** shoots a projectile from the player's position along the touched area */
	private void shootProjectile(final float pX, final float pY) {

		if (!CoolDown.sharedCoolDown().checkValidity()) {
			return;
		}

		int offX = (int) (pX - hero.getX());
		int offY = (int) (pY - hero.getY());
		if (offX <= 0)
			return;

		final Sprite projectile;
		// position the projectile on the player
		projectile = pPool.obtainPoolItem();
		projectile.setPosition(hero.getX() + hero.getWidth(), hero.getY());

		int realX = (int) (mCamera.getWidth() + projectile.getWidth() / 2.0f);
		float ratio = (float) offY / (float) offX;
		int realY = (int) ((realX * ratio) + projectile.getY());

		int offRealX = (int) (realX - projectile.getX());
		int offRealY = (int) (realY - projectile.getY());
		float length = (float) Math.sqrt((offRealX * offRealX)
				+ (offRealY * offRealY));
		float velocity = 480.0f / 1.0f; // 480 pixels / 1 sec
		float realMoveDuration = length / velocity;

		// defining a moveBymodifier from the projectile's position to the
		// calculated one
		MoveByModifier movMByod = new MoveByModifier(realMoveDuration, realX,
				realY);
		LoopEntityModifier loopMod = new LoopEntityModifier(
				new RotationModifier(0.5f, 0, -360));

		final ParallelEntityModifier par = new ParallelEntityModifier(movMByod,
				loopMod);

		DelayModifier dMod = new DelayModifier(0.55f);
		dMod.addModifierListener(new IModifierListener<IEntity>() {

			@Override
			public void onModifierStarted(IModifier<IEntity> arg0, IEntity arg1) {
			}

			@Override
			public void onModifierFinished(IModifier<IEntity> arg0, IEntity arg1) {
				// TODO Auto-generated method stub
				shootingSound.play();
				projectile.setVisible(true);
				projectile.setPosition(hero.getX() + hero.getWidth(),
						hero.getY() + hero.getHeight() / 3);
				projectilesToBeAdded.add(projectile);
			}
		});

		SequenceEntityModifier seq = new SequenceEntityModifier(dMod, par);
		projectile.registerEntityModifier(seq);
		projectile.setVisible(false);
		mMainScene.attachChild(projectile, 1);

		hero.animate(50, false);
	}

	/** adds a target at a random location and let it move along the x-axis */
	public void addTarget() {
		Random rand = new Random();

		int x = (int) mCamera.getWidth() + mTargetTextureRegion.getWidth();
		int minY = mTargetTextureRegion.getHeight();
		int maxY = (int) (mCamera.getHeight() - mTargetTextureRegion
				.getHeight());
		int rangeY = maxY - minY;
		int y = rand.nextInt(rangeY) + minY;

		AnimatedSprite target;
		target = tPool.obtainPoolItem();
		target.setPosition(x, y);
		target.animate(300);
		mMainScene.attachChild(target);

		int minDuration = 2;
		int maxDuration = 4;
		int rangeDuration = maxDuration - minDuration;
		int actualDuration = rand.nextInt(rangeDuration) + minDuration;

		MoveXModifier mod = new MoveXModifier(actualDuration, target.getX(),
				-target.getWidth());
		target.registerEntityModifier(mod.deepCopy());

		TargetsToBeAdded.add(target);

	}

	/** a Time Handler for spawning targets, triggers every 1 second */
	private void createSpriteSpawnTimeHandler() {
		TimerHandler spriteTimerHandler;
		float mEffectSpawnDelay = 1f;

		spriteTimerHandler = new TimerHandler(mEffectSpawnDelay, true,
				new ITimerCallback() {

					@Override
					public void onTimePassed(TimerHandler pTimerHandler) {

						addTarget();
					}
				});

		getEngine().registerUpdateHandler(spriteTimerHandler);
	}

	/** to restart the game and clear the whole screen */
	public void restart() {

		runOnUpdateThread(new Runnable() {

			@Override
			// to safely detach and re-attach the sprites
			public void run() {
				mMainScene.detachChildren();
				mMainScene.attachChild(hero, 0);
				mMainScene.attachChild(score);
			}
		});

		// resetting everything
		hitCount = 0;
		score.setText(String.valueOf(hitCount));
		projectileLL.clear();
		projectilesToBeAdded.clear();
		TargetsToBeAdded.clear();
		targetLL.clear();
	}

	@Override
	// pauses the music and the game when the game goes to the background
	protected void onPause() {
		if (runningFlag) {
			pauseMusic();
			if (mEngine.isRunning()) {
				pauseGame();
				pauseFlag = true;
			}
		}
		super.onPause();
	}

	@Override
	public void onResumeGame() {
		super.onResumeGame();
		// shows this Toast when coming back to the game
		if (runningFlag) {
			if (pauseFlag) {
				pauseFlag = false;
				Toast.makeText(this, "Menu button to resume",
						Toast.LENGTH_SHORT).show();
			} else {
				// in case the user clicks the home button while the game on the
				// resultScene
				resumeMusic();
				mEngine.stop();
			}
		} else {
			runningFlag = true;
		}
	}

	public void pauseMusic() {
		if (runningFlag)
			if (backgroundMusic.isPlaying())
				backgroundMusic.pause();
	}

	public void resumeMusic() {
		if (runningFlag)
			if (!backgroundMusic.isPlaying())
				backgroundMusic.resume();
	}

	public void fail() {
		if (mEngine.isRunning()) {
			winSprite.setVisible(false);
			failSprite.setVisible(true);
			mMainScene.setChildScene(mResultScene, false, true, true);
			mEngine.stop();
		}
	}

	public void win() {
		if (mEngine.isRunning()) {
			failSprite.setVisible(false);
			winSprite.setVisible(true);
			mMainScene.setChildScene(mResultScene, false, true, true);
			mEngine.stop();
		}
	}

	public void pauseGame() {
		if (runningFlag) {
			mMainScene.setChildScene(mPauseScene, false, true, true);
			mEngine.stop();
		}
	}

	public void unPauseGame() {
		mMainScene.clearChildScene();
	}

	@Override
	public boolean onKeyDown(final int pKeyCode, final KeyEvent pEvent) {
		// if menu button is pressed
		if (pKeyCode == KeyEvent.KEYCODE_MENU
				&& pEvent.getAction() == KeyEvent.ACTION_DOWN) {
			if (mEngine.isRunning() && backgroundMusic.isPlaying()) {
				pauseMusic();
				pauseFlag = true;
				pauseGame();
				Toast.makeText(this, "Menu button to resume",
						Toast.LENGTH_SHORT).show();
			} else {
				if (!backgroundMusic.isPlaying()) {
					unPauseGame();
					pauseFlag = false;
					resumeMusic();
					mEngine.start();
				}
				return true;
			}
			// if back key was pressed
		} else if (pKeyCode == KeyEvent.KEYCODE_BACK
				&& pEvent.getAction() == KeyEvent.ACTION_DOWN) {

			if (!mEngine.isRunning() && backgroundMusic.isPlaying()) {
				mMainScene.clearChildScene();
				mEngine.start();
				restart();
				return true;
			}
			return super.onKeyDown(pKeyCode, pEvent);
		}
		return super.onKeyDown(pKeyCode, pEvent);
	}
}