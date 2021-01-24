import java.nio.ByteBuffer;
import java.util.logging.Logger;

import ru.spbstu.pipeline.IExecutable;
import ru.spbstu.pipeline.RC;



public class Decoder{
    final int doubleSize = 8;
    double leftBorder;
    double rightBorder;
    int textLen;
    Double eps;
    ByteDouPair[] workingSegment;
    PacketsMemory packetsMemory = new PacketsMemory();
    final Logger logger;


    public Decoder(Logger logger){
        this.logger = logger;
    }

    public RC decode(byte[] encodeData, IExecutable consumer){
        byte[] outputData;
        packetsMemory.addData(encodeData);

        while (packetsMemory.nextPackIsReady()) {
            ByteBuffer encodeDataBuf = ByteBuffer.wrap(packetsMemory.getNextPack());
            if (!fillDecodeParam(encodeDataBuf)) {
                return RC.CODE_SUCCESS;
            }
            double encodeText[] = getEncodeText(encodeDataBuf);
            byte text[] = decodePacks(encodeText);
            workingSegment = null;
            packetsMemory.addDecodeData(text);
        }
        outputData = packetsMemory.getDecodeData();
        return consumer.execute(outputData);
    }

    /* the method translate input data to decoding data */
    private boolean fillDecodeParam(ByteBuffer encodeData){
        try{
            encodeData.getInt();
            eps = encodeData.getDouble();
            int uniqueSymbolsNum = encodeData.getInt();
            leftBorder = encodeData.getDouble();
            workingSegment = new ByteDouPair[uniqueSymbolsNum];
            for (int i = 0; i < uniqueSymbolsNum; i++){
                byte curSymbol = encodeData.get();
                double curSymRBorder = encodeData.getDouble();
                workingSegment[i] = new ByteDouPair(curSymbol, curSymRBorder);
                if (i == uniqueSymbolsNum - 1)
                    rightBorder = curSymRBorder;
            }
            textLen = encodeData.getInt();
            return true;
        }catch (Throwable ex){
            logger.severe(LogMsg.INVALID_CONFIG_DATA.msg);
        }
        return false;
    }

    /* the method translate input data to encoding text */
    private double[] getEncodeText(ByteBuffer encodeData){
        int encodeTextLen = (encodeData.capacity() - encodeData.position()) / doubleSize;
        double encodeText[] = new double[encodeTextLen];
        for (int i = 0; i < encodeTextLen; i++){
            encodeText[i] = encodeData.getDouble();
        }
        return encodeText;
    }

    /* decode each double number to sequence bytes */
    private byte[] decodePacks(double[] encodeText){
        byte decodeText[] = new byte[textLen];
        int i = 0;
        int startDecodeText = 0;
        for ( ; i < encodeText.length; i++){
            startDecodeText = decodePack(decodeText, encodeText[i], startDecodeText);
        }

        return decodeText;
    }

    /* the method decode one double number to sequence bytes */
    private int decodePack(byte[] decodeText, double encodeNum, int startDecodeText){
        double curSegment[] = {leftBorder, rightBorder};
        int i = startDecodeText;
        while (i < decodeText.length){
            byte nextByte = calcNewSegment(curSegment, encodeNum);
            if (Math.abs(curSegment[0] - curSegment[1]) < eps){
                break;
            }
            decodeText[i] = nextByte;
            i++;
        }
        return i;
    }

    /* this method calculate new working segment by double number */
    private byte calcNewSegment(double[] curSegment, double encodeNum){
        //double k = (rightBorder - leftBorder) / (curSegment[1] - curSegment[0]);
        //double encodeNumOnGS = k * (encodeNum - curSegment[0])  + leftBorder;
        double globalSegment[] = {leftBorder, rightBorder};
        double encodeNumOnGS = movePointFromTo(curSegment, globalSegment, encodeNum);
        int i = searchNumInGS(encodeNumOnGS);

        try {
            double newSegmentR = movePointFromTo(globalSegment, curSegment, workingSegment[i].rBorder);
            // 1 / k * (workingSegment[i].rBorder - leftBorder)  + curSegment[0];
            if (i != 0){
                double newSegmentL = movePointFromTo(globalSegment, curSegment, workingSegment[i - 1].rBorder);
                // 1 / k * (workingSegment[i - 1].rBorder - leftBorder)  + curSegment[0];
                curSegment[0] = newSegmentL;
            }
            curSegment[1] = newSegmentR;
        }catch(ArrayIndexOutOfBoundsException ex)
        {
            System.out.println(ex.getMessage());
            System.out.println("File can not be decoded");
            System.exit(0);
        }

        return workingSegment[i].symbol;
    }

    /* maps point from one segment to another */
    private double movePointFromTo(double[] curSegment, double[] purpSegment, double point){
        double k = (purpSegment[1] - purpSegment[0]) / (curSegment[1] - curSegment[0]);
        double purposePoint = k * (point - curSegment[0])  + purpSegment[0];

        return purposePoint;
    }

    /* the method search byte appropriate input number */
    private int searchNumInGS(double num){
        int i = 0;
        for ( ; i < workingSegment.length - 1; i++){
            if (workingSegment[i].rBorder >= num){
                break;
            }
        }
        return i;
    }
}

