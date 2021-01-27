import java.io.*;
import java.util.HashMap;
import java.util.logging.Logger;

public class SyntacticalAnalyser {
    static final Logger logger = Logger.getLogger(SyntacticalAnalyser.class.getName());

    private SyntacticalAnalyser(){ }

    static public HashMap<String, String> getValidExpr(String cfgPath,String separator, String endLine) {
        if (cfgPath == null || separator == null || endLine == null) {
            logger.severe(LogMsg.INVALID_ARGUMENTS.msg);
            return null;
        }
        logger.info(LogMsg.START_PARS.msg + "\n" + cfgPath);
        HashMap<String, String> validEpr = new HashMap<>();
        try{
            File cfg = new File(cfgPath);
            FileReader cfgReader = new FileReader(cfg);
            BufferedReader cfgBuf = new BufferedReader(cfgReader);
            String line = cfgBuf.readLine();
            while(line != null){
                if (line.matches(
                        "[ ]*[a-zA-Z_][a-zA-Z_0-9]*[ ]*" +
                        separator +
                        "[ ]*[\\\\a-zA-Z_0-9'.-]+[ ]*" +
                        endLine))
                {
                    String cleanLine = line.replace(" ", "").replace(endLine, "");
                    int eqIndex = cleanLine.indexOf(separator);
                    validEpr.put(
                            cleanLine.substring(0, eqIndex),
                            cleanLine.substring(eqIndex + 1, cleanLine.length()));
                }
                else{
                    logger.info(LogMsg.INVALID_EXPRESSION.msg + ": " + line);
                }
                line = cfgBuf.readLine();
            }
            cfgBuf.close();
            cfgReader.close();
        }catch(FileNotFoundException ex){
            logger.severe(LogMsg.NO_CONFIG_FILE.msg);
        }catch(IOException ex){
            logger.severe(LogMsg.CONFIG_GRAMMAR_ERROR.msg);
        }
        return validEpr;
    }
}
