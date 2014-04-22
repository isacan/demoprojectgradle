package com.eziktek.demoproject;

import sun.font.TrueTypeFont;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.eziktek.demoproject.Game.Koala.State;


public class Game implements ApplicationListener
{

	static class Koala
	{
		static float WIDTH;
		static float HEIGHT;
		static float MAX_VELOCITY = 10f;
		static float JUMP_VELOCITY = 40f;
		static float DAMPING = 0.87f;

		enum State
		{
			Standing, Walking, Jumping
		}

		final Vector2 position = new Vector2();
		final Vector2 velocity = new Vector2();
		State state = State.Walking;
		float stateTime = 0;
		boolean facesRight = true;
		boolean grounded = false;
	}

	private TiledMap map;
	private OrthogonalTiledMapRenderer renderer;
	private OrthographicCamera camera;
	private Texture koalaTexture;
	private Animation stand;
	private Animation walk;
	private Animation jump;
	private Koala koala;
	private Pool<Rectangle> rectPool = new Pool<Rectangle>()
	{
		@Override
		protected Rectangle newObject()
		{
			return new Rectangle();
		}
	};
	private Array<Rectangle> tiles = new Array<Rectangle>();

	private static final float GRAVITY = -2.5f;
	
	private SpriteBatch batch;
	private Stage stage;
	private BitmapFont font;
	private TextureAtlas buttonsAtlas; 
	private Skin buttonSkin; 
	private Button buttonLeft,buttonRight,buttonJump,buttonFire;
	private boolean left,right,jumped,fire;
	private static final int BULLET_SIZE = 1;
	private Vector2 bullet_pos,bulletDirection;
	private Pixmap pixmap;
	int screenWidth, screenHeight;

	@Override
	public void create()
	{

		koalaTexture = new Texture("data/koalio.png");
		TextureRegion[] regions = TextureRegion.split(koalaTexture, 18, 26)[0];
		stand = new Animation(0, regions[0]);
		jump = new Animation(0, regions[1]);
		walk = new Animation(0.15f, regions[2], regions[3], regions[4]);
		walk.setPlayMode(Animation.LOOP_PINGPONG);


		Koala.WIDTH = 1 / 16f * regions[0].getRegionWidth();
		Koala.HEIGHT = 1 / 16f * regions[0].getRegionHeight();


		map = new TmxMapLoader().load("data/level1.tmx");
		renderer = new OrthogonalTiledMapRenderer(map, 1 / 16f);

		camera = new OrthographicCamera();
		camera.setToOrtho(false, 30, 20);
		camera.update();

		koala = new Koala();
		koala.position.set(20, 20);
		
		
		batch=new SpriteBatch();
		buttonsAtlas=new TextureAtlas("data/buttons.pack");
		buttonSkin=new Skin();
		buttonSkin.addRegions(buttonsAtlas);
		font = new BitmapFont(Gdx.files.internal("data/myfont.fnt"),false);
		
		
		bullet_pos=null;
		bulletDirection=new Vector2(1, 0);
		pixmap=new Pixmap(32, 32, Pixmap.Format.RGB565);
		
		stage=new Stage(800, 480, true);
		stage.clear();
		Gdx.input.setInputProcessor(stage);
		
		TextButtonStyle styleLeft=new TextButtonStyle(),styleRight=new TextButtonStyle(),styleJump=new TextButtonStyle(),styleFire=new TextButtonStyle();
		styleLeft.up=buttonSkin.getDrawable("left_p");
		styleLeft.down=buttonSkin.getDrawable("left");
		styleLeft.font=font;
		
		styleRight.up=buttonSkin.getDrawable("right_p");
		styleRight.down=buttonSkin.getDrawable("right");
		styleRight.font=font;
		
		styleJump.up=buttonSkin.getDrawable("jump_p");
		styleJump.down=buttonSkin.getDrawable("jump");
		styleJump.font=font;
		
		styleFire.up=buttonSkin.getDrawable("fire");
		styleFire.down=buttonSkin.getDrawable("fire");
		styleFire.font=font;
		
		buttonLeft=new Button(styleLeft);
		buttonLeft.setPosition(25, -25);
		buttonLeft.setHeight(100);
		buttonLeft.setWidth(100);
		buttonLeft.addListener(buttonLeftListener);
		
		buttonRight=new Button(styleRight);
		buttonRight.setPosition(100,-25);
		buttonRight.setHeight(100);
		buttonRight.setWidth(100);
		buttonRight.addListener(buttonRightListener);
		
		buttonJump=new Button(styleJump);
		buttonJump.setPosition(600, -20);
		buttonJump.setHeight(100);
		buttonJump.setWidth(100);
		buttonJump.addListener(buttonJumpListener);
		
		buttonFire=new Button(styleFire);
		buttonFire.setPosition(700, -10);
		buttonFire.setHeight(80);
		buttonFire.setWidth(80);
		buttonFire.addListener(buttonFireListener);
		
		stage.addActor(buttonLeft);
		stage.addActor(buttonRight);
		stage.addActor(buttonJump);
		//stage.addActor(buttonFire);
		
	}

	@Override
	public void render()
	{

		Gdx.gl.glClearColor(0.7f, 0.7f, 1.0f, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		// get the delta time
		float deltaTime = Gdx.graphics.getDeltaTime();

		
		updateKoala(deltaTime);


		renderer.setView(camera);
		renderer.render();

		renderKoala(deltaTime);
		
		stage.act();
		
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		stage.draw();
		batch.end();
		
	}
	
	InputListener buttonLeftListener=new InputListener(){
		
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			
			
			left=true;
			
			
			return true;
		}
		
		

		@Override
		public void touchUp(InputEvent event, float x, float y,
				int pointer, int button) {
			left=false;
		}
		
		
	};
		
	InputListener buttonRightListener=new InputListener(){
		
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			
			right=true;
	
			return true;
		}
		
		

		@Override
		public void touchUp(InputEvent event, float x, float y,
				int pointer, int button) {
			right=false;
		}
		
		
	};
	
	InputListener buttonJumpListener=new InputListener(){
		
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			
			jumped=true;
			
			return true;
		}
		
		

		@Override
		public void touchUp(InputEvent event, float x, float y,
				int pointer, int button) {
			
			jumped=false;
			
		}
		
		
	};
	
	InputListener buttonFireListener=new InputListener(){
		
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			
			Vector2 direction = new Vector2(0, 0);
			float delta = Gdx.graphics.getDeltaTime() * koala.MAX_VELOCITY;
			
			if(koala.facesRight){
				direction.x = 1 * delta;
			}
			else{
				direction.x = -1 * delta;
			}
			
			bullet_pos=new Vector2(koala.position.cpy().add(
					koala.WIDTH / 2 - BULLET_SIZE / 2,
					koala.HEIGHT / 2 - BULLET_SIZE / 2));
			
			bulletDirection.set(direction);
			
			return true;
		}
		
		

		@Override
		public void touchUp(InputEvent event, float x, float y,
				int pointer, int button) {

			
		}
		
		
	};
	

	private Vector2 tmp = new Vector2();

	private void updateKoala(float deltaTime)
	{
		if (deltaTime == 0)
			return;
		koala.stateTime += deltaTime;


		if ((Gdx.input.isKeyPressed(Keys.SPACE) || jumped ) && koala.grounded) //isTouched(0.75f, 1)
		{
			koala.velocity.y += Koala.JUMP_VELOCITY;
			koala.state = Koala.State.Jumping;
			koala.grounded = false;
		}

		if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A) || left) //|| isTouched(0, 0.25f)
		{
			koala.velocity.x = -Koala.MAX_VELOCITY;
			if (koala.grounded)
				koala.state = Koala.State.Walking;
			koala.facesRight = false;
		}

		if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D) || right ) //|| isTouched(0.25f, 0.5f)
		{
			koala.velocity.x = Koala.MAX_VELOCITY;
			if (koala.grounded)
				koala.state = Koala.State.Walking;
			koala.facesRight = true;
		}


		koala.velocity.add(0, GRAVITY);


		if (Math.abs(koala.velocity.x) > Koala.MAX_VELOCITY)
		{
			koala.velocity.x = Math.signum(koala.velocity.x) * Koala.MAX_VELOCITY;
		}


		if (Math.abs(koala.velocity.x) < 1)
		{
			koala.velocity.x = 0;
			if (koala.grounded)
				koala.state = Koala.State.Standing;
		}

	
		koala.velocity.mul(deltaTime);


		Rectangle koalaRect = rectPool.obtain();
		koalaRect.set(koala.position.x, koala.position.y, Koala.WIDTH, Koala.HEIGHT);
		int startX, startY, endX, endY;
		if (koala.velocity.x > 0)
		{
			startX = endX = (int) (koala.position.x + Koala.WIDTH + koala.velocity.x);
		}
		else
		{
			startX = endX = (int) (koala.position.x + koala.velocity.x);
		}
		startY = (int) (koala.position.y);
		endY = (int) (koala.position.y + Koala.HEIGHT);
		getTiles(startX, startY, endX, endY, tiles);
		koalaRect.x += koala.velocity.x;
		for (Rectangle tile : tiles)
		{
			if (koalaRect.overlaps(tile))
			{
				koala.velocity.x = 0;
				break;
			}
		}
		koalaRect.x = koala.position.x;

		if (koala.velocity.y > 0)
		{
			startY = endY = (int) (koala.position.y + Koala.HEIGHT + koala.velocity.y);
		}
		else
		{
			startY = endY = (int) (koala.position.y + koala.velocity.y);
		}
		startX = (int) (koala.position.x);
		endX = (int) (koala.position.x + Koala.WIDTH);
		getTiles(startX, startY, endX, endY, tiles);
		koalaRect.y += koala.velocity.y;
		for (Rectangle tile : tiles)
		{
			if (koalaRect.overlaps(tile))
			{

				if (koala.velocity.y > 0)
				{
					koala.position.y = tile.y - Koala.HEIGHT;
		
					TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(1);
					layer.setCell((int) tile.x, (int) tile.y, null);
				}
				else
				{
					koala.position.y = tile.y + tile.height;

					koala.grounded = true;
				}
				koala.velocity.y = 0;
				break;
			}
		}
		rectPool.free(koalaRect);


		koala.position.add(koala.velocity);
		koala.velocity.mul(1 / deltaTime);

		koala.velocity.x *= Koala.DAMPING;

		if(bullet_pos!=null){
			
			if(koala.state==State.Standing){
			
			if(koala.facesRight)
				bullet_pos.x+=deltaTime*2;
			else
				bullet_pos.x-=deltaTime*2;
			
			}
			else{
				
				
			}
		}
		
		
	}

	private boolean isTouched(float startX, float endX)
	{

		for (int i = 0; i < 2; i++)
		{
			float x = Gdx.input.getX() / (float) Gdx.graphics.getWidth();
			if (Gdx.input.isTouched(i) && (x >= startX && x <= endX))
			{
				return true;
			}
		}
		return false;
	}

	private void getTiles(int startX, int startY, int endX, int endY, Array<Rectangle> tiles)
	{
		TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(0);
		rectPool.freeAll(tiles);
		tiles.clear();
		for (int y = startY; y <= endY; y++)
		{
			for (int x = startX; x <= endX; x++)
			{
				Cell cell = layer.getCell(x, y);
				if (cell != null)
				{
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);
				}
			}
		}
	}

	private void renderKoala(float deltaTime)
	{

		TextureRegion frame = null;
		switch (koala.state)
		{
			case Standing:
				frame = stand.getKeyFrame(koala.stateTime);
				break;
			case Walking:
				frame = walk.getKeyFrame(koala.stateTime);
				break;
			case Jumping:
				frame = jump.getKeyFrame(koala.stateTime);
				break;
		}


		SpriteBatch batch = renderer.getSpriteBatch();
		batch.begin();
		
		if (koala.facesRight)
		{
			batch.draw(frame, koala.position.x, koala.position.y, Koala.WIDTH, Koala.HEIGHT);
		}
		else
		{
			batch.draw(frame, koala.position.x + Koala.WIDTH, koala.position.y, -Koala.WIDTH, Koala.HEIGHT);
		}
		if(bullet_pos!=null){
			pixmap.drawRectangle(0, 0, BULLET_SIZE, BULLET_SIZE);
			batch.draw(new Texture(pixmap), bullet_pos.x, bullet_pos.y,
					BULLET_SIZE, BULLET_SIZE);
		}
		batch.end();
	}

	@Override
	public void dispose()
	{
	}

	@Override
	public void resize(int width, int height)
	{


	}

	@Override
	public void pause()
	{


	}

	@Override
	public void resume()
	{
		
	}
}