public class ByteDouPair implements Comparable<Object>{
    public final Byte symbol;
    public final Double rBorder;
    public ByteDouPair(Byte symbol, Double rBorder){
        this.symbol = symbol;
        this.rBorder = rBorder;
    }
    public int compareTo(Object o){
        if (rBorder > ((ByteDouPair)o).rBorder)
            return 1;
        else if(rBorder < ((ByteDouPair)o).rBorder)
            return -1;
        else
            return 0;
    }
}
