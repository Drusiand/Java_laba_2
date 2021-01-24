import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.logging.Logger;


public class PacketsMemory{
    ArrayDeque<byte[]> allData = new ArrayDeque<>();
    ArrayDeque<byte[]> decodeData = new ArrayDeque<>();
    static final Logger logger = Logger.getLogger(PacketsMemory.class.getName());

    public void addData(byte[] nextData){
        if (allData.size() == 0 || allData.peekFirst().length >= 4) {
            allData.addFirst(nextData);
            return;
        }

        byte[] newFirstArr = new byte[allData.peekFirst().length + nextData.length];
        for (int i = 0; i < allData.peekFirst().length; i++)
            newFirstArr[i] = allData.peekFirst()[i];
        for (int j = 0, i = allData.peekFirst().length; j < nextData.length; i++, j++)
            newFirstArr[i] = nextData[j];

        allData.pollFirst();
        allData.addFirst(newFirstArr);
    }

    private int getPackSize()
    {
        if (allData.size() == 0)
            return 0;
        ByteBuffer packSizeBuf = ByteBuffer.wrap(Arrays.copyOfRange(allData.peekLast(), 0, 4));
        return packSizeBuf.getInt();
    }

    public boolean nextPackIsReady(){
        if (allData.size() == 0)
            return false;

        int packSize = getPackSize();
        if (packSize == 0)
            return false;

        int allDataSize = 0;
        for (byte[] i : allData){
            allDataSize += i.length;
        }

        if (packSize <= allDataSize)
            return true;
        return false;
    }

    public byte[] getNextPack(){
        if (!nextPackIsReady())
            return null;
        int i = 0;
        int j = 0;
        int packSize = getPackSize();

        byte[] pack;
        try {
            pack = new byte[packSize];
        }
        catch(NegativeArraySizeException ex){
            logger.severe(LogMsg.INVALID_ARGUMENTS.msg);
            return null;
        }

        while (i < packSize){
            if (j >= allData.peekLast().length) {
                allData.pollLast();
                j = 0;
            }else{
                pack[i] = allData.peekLast()[j];
                i++;
                j++;
            }
        }


        if (allData.peekLast().length > j) {
            byte[] extraData = new byte[allData.peekLast().length - j];
            for (int k = 0; j < allData.peekLast().length; j++, k++){
                extraData[k] = allData.peekLast()[j];
            }
            allData.pollLast();
            allData.addLast(extraData);
        }
        else if (allData.peekLast().length == j)
            allData.pollLast();


        return pack;
    }

    public void addDecodeData(byte[] nextData){
        decodeData.addFirst(nextData);
    }

    public byte[] getDecodeData(){
        if (decodeData.size() == 0)
            return null;

        int allDataSize = 0;
        for (byte[] i : decodeData){
            allDataSize += i.length;
        }

        byte[] allDecodeData = new byte[allDataSize];

        for (int i = 0, j = 0; i < allDataSize; i++, j++){
            if (j >= decodeData.peekLast().length) {
                decodeData.pollLast();
                j = 0;
            }
            allDecodeData[i] = decodeData.peekLast()[j];
        }
        decodeData.pollLast();

        return allDecodeData;
    }

}

