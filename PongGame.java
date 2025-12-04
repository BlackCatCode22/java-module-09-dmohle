import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

// --- MAIN CLASS ---
public class PongGame {
    public static void main(String[] args) {
        GameFrame frame = new GameFrame();
    }
}

// --- GAME FRAME ---
class GameFrame extends JFrame {
    GamePanel panel;

    GameFrame(){
        panel = new GamePanel();
        this.add(panel);
        this.setTitle("Java Pong - Point Messages");
        this.setResizable(false);
        this.setBackground(Color.black);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);
        this.setLocationRelativeTo(null);
    }
}

// --- GAME PANEL ---
class GamePanel extends JPanel implements Runnable {

    static final int GAME_WIDTH = 1000;
    static final int GAME_HEIGHT = (int)(GAME_WIDTH * (0.5555));
    static final Dimension SCREEN_SIZE = new Dimension(GAME_WIDTH, GAME_HEIGHT);
    static final int BALL_DIAMETER = 20;
    static final int PADDLE_WIDTH = 25;
    static final int PADDLE_HEIGHT = 100;

    Thread gameThread;
    Image image;
    Graphics graphics;
    Random random = new Random();
    Paddle paddle1;
    Paddle paddle2;
    Ball ball;
    Score score;

    boolean p1HasAdvantage;
    int restingY;
    String advantageMessage = "";
    Color advantageColor = Color.white;

    // New variables for the Point Message
    boolean showWinMessage = false;
    String lastWinMessage = "";
    Color lastWinColor = Color.white;

    GamePanel(){
        score = new Score(GAME_WIDTH, GAME_HEIGHT);
        newPaddles();
        newBall();
        this.setFocusable(true);
        this.setPreferredSize(SCREEN_SIZE);

        gameThread = new Thread(this);
        gameThread.start();
    }

    public void newBall(){
        ball = new Ball((GAME_WIDTH/2)-(BALL_DIAMETER/2), random.nextInt(GAME_HEIGHT-BALL_DIAMETER), BALL_DIAMETER, BALL_DIAMETER, random);
    }

    public void newPaddles(){
        int centerY = (GAME_HEIGHT/2)-(PADDLE_HEIGHT/2);
        restingY = random.nextInt(GAME_HEIGHT - PADDLE_HEIGHT);

        int baseSpeed = 5;
        int boostSpeed = 15;

        p1HasAdvantage = random.nextBoolean();

        if(p1HasAdvantage) {
            paddle1 = new Paddle(0, centerY, PADDLE_WIDTH, PADDLE_HEIGHT, 1);
            paddle1.speed = boostSpeed;
            paddle2 = new Paddle(GAME_WIDTH-PADDLE_WIDTH, centerY, PADDLE_WIDTH, PADDLE_HEIGHT, 2);
            paddle2.speed = baseSpeed;
            advantageMessage = "P1: WAIT & STRIKE (FAST)";
            advantageColor = new Color(60, 160, 255);
        } else {
            paddle1 = new Paddle(0, centerY, PADDLE_WIDTH, PADDLE_HEIGHT, 1);
            paddle1.speed = baseSpeed;
            paddle2 = new Paddle(GAME_WIDTH-PADDLE_WIDTH, centerY, PADDLE_WIDTH, PADDLE_HEIGHT, 2);
            paddle2.speed = boostSpeed;
            advantageMessage = "P2: WAIT & STRIKE (FAST)";
            advantageColor = new Color(255, 60, 60);
        }
    }

    public void paint(Graphics g) {
        image = createImage(getWidth(), getHeight());
        graphics = image.getGraphics();
        draw(graphics);
        g.drawImage(image,0,0,this);
    }

    public void draw(Graphics g) {
        paddle1.draw(g);
        paddle2.draw(g);
        ball.draw(g);
        score.draw(g);

        // Draw Advantage Text
        g.setFont(new Font("Consolas", Font.BOLD, 20));
        g.setColor(advantageColor);
        FontMetrics metrics = g.getFontMetrics();
        int x = (GAME_WIDTH - metrics.stringWidth(advantageMessage)) / 2;
        g.drawString(advantageMessage, x, 100);

        // Draw Speed Stats
        g.setFont(new Font("Consolas", Font.PLAIN, 15));
        g.setColor(new Color(60, 160, 255));
        g.drawString("SPD: " + paddle1.speed, (GAME_WIDTH/2)-120, 80);
        g.setColor(new Color(255, 60, 60));
        g.drawString("SPD: " + paddle2.speed, (GAME_WIDTH/2)+60, 80);

        // --- DRAW POINT MESSAGE ---
        if(showWinMessage) {
            g.setFont(new Font("Consolas", Font.BOLD, 50));
            g.setColor(lastWinColor);
            metrics = g.getFontMetrics();
            int msgX = (GAME_WIDTH - metrics.stringWidth(lastWinMessage)) / 2;
            int msgY = GAME_HEIGHT / 2 + 100; // Display below center line
            g.drawString(lastWinMessage, msgX, msgY);
        }

        Toolkit.getDefaultToolkit().sync();
    }

    public void move() {
        if(p1HasAdvantage) {
            if(ball.xVelocity > 0 || ball.x > GAME_WIDTH/2) movePaddleTo(paddle1, restingY);
            else movePaddleTo(paddle1, ball.y);
        } else {
            movePaddleTo(paddle1, ball.y);
        }

        if(!p1HasAdvantage) {
            if(ball.xVelocity < 0 || ball.x < GAME_WIDTH/2) movePaddleTo(paddle2, restingY);
            else movePaddleTo(paddle2, ball.y);
        } else {
            movePaddleTo(paddle2, ball.y);
        }

        paddle1.move();
        paddle2.move();
        ball.move();
    }

    public void movePaddleTo(Paddle p, int targetY) {
        int paddleCenter = p.y + (PADDLE_HEIGHT / 2);
        int targetCenter;
        if(isTrackingBall(p)) targetCenter = targetY + (BALL_DIAMETER / 2);
        else targetCenter = targetY + (PADDLE_HEIGHT / 2);

        if(paddleCenter < targetCenter - 15) p.setYDirection(p.speed);
        else if (paddleCenter > targetCenter + 15) p.setYDirection(-p.speed);
        else p.setYDirection(0);
    }

    private boolean isTrackingBall(Paddle p) {
        if(p.id == 1 && p1HasAdvantage && (ball.xVelocity > 0 || ball.x > GAME_WIDTH/2)) return false;
        if(p.id == 2 && !p1HasAdvantage && (ball.xVelocity < 0 || ball.x < GAME_WIDTH/2)) return false;
        return true;
    }

    public void checkCollision() {
        if(ball.y <= 0) ball.setYDirection(-ball.yVelocity);
        if(ball.y >= GAME_HEIGHT-BALL_DIAMETER) ball.setYDirection(-ball.yVelocity);

        if(ball.intersects(paddle1)) {
            handleBounce(paddle1);
            if(p1HasAdvantage) restingY = random.nextInt(GAME_HEIGHT - PADDLE_HEIGHT);
        }

        if(ball.intersects(paddle2)) {
            handleBounce(paddle2);
            if(!p1HasAdvantage) restingY = random.nextInt(GAME_HEIGHT - PADDLE_HEIGHT);
        }

        if(paddle1.y <= 0) paddle1.y = 0;
        if(paddle1.y >= (GAME_HEIGHT-PADDLE_HEIGHT)) paddle1.y = GAME_HEIGHT-PADDLE_HEIGHT;
        if(paddle2.y <= 0) paddle2.y = 0;
        if(paddle2.y >= (GAME_HEIGHT-PADDLE_HEIGHT)) paddle2.y = GAME_HEIGHT-PADDLE_HEIGHT;

        // --- SCORING LOGIC WITH PAUSE ---

        // Player 2 (RED) Scored
        if(ball.x <= 0) {
            score.player2++;
            displayWinMessage("Point - Red!", new Color(255, 60, 60));
            newPaddles();
            newBall();
        }

        // Player 1 (BLUE) Scored
        if(ball.x >= GAME_WIDTH-BALL_DIAMETER) {
            score.player1++;
            displayWinMessage("Point - Blue!", new Color(60, 160, 255));
            newPaddles();
            newBall();
        }
    }

    // New helper to handle the pause logic
    public void displayWinMessage(String message, Color color) {
        lastWinMessage = message;
        lastWinColor = color;
        showWinMessage = true;
        repaint(); // Force one paint so text appears

        try {
            Thread.sleep(2000); // Pause game logic for 2 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        showWinMessage = false;
    }

    public void handleBounce(Paddle p) {
        ball.xVelocity = Math.abs(ball.xVelocity);
        ball.xVelocity++;
        if(ball.yVelocity > 0) ball.yVelocity++;
        else ball.yVelocity--;

        if(p.id == 1) ball.setXDirection(ball.xVelocity);
        else ball.setXDirection(-ball.xVelocity);

        ball.setYDirection(ball.yVelocity);
    }

    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        while(true) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            if(delta >= 1) {
                move();
                checkCollision();
                repaint();
                delta--;
            }
        }
    }
}

// --- PADDLE CLASS ---
class Paddle extends Rectangle{
    int id;
    int yVelocity;
    int speed = 5;

    Paddle(int x, int y, int PADDLE_WIDTH, int PADDLE_HEIGHT, int id){
        super(x, y, PADDLE_WIDTH, PADDLE_HEIGHT);
        this.id = id;
    }

    public void setYDirection(int yDirection) { yVelocity = yDirection; }
    public void move() { y = y + yVelocity; }
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if(id == 1) g2d.setColor(new Color(60, 160, 255));
        else g2d.setColor(new Color(255, 60, 60));
        g2d.fillRect(x, y, width, height);
    }
}

// --- BALL CLASS ---
class Ball extends Rectangle{
    Random random;
    int xVelocity;
    int yVelocity;
    int initialSpeed = 3;

    Ball(int x, int y, int width, int height, Random random){
        super(x, y, width, height);
        this.random = random;
        int randomXDirection = random.nextInt(2);
        if(randomXDirection == 0) randomXDirection--;
        setXDirection(randomXDirection*initialSpeed);

        int randomYDirection = random.nextInt(2);
        if(randomYDirection == 0) randomYDirection--;
        setYDirection(randomYDirection*initialSpeed);
    }

    public void setXDirection(int randomXDirection) { xVelocity = randomXDirection; }
    public void setYDirection(int randomYDirection) { yVelocity = randomYDirection; }
    public void move() { x += xVelocity; y += yVelocity; }
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.white);
        g2d.fillOval(x, y, height, width);
    }
}

// --- SCORE CLASS ---
class Score extends Rectangle{
    static int GAME_WIDTH;
    static int GAME_HEIGHT;
    int player1;
    int player2;

    Score(int GAME_WIDTH, int GAME_HEIGHT){
        Score.GAME_WIDTH = GAME_WIDTH;
        Score.GAME_HEIGHT = GAME_HEIGHT;
    }

    public void draw(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Consolas", Font.PLAIN, 60));
        g.drawLine(GAME_WIDTH/2, 0, GAME_WIDTH/2, GAME_HEIGHT);
        g.drawString(String.valueOf(player1/10) + String.valueOf(player1%10), (GAME_WIDTH/2)-85, 50);
        g.drawString(String.valueOf(player2/10) + String.valueOf(player2%10), (GAME_WIDTH/2)+20, 50);
    }
}