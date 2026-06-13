package imageprocessing;
import java.awt.image.BufferedImage;
public class SequentialPipeline {
    public static long run(BufferedImage input, BufferedImage output) {
        int w=input.getWidth(), h=input.getHeight();
        BufferedImage tmp1=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        BufferedImage tmp2=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        long start=System.nanoTime();
        Filters.gaussianBlur(input,tmp1,0,h);
        Filters.sobelEdge(tmp1,tmp2,0,h);
        Filters.brightnessContrast(tmp2,output,0,h,1.2f,15f);
        return (System.nanoTime()-start)/1_000_000;
    }
}
