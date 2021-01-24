
import java.util.logging.Logger;


public class Main {
    static final Logger logger = Logger.getLogger(Reader.class.getName());
    public static void main(String[] args) {
        if (args.length != 1)
        {
            logger.severe(LogMsg.INVALID_ARGUMENTS.msg);
        }else {
            Manager manager = new Manager(args[0]);
            manager.execute();
        }
     }
}
