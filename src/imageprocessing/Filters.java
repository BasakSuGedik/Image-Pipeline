package imageprocessing;
import java.awt.image.BufferedImage;
public class Filters {
    private static final float[] GAUSS_KERNEL = {0.0625f, 0.25f, 0.375f, 0.25f, 0.0625f};
    public static void gaussianBlur(BufferedImage src, BufferedImage dst, int startRow, int endRow) {
        int width = src.getWidth(), height = src.getHeight();
        float[][] tmpR = new float[endRow-startRow][width];
        float[][] tmpG = new float[endRow-startRow][width];
        float[][] tmpB = new float[endRow-startRow][width];
        for (int y = startRow; y < endRow; y++) {
            for (int x = 0; x < width; x++) {
                float r=0,g=0,b=0;
                for (int k=-2;k<=2;k++) {
                    int xx=clamp(x+k,0,width-1); int rgb=src.getRGB(xx,y);
                    r+=GAUSS_KERNEL[k+2]*((rgb>>16)&0xFF); g+=GAUSS_KERNEL[k+2]*((rgb>>8)&0xFF); b+=GAUSS_KERNEL[k+2]*(rgb&0xFF);
                }
                tmpR[y-startRow][x]=r; tmpG[y-startRow][x]=g; tmpB[y-startRow][x]=b;
            }
        }
        for (int y = startRow; y < endRow; y++) {
            for (int x = 0; x < width; x++) {
                float r=0,g=0,b=0;
                for (int k=-2;k<=2;k++) {
                    int yy=clamp(y+k,0,height-1); float kr,kg,kb;
                    if (yy>=startRow&&yy<endRow) { kr=tmpR[yy-startRow][x]; kg=tmpG[yy-startRow][x]; kb=tmpB[yy-startRow][x]; }
                    else { int rgb=src.getRGB(x,yy); kr=(rgb>>16)&0xFF; kg=(rgb>>8)&0xFF; kb=rgb&0xFF; }
                    r+=GAUSS_KERNEL[k+2]*kr; g+=GAUSS_KERNEL[k+2]*kg; b+=GAUSS_KERNEL[k+2]*kb;
                }
                dst.setRGB(x,y,toRGB(r,g,b));
            }
        }
    }
    private static final int[] SOBEL_X = {-1,0,1,-2,0,2,-1,0,1};
    private static final int[] SOBEL_Y = {-1,-2,-1,0,0,0,1,2,1};
    public static void sobelEdge(BufferedImage src, BufferedImage dst, int startRow, int endRow) {
        int width=src.getWidth(), height=src.getHeight();
        for (int y=startRow;y<endRow;y++) {
            for (int x=0;x<width;x++) {
                float gxR=0,gyR=0,gxG=0,gyG=0,gxB=0,gyB=0; int idx=0;
                for (int dy=-1;dy<=1;dy++) for (int dx=-1;dx<=1;dx++) {
                    int xx=clamp(x+dx,0,width-1),yy=clamp(y+dy,0,height-1);
                    int rgb=src.getRGB(xx,yy);
                    float pr=(rgb>>16)&0xFF,pg=(rgb>>8)&0xFF,pb=rgb&0xFF;
                    gxR+=SOBEL_X[idx]*pr; gyR+=SOBEL_Y[idx]*pr;
                    gxG+=SOBEL_X[idx]*pg; gyG+=SOBEL_Y[idx]*pg;
                    gxB+=SOBEL_X[idx]*pb; gyB+=SOBEL_Y[idx]*pb; idx++;
                }
                dst.setRGB(x,y,toRGB((float)Math.sqrt(gxR*gxR+gyR*gyR),(float)Math.sqrt(gxG*gxG+gyG*gyG),(float)Math.sqrt(gxB*gxB+gyB*gyB)));
            }
        }
    }
    public static void brightnessContrast(BufferedImage src, BufferedImage dst, int startRow, int endRow, float alpha, float beta) {
        int width=src.getWidth();
        for (int y=startRow;y<endRow;y++) for (int x=0;x<width;x++) {
            int rgb=src.getRGB(x,y);
            dst.setRGB(x,y,toRGB(clampF(alpha*((rgb>>16)&0xFF)+beta,0,255),clampF(alpha*((rgb>>8)&0xFF)+beta,0,255),clampF(alpha*(rgb&0xFF)+beta,0,255)));
        }
    }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private static float clampF(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    static int toRGB(float r,float g,float b){return 0xFF000000|((int)clampF(r,0,255)<<16)|((int)clampF(g,0,255)<<8)|(int)clampF(b,0,255);}
}
