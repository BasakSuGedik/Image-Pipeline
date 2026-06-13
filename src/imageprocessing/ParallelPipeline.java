package imageprocessing;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.*;
public class ParallelPipeline {
    public static long run(BufferedImage input, BufferedImage output, int threadCount) throws InterruptedException, ExecutionException {
        int w=input.getWidth(), h=input.getHeight();
        BufferedImage tmp1=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        BufferedImage tmp2=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        ExecutorService pool=Executors.newFixedThreadPool(threadCount);
        int[] start=new int[threadCount], end=new int[threadCount];
        int sh=h/threadCount;
        for (int i=0;i<threadCount;i++){start[i]=i*sh; end[i]=(i==threadCount-1)?h:(i+1)*sh;}
        long t0=System.nanoTime();
        submitAndWait(pool,threadCount,(i)->Filters.gaussianBlur(input,tmp1,start[i],end[i]));
        submitAndWait(pool,threadCount,(i)->Filters.sobelEdge(tmp1,tmp2,start[i],end[i]));
        submitAndWait(pool,threadCount,(i)->Filters.brightnessContrast(tmp2,output,start[i],end[i],1.2f,15f));
        long elapsed=(System.nanoTime()-t0)/1_000_000;
        pool.shutdown(); return elapsed;
    }
    private static void submitAndWait(ExecutorService pool,int n,Task t) throws InterruptedException,ExecutionException {
        List<Future<?>> f=new ArrayList<>();
        for (int i=0;i<n;i++){final int idx=i; f.add(pool.submit(()->{t.run(idx);return null;}));}
        for (Future<?> fu:f) fu.get();
    }
    interface Task{void run(int i);}
}
