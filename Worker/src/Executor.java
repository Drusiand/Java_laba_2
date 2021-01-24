import ru.spbstu.pipeline.IExecutable;
import ru.spbstu.pipeline.IExecutor;
import ru.spbstu.pipeline.RC;

import java.util.HashMap;
import java.util.logging.Logger;

public class Executor implements IExecutor{

    final Encoder encoder;
    final Decoder decoder;
    IExecutable consumer;
    IExecutable producer;
    ExecutorGrammar grammar = new ExecutorGrammar();
    String exeMode;

    public Executor(Logger logger){
        encoder = new Encoder(logger);
        decoder = new Decoder(logger);
    }


    @Override
    public RC setConfig(String cfgPath){
        HashMap<String,String> cfgParam =
                SyntacticalAnalyser.getValidExpr(cfgPath,
                        grammar.delimiter(),
                        grammar.token(0));

        if (cfgParam.get(grammar.token(1)) == null)
            return RC.CODE_CONFIG_SEMANTIC_ERROR;

        exeMode = cfgParam.get(grammar.token(1));
        if (exeMode.equals(grammar.token(2)))
            return encoder.setConfig(cfgParam);
        if (exeMode.equals(grammar.token(3)))
            return RC.CODE_SUCCESS;
        return RC.CODE_CONFIG_GRAMMAR_ERROR;
    }

    @Override
    public RC setConsumer(IExecutable consumer){
        if (consumer == null)
            return RC.CODE_INVALID_ARGUMENT;
        this.consumer = consumer;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IExecutable producer){
        if (producer == null)
            return RC.CODE_INVALID_ARGUMENT;
        this.producer = producer;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute(byte[] inputData){
        if (inputData == null)
            return consumer.execute(null);
        else if (exeMode.equals(grammar.token(2)))
            return encoder.encode(inputData, consumer);
        else if (exeMode.equals(grammar.token(3)))
            return decoder.decode(inputData, consumer);

        return RC.CODE_CONFIG_GRAMMAR_ERROR;
    }
}
