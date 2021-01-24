import java.util.logging.Logger;
import java.io.*;
import java.util.HashMap;


import ru.spbstu.pipeline.IExecutable;
import ru.spbstu.pipeline.IReader;
import ru.spbstu.pipeline.RC;

public class Reader implements IReader{
    private Integer nProcBytes;
    int fileSize;
    int nReadData;
    FileInputStream bFile;
    ReaderGrammar grammar = new ReaderGrammar();
    IExecutable consumer;
    IExecutable producer;


    final Logger logger;

    public Reader(Logger logger){
        this.logger = logger;
    }


    private byte[] readFile(){
        if (bFile == null)
            return  null;

        try{
            byte[] data = null;
            if (nProcBytes < fileSize - nReadData){
                data = new byte[nProcBytes];
                bFile.read(data);
                nReadData += nProcBytes;
            }
            else if (fileSize - nReadData > 0){
                data = new byte[fileSize - nReadData];
                nReadData = fileSize;
                bFile.read(data);
            }
            return data;
        } catch (IOException ex) {
            logger.severe(LogMsg.FAILED_TO_READ.msg);
        }
        return null;
    }

    @Override
    public RC setInputStream(FileInputStream file){
        if (file == null)
            return RC.CODE_INVALID_INPUT_STREAM;
        bFile = file;
        try {
            fileSize = (int)file.getChannel().size();
        } catch (IOException e) {
            logger.severe(LogMsg.INVALID_INPUT_STREAM.msg);
            return RC.CODE_INVALID_INPUT_STREAM;
        }
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConfig(String cfgPath){
        HashMap<String,String> cfgParam =
                SyntacticalAnalyser.getValidExpr(cfgPath,
                        grammar.delimiter(),
                        grammar.token(0));

        nProcBytes = SemanticAnalyser.getInteger(cfgParam, grammar.token(1));
        if (nProcBytes == null){
            logger.severe(LogMsg.INVALID_CONFIG_DATA.msg);
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        }
        logger.info(LogMsg.SUCCESS.msg);
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConsumer(IExecutable consumer){
        if (consumer == null)
            return RC.CODE_INVALID_ARGUMENT;
        this.consumer = consumer;
        return RC.CODE_SUCCESS;
    }


    public RC setProducer(IExecutable var1){
        producer = null;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute(byte[] inputData){
        byte[] data;
        RC rc = RC.CODE_SUCCESS;
        do{
            data = readFile();
            if (data == null)
                break;
            rc = consumer.execute(data);
            if (rc != RC.CODE_SUCCESS)
                break;
            //logger...
        }while (data != null);

        return rc;
    }
}
