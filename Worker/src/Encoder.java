import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import ru.spbstu.pipeline.RC;
import ru.spbstu.pipeline.IExecutable;


public class Encoder {
    HashMap<Byte, Double[]> workingSegment = new HashMap<>();  // working segment with distributed characters
    Double rightBorder;                                        // global working segment border
    Double leftBorder;
    Double eps;
    final Logger logger;

    public Encoder(Logger logger){
        this.logger = logger;
    }

    public RC setConfig(HashMap<String, String> cfgParam){

        ExecutorGrammar grammar = new ExecutorGrammar();

        rightBorder = SemanticAnalyser.getDouble(cfgParam, grammar.token(4));
        leftBorder = SemanticAnalyser.getDouble(cfgParam, grammar.token(5));
        eps = SemanticAnalyser.getDouble(cfgParam, grammar.token(6));

        if (eps == null || rightBorder == null || leftBorder == null || rightBorder <= leftBorder){
            logger.severe(LogMsg.INVALID_CONFIG_DATA.msg);
            return RC.CODE_CONFIG_GRAMMAR_ERROR;
        }
        //logger.info(LogMsg.SUCCESS.msg);
        return RC.CODE_SUCCESS;
    }

    public RC encode(byte[] text, IExecutable consumer){

        byte[] outputData;

        HashMap<Byte, Double> OccTable = charOccTable(text);
        fillWorkingSegment(OccTable);

        ArrayDeque<Double> encodedTextQ = new ArrayDeque<>();
        int startPakc = 0;
        while(startPakc < text.length){
            startPakc = encodePack(text, encodedTextQ, startPakc);
        }

        double[] encodedText = new double[encodedTextQ.size()];
        int nDouble = encodedTextQ.size();
        for (int i = 0; i < nDouble; i++){
            encodedText[i] = encodedTextQ.pollLast();
        }


        outputData = packToBytes(encodedText, text.length);
        workingSegment = new HashMap<>();

        return consumer.execute(outputData);
    }

    /* the method counts the fraction of occurrences of each character from the text */
    private HashMap<Byte, Double> charOccTable(byte[] text){
        HashMap<Byte, Double> charOccTable = new HashMap<>(); //character occurrence table
        double unitOccFrac = 1. / ((double)text.length); // unit fraction of occurrence

        for (Byte symbol : text){
            if (charOccTable.containsKey(symbol)){
                Double charOccFrac = charOccTable.get(symbol); // number of occurrences of character
                charOccFrac += unitOccFrac;
                charOccTable.put(symbol, charOccFrac);
            } else{
                //case when symbol meets first time
                charOccTable.put(symbol, unitOccFrac);
            }
        }

        return charOccTable;
    }

    /* transition from the table of occurrences to the working segment */
    private void fillWorkingSegment(HashMap<Byte, Double> charOccTable){
        Double curLeftBorder = leftBorder; //the current left border of the unfilled working segment

        for (Byte symbol : charOccTable.keySet()) {
            Double charLeftBorder = curLeftBorder;
            double charRightBorder = curLeftBorder + charOccTable.get(symbol) * (rightBorder - leftBorder);
            workingSegment.put(symbol, new Double[]{charLeftBorder, charRightBorder});
            curLeftBorder = charRightBorder;
        }

    }

    /* this method encode characters in double number while segment size less eps*/
    private int encodePack(byte[] text, ArrayDeque<Double> encodeTextQ, int startPack){
        Double[] curSegment = new Double[]{leftBorder, rightBorder};
        int i = startPack;
        while(i < text.length){
            calcNewSegment(text[i], curSegment);
            i++;
            if (Math.abs(curSegment[0] - curSegment[1]) < eps){
                i--;
                break;
            }
        }

        encodeTextQ.addFirst((curSegment[1] + curSegment[0]) / 2.);
        return i;
    }

    /* this method calculate new working segment by another symbol */
    /* the curSegment argument is current working segment border */
    private void calcNewSegment(Byte symbol, Double[] curSegment){
        Double newSegmentL = moveToCur(workingSegment.get(symbol)[0], curSegment);
        Double newSegmentR = moveToCur(workingSegment.get(symbol)[1], curSegment);
        curSegment[0] = newSegmentL;
        curSegment[1] = newSegmentR;
    }

    /* this method move point from global segment to current */
    private Double moveToCur(double x, Double[] curSegment){
        return (x - leftBorder) * (curSegment[1] - curSegment[0]) / (rightBorder - leftBorder) + curSegment[0];
    }

    /* the method pack encode text and decoding data in byte array*/
    private byte[] packToBytes(double[] encodeText, int textLen){
        final int byteSize = 1;
        final int intSize = 4;
        final int doubleSize = 8;

        ByteDouPair[] pairArr = HashToPairArr();
        Arrays.sort(pairArr);

        int allDataSize =
                intSize + // current package data size
                doubleSize + // eps
                intSize + // unique symbols num
                doubleSize + // working segment lift border
                pairArr.length * (byteSize + doubleSize) + // symbols and their right borders pair
                intSize + // num of all symbols in text
                encodeText.length * doubleSize; // set of numbers encoding text
        ByteBuffer allEncodeData = ByteBuffer.allocate(allDataSize);

        putCodeKeys(allEncodeData, pairArr);
        putTextData(allEncodeData, encodeText, textLen);

        return allEncodeData.array();
    }

    /* the method pack decoding data in top of package encoding data */
    private void putCodeKeys(ByteBuffer buf, ByteDouPair[] pairArr){
        buf.putInt(buf.capacity());
        buf.putDouble(eps);
        buf.putInt(pairArr.length);
        buf.putDouble(leftBorder);
        for (ByteDouPair byteDouPair : pairArr) {
            buf.put(byteDouPair.symbol);
            buf.putDouble(byteDouPair.rBorder);
        }
    }

    /* the method pack encode text in package encoding data */
    private void putTextData(ByteBuffer buf, double[] encodeText, int textLen){
        buf.putInt(textLen);
        for (double codeNum : encodeText){
            buf.putDouble(codeNum);
        }
    }

    /* the method make array of Double and Byte pair to sort array and put its in encoding data */
    private ByteDouPair[] HashToPairArr(){
        int pairNum = workingSegment.size();
        ByteDouPair[] pairArr = new ByteDouPair[pairNum];
        int i = 0;
        for (Byte symbol : workingSegment.keySet()){
            pairArr[i] = new ByteDouPair(symbol, workingSegment.get(symbol)[1]); // pair of char value in byte type and its segment right border
            i++;
        }
        return pairArr;
    }
}
