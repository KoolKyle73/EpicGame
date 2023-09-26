package forrest.epic.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.Game;

import java.util.Iterator;

public class MyGdxGame extends Game {
    public BitmapFont font;
    private final static int PROJECTILE_SPEED = 5;
    private final static int PLAYER_SPEED = 2;
    private Sprite playerSprite;
    private Sprite zombieSprite;
    private Sprite bloodSprite;
    private Texture projectileSprite;
    private Player player;
    private Array<Rectangle> zombies;
    private Array<Projectile> projectiles;
    private Array<Rectangle> bloods;
    private Sound shot;
    private Sound death;
    private Sound zombieSpawn;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private long lastFireTime;
    private long lastSpawnTime;
    private int score = 0;

    @Override
    public void create() {
        playerSprite = new Sprite(new Texture(Gdx.files.internal("playerSprite.png")));
        zombieSprite = new Sprite(new Texture(Gdx.files.internal("zombieSprite.png")));
        bloodSprite = new Sprite(new Texture(Gdx.files.internal("blood.png")));
        projectileSprite = new Texture(Gdx.files.internal("projectile.png"));
        shot = Gdx.audio.newSound(Gdx.files.internal("shot.mp3"));
        death = Gdx.audio.newSound(Gdx.files.internal("death.mp3"));
        zombieSpawn = Gdx.audio.newSound(Gdx.files.internal("zombie.mp3"));

        font = new BitmapFont();

        playerSprite.setCenter(playerSprite.getX() + 32/2, playerSprite.getY() + 32/2);
        zombieSprite.setCenter(zombieSprite.getX() + 32/2, zombieSprite.getY() + 32/2);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        batch = new SpriteBatch();

        player = new Player();
        player.x = 800 / 2;
        player.y = 480 / 2;
        player.z = 0;
        player.width = 32;
        player.height = 32;

        projectiles = new Array<Projectile>();
        zombies = new Array<Rectangle>();
        bloods = new Array<Rectangle>();
    }

    private boolean isOnScreen(float x, float y) {
        return (x < 800 && x > 0) && (y < 480 && y > 0);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, .5f, 0, 1);

        camera.update();

        playerSprite.setX(player.x);
        playerSprite.setY(player.y);
        playerSprite.setScale(player.width / 32, player.height / 32);

        batch.begin();

        for(Rectangle blood: bloods) {
            bloodSprite.setX(blood.x);
            bloodSprite.setY(blood.y);

            bloodSprite.draw(batch);
        }

        playerSprite.draw(batch);

        for (Iterator<Projectile> iter = projectiles.iterator(); iter.hasNext(); ) {
            Projectile projectile = iter.next();

            projectile.x += (float) Math.cos(projectile.trajectory) * PROJECTILE_SPEED;
            projectile.y += (float) Math.sin(projectile.trajectory) * PROJECTILE_SPEED;

            if (!isOnScreen(projectile.x, projectile.y)) {
                iter.remove();
            } else {
                batch.draw(projectileSprite, projectile.x, projectile.y);
            }

            for (Iterator<Rectangle> iterz = zombies.iterator(); iterz.hasNext(); ) {
                Rectangle zombie = iterz.next();

                if (projectile.overlaps(zombie)){
                    iterz.remove();

                    score += 1;

                    death.play();
                    spawnBlood(zombie.x, zombie.y);
                }
            }
        }

        for (Iterator<Rectangle> iter = zombies.iterator(); iter.hasNext(); ) {
            Rectangle zombie = iter.next();

            //Set position
            Vector2 zombiePos = new Vector2(zombie.getPosition(new Vector2()));
            Vector2 playerPos = new Vector2(player.getPosition(new Vector2()));

            Vector2 dir = new Vector2();

            dir.x = playerPos.x - zombiePos.x;
            dir.y = playerPos.y - zombiePos.y;

            dir.nor();

            zombie.x += dir.x * (PLAYER_SPEED - .5);
            zombie.y += dir.y * (PLAYER_SPEED - .5);

            zombieSprite.setX(zombie.x);
            zombieSprite.setY(zombie.y);

            //Set rotation
            float dx = player.x - zombie.x;
            float dy = player.y - zombie.y;

            double angle = MathUtils.atan2(dy, dx) * 180 / Math.PI;

            zombieSprite.setRotation((float) angle);

            zombieSprite.draw(batch);
        }


        font.draw(batch, Integer.toString(score) + " Kills", 750, 450);
        batch.end();

        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        Vector2 playerPos = new Vector2(player.getPosition(new Vector2()));

        camera.unproject(mousePos);

        Vector2 dir = new Vector2();

        dir.x = mousePos.x - playerPos.x;
        dir.y = mousePos.y - playerPos.y;

        dir.nor();

        double angle = MathUtils.atan2(dir.y, dir.x) * 180 / Math.PI;

        playerSprite.setRotation((float) angle);

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            player.y += 50 * PLAYER_SPEED * Gdx.graphics.getDeltaTime();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            player.y -= 50 * PLAYER_SPEED * Gdx.graphics.getDeltaTime();
        }


        if (Gdx.input.isKeyPressed(Input.Keys.A)){
            player.x -= 50 * PLAYER_SPEED * Gdx.graphics.getDeltaTime();
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            player.x += 50 * PLAYER_SPEED * Gdx.graphics.getDeltaTime();
        }

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            if (lastFireTime == 0 || TimeUtils.nanoTime() - lastFireTime > 300000000) {
                spawnProjectile();
            }
        }

        if (lastSpawnTime == 0 || TimeUtils.nanoTime() - lastSpawnTime > 700000000f) {
            spawnZombie();
        }
    }

    private void spawnProjectile() {
        shot.play();

        Projectile projectile = new Projectile();
        projectile.x = player.x;
        projectile.y = player.y;
        projectile.width = 8;
        projectile.height = 8;
        projectiles.add(projectile);

        double rot = playerSprite.getRotation() * Math.PI / 180;

        projectile.trajectory = rot + (MathUtils.random(-0.05f, 0.05f));

        lastFireTime = TimeUtils.nanoTime();
    }

    private void spawnZombie() {
        Rectangle zombie = new Rectangle();
        zombie.x = MathUtils.random(0, 800);
        zombie.y = MathUtils.random(0, 480);
        zombie.width = 32;
        zombie.height = 32;
        zombies.add(zombie);

        zombieSpawn.play();
        lastSpawnTime = TimeUtils.nanoTime();
    }

    private void spawnBlood(float x, float y) {
        Rectangle blood = new Rectangle();

        blood.x = x;
        blood.y = y;
        blood.height = 32;
        blood.width = 32;
        bloods.add(blood);
    }

    @Override
    public void dispose() {

    }
}
