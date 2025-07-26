import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class BubbleGame extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private static final int BUBBLE_SIZE = 40;
    private static final int ROWS = 8;
    private static final int COLS = 15;
    
    private final javax.swing.Timer timer;
    private List<Bubble> bubbles;
    private Bubble shooterBubble;
    private Bubble nextBubble;
    private int mouseX, mouseY;
    private boolean shooting = false;
    private double shootAngle;
    private int score = 0;
    private boolean gameOver = false;
    
    private final Color[] colors = {
        Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, 
        Color.MAGENTA, Color.CYAN, Color.ORANGE
    };
    
    public BubbleGame() {
        setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        
        initGame();
        
        timer = new javax.swing.Timer(16, this); // ~60 FPS
        timer.start();
        
        initializeListeners();
    }
    
    private void initializeListeners() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    
    private void initGame() {
        bubbles = new ArrayList<>();
        Random rand = new Random();
        
        // Create initial bubble grid
        for (int row = 0; row < ROWS / 2; row++) {
            for (int col = 0; col < COLS; col++) {
                if (rand.nextDouble() < 0.7) { // 70% chance for bubble
                    int x = col * BUBBLE_SIZE + (row % 2) * (BUBBLE_SIZE / 2) + BUBBLE_SIZE / 2;
                    int y = row * (BUBBLE_SIZE - 5) + BUBBLE_SIZE / 2;
                    Color color = colors[rand.nextInt(colors.length)];
                    bubbles.add(new Bubble(x, y, color));
                }
            }
        }
        
        // Create shooter bubble
        shooterBubble = new Bubble(GAME_WIDTH / 2, GAME_HEIGHT - 50, colors[new Random().nextInt(colors.length)]);
        shooterBubble.vx = 0;
        shooterBubble.vy = 0;
        
        // Create next bubble
        nextBubble = new Bubble(GAME_WIDTH / 2 + 60, GAME_HEIGHT - 50, colors[new Random().nextInt(colors.length)]);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw bubbles
        for (Bubble bubble : bubbles) {
            bubble.draw(g2d);
        }
        
        // Draw shooter bubble
        if (!shooting) {
            shooterBubble.draw(g2d);
            
            // Draw aiming line
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            int lineLength = 100;
            int endX = (int) (shooterBubble.x + Math.cos(shootAngle) * lineLength);
            int endY = (int) (shooterBubble.y + Math.sin(shootAngle) * lineLength);
            g2d.drawLine((int)shooterBubble.x, (int)shooterBubble.y, endX, endY);
        } else {
            shooterBubble.draw(g2d);
        }
        
        // Draw next bubble
        g2d.setColor(Color.WHITE);
        g2d.drawString("Next:", WIDTH / 2 + 20, HEIGHT - 70);
        nextBubble.draw(g2d);
        
        // Draw score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 20, 30);
        
        // Draw game over
        if (gameOver) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String msg = "Game Over!";
            int x = (WIDTH - fm.stringWidth(msg)) / 2;
            int y = HEIGHT / 2;
            g2d.drawString(msg, x, y);
            
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            fm = g2d.getFontMetrics();
            String restart = "Click to restart";
            x = (WIDTH - fm.stringWidth(restart)) / 2;
            g2d.drawString(restart, x, y + 60);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;
        
        if (shooting) {
            // Move shooter bubble
            shooterBubble.x += shooterBubble.vx;
            shooterBubble.y += shooterBubble.vy;
            
            // Check wall collisions
            if (shooterBubble.x <= BUBBLE_SIZE / 2 || shooterBubble.x >= WIDTH - BUBBLE_SIZE / 2) {
                shooterBubble.vx = -shooterBubble.vx;
            }
            
            // Check collision with other bubbles
            Bubble hitBubble = null;
            for (Bubble bubble : bubbles) {
                if (shooterBubble.collidesWith(bubble)) {
                    hitBubble = bubble;
                    break;
                }
            }
            
            // Check if reached top
            if (shooterBubble.y <= BUBBLE_SIZE / 2 || hitBubble != null) {
                // Snap to grid position
                snapToGrid();
                
                // Check for matches
                List<Bubble> matches = findMatches(shooterBubble);
                if (matches.size() >= 3) {
                    bubbles.removeAll(matches);
                    score += matches.size() * 10;
                    
                    // Remove floating bubbles
                    removeFloatingBubbles();
                }
                
                // Reset for next shot
                shooting = false;
                shooterBubble = nextBubble;
                shooterBubble.x = WIDTH / 2;
                shooterBubble.y = HEIGHT - 50;
                shooterBubble.vx = 0;
                shooterBubble.vy = 0;
                nextBubble = new Bubble(WIDTH / 2 + 60, HEIGHT - 50, 
                    colors[new Random().nextInt(colors.length)]);
                
                // Check game over
                checkGameOver();
            }
        }
        
        repaint();
    }
    
    private void snapToGrid() {
        int row = (int) ((shooterBubble.y - BUBBLE_SIZE / 2) / (BUBBLE_SIZE - 5));
        int col;
        
        if (row % 2 == 0) {
            col = (int) ((shooterBubble.x - BUBBLE_SIZE / 2) / BUBBLE_SIZE);
        } else {
            col = (int) ((shooterBubble.x - BUBBLE_SIZE) / BUBBLE_SIZE);
        }
        
        int x = col * BUBBLE_SIZE + (row % 2) * (BUBBLE_SIZE / 2) + BUBBLE_SIZE / 2;
        int y = row * (BUBBLE_SIZE - 5) + BUBBLE_SIZE / 2;
        
        shooterBubble.x = x;
        shooterBubble.y = y;
        shooterBubble.vx = 0;
        shooterBubble.vy = 0;
        
        bubbles.add(shooterBubble);
    }
    
    private List<Bubble> findMatches(Bubble bubble) {
        List<Bubble> matches = new ArrayList<>();
        List<Bubble> toCheck = new ArrayList<>();
        Set<Bubble> visited = new HashSet<>();
        
        toCheck.add(bubble);
        
        while (!toCheck.isEmpty()) {
            Bubble current = toCheck.remove(0);
            if (visited.contains(current)) continue;
            
            visited.add(current);
            matches.add(current);
            
            for (Bubble other : bubbles) {
                if (!visited.contains(other) && current.isAdjacent(other) && 
                    current.color.equals(other.color)) {
                    toCheck.add(other);
                }
            }
        }
        
        return matches;
    }
    
    private void removeFloatingBubbles() {
        Set<Bubble> connected = new HashSet<>();
        List<Bubble> toCheck = new ArrayList<>();
        
        // Find all bubbles connected to top row
        for (Bubble bubble : bubbles) {
            if (bubble.y <= BUBBLE_SIZE) {
                toCheck.add(bubble);
            }
        }
        
        while (!toCheck.isEmpty()) {
            Bubble current = toCheck.remove(0);
            if (connected.contains(current)) continue;
            
            connected.add(current);
            
            for (Bubble other : bubbles) {
                if (!connected.contains(other) && current.isAdjacent(other)) {
                    toCheck.add(other);
                }
            }
        }
        
        // Remove unconnected bubbles
        List<Bubble> toRemove = new ArrayList<>();
        for (Bubble bubble : bubbles) {
            if (!connected.contains(bubble)) {
                toRemove.add(bubble);
            }
        }
        
        bubbles.removeAll(toRemove);
        score += toRemove.size() * 5;
    }
    
    private void checkGameOver() {
        for (Bubble bubble : bubbles) {
            if (bubble.y >= HEIGHT - 100) {
                gameOver = true;
                break;
            }
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (gameOver) {
            // Restart game
            gameOver = false;
            score = 0;
            shooting = false;
            bubbles.clear();
            initGame();
            return;
        }
        
        if (!shooting) {
            shooting = true;
            double speed = 8;
            shooterBubble.vx = Math.cos(shootAngle) * speed;
            shooterBubble.vy = Math.sin(shootAngle) * speed;
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!shooting) {
            mouseX = e.getX();
            mouseY = e.getY();
            shootAngle = Math.atan2(mouseY - shooterBubble.y, mouseX - shooterBubble.x);
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseDragged(MouseEvent e) {}
    
    // Bubble class
    class Bubble {
        double x, y, vx, vy;
        Color color;
        
        public Bubble(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = 0;
            this.vy = 0;
        }
        
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.fillOval((int)(x - BUBBLE_SIZE/2), (int)(y - BUBBLE_SIZE/2), BUBBLE_SIZE, BUBBLE_SIZE);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawOval((int)(x - BUBBLE_SIZE/2), (int)(y - BUBBLE_SIZE/2), BUBBLE_SIZE, BUBBLE_SIZE);
        }
        
        public boolean collidesWith(Bubble other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            return distance < BUBBLE_SIZE - 5;
        }
        
        public boolean isAdjacent(Bubble other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            return distance < BUBBLE_SIZE + 5;
        }
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Bubble Shooter");
        BubbleGame game = new BubbleGame();
        
        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
