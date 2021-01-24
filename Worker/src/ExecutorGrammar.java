import ru.spbstu.pipeline.BaseGrammar;

public class ExecutorGrammar extends BaseGrammar{
    public ExecutorGrammar(){
        super(new String[] {";", "exeMode", "-e", "-d", "rightBorder", "leftBorder", "eps"});
    }

}
