package com.example.andenginerunner;

import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.util.pool.GenericPool;

public class TargetsPool extends GenericPool<AnimatedSprite> {

	private TiledTextureRegion mTextureRegion;

	public TargetsPool(TiledTextureRegion mTargetTextureRegion) {
		if (mTargetTextureRegion == null) {
			throw new IllegalArgumentException(
					"The texture region must not be NULL");
		}
		mTextureRegion = mTargetTextureRegion;
	}

	@Override
	protected AnimatedSprite onAllocatePoolItem() {
		return new AnimatedSprite(0, 0, mTextureRegion.deepCopy());
	}

	protected void onHandleRecycleItem(final AnimatedSprite target) {
		target.clearEntityModifiers();
		target.clearUpdateHandlers();
		target.setVisible(false);
		target.detachSelf();
		target.reset();
	}

}