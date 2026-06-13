package imageprocessing;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
public class ImageGenerator {
    public static BufferedImage generate(int width, int height) {
        BufferedImage img=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0,0,new Color(30,80,160),width,height,new Color(120,30,120)));
        g.fillRect(0,0,width,height);
        g.setColor(new Color(255,255,255,40));
        int step=Math.max(20,width/40);
        for (int x=0;x<width;x+=step) g.drawLine(x,0,x,height);
        for (int y=0;y<height;y+=step) g.drawLine(0,y,width,y);
        Random rng=new Random(42);
        for (int i=0;i<80;i++) {
            int cx=rng.nextInt(width),cy=rng.nextInt(height),r=10+rng.nextInt(Math.max(1,width/12));
            g.setColor(new Color(rng.nextInt(256),rng.nextInt(256),rng.nextInt(256),180));
            g.fillOval(cx-r,cy-r,2*r,2*r);
        }
        g.setColor(new Color(255,255,255,120)); g.setStroke(new BasicStroke(3));
        for (int i=0;i<6;i++) g.drawLine(rng.nextInt(width),rng.nextInt(height),rng.nextInt(width),rng.nextInt(height));
        g.dispose(); return img;
    }
}
