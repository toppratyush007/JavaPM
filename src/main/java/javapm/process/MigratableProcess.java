package javapm.process; 
import javapm.io.TransactionalFileInputStream;
import javapm.io.TransactionalFileOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The abstract class of Migratable Process
 *
 * The default behaviours of migratable process contains
 * <code>run()</code> which implements the <code>Runnable</code>
 * interface, and <code>suspend()</code>,
 * We need to save the IO state during the migration.
 * So we use our transactional file IO stream classes.
 *
 * @author Vasu Vardhan(vardhan)
 * @author Pratyush Kumar(pratyush)
 * @see javapm.io.TransactionalFileInputStream
 * @see javapm.io.TransactionalFileOutputStream
 */
public abstract class MigratableProcess implements Runnable, Serializable{
    protected static Logger LOG = LogManager.getLogger(MigratableProcess.class);


    /**
     * Process's arguments list
     */
    protected List<String> arguments;

    /**
     * The suspending flag. When the flag is set, the
     * <code>processing()</code> function should break the idle loop.
     */
    protected volatile boolean suspending;

    /**
     * The process ID
     */
    protected long id;

    /**
     * Constructor of MigratableProcess without any argument.
     */
    public MigratableProcess(){
        initProcess(new String[0]);
        this.id = ProcessManager.getInstance().generateID();
    }

    /**
     * Constructor of MigratableProcess with arguments
     *
     * @param arguments the process's arguments
     */
    public MigratableProcess(String[] arguments){
        initProcess(arguments);
        this.id = ProcessManager.getInstance().generateID();
    }

    /**
     * Initialize the process's fields, generate a process ID from
     * <code>ProcessManager</code>
     *
     * @param arguments the process's arguments
     * @see javapm.process.ProcessManager#generateID()
     */
    public void initProcess(String[] arguments){
        this.arguments = new ArrayList<String>(Arrays.asList(arguments));
        this.suspending = false;
        //this.id = ProcessManager.getInstance().generateID();
    }

    /**
     * The implementation of <code>Runnable</code> interface.
     * Call the <code>processing()</code> idle loop, if any
     * <code>IOException</code> raised, we log the error information.
     * We assure when the process finished its work, the
     * <code>ProcessManager</code> is notified by using
     * <code>finishProcess</code>.
     *
     * @see javapm.process.MigratableProcess#processing()
     * @see javapm.process.ProcessManager#finishProcess(MigratableProcess)
     */
    public void run(){
        try{
            processing();
        }catch (IOException e){
            LOG.error(this.getClass().getSimpleName() + "[" + id + "]", e);
        }finally {
            ProcessManager.getInstance().finishProcess(this);
            suspending = false;
        }
    }

    /**
     * Suspend the running process.
     * Set the <code>suspending</code> flag, and wait the process breaks
     * from its idle loop.
     *
     * @throws InterruptedException if the suspending process is
     *                              interrupted
     */
    public void suspend() throws InterruptedException {
        suspending = true;
        while(suspending)
        {
        	Thread.sleep(10);
        }
    }

    /**
     * Resume the running process from suspending.
     * Clear the <code>suspending</code> flag, and start the process again.
     *
     */
    public void resume(){
    	suspending = false;
    }
    
    
    /**
     * Using reflection to set <code>migrated</code> flag of
     *  <code>TransactionalFileInputStream</code>,
     * <code>TransactionalFileOutputStream</code>, 
     * and generate a new process ID after the migration.
     *
     * @see javapm.process.ProcessManager#generateID()
     * @see javapm.io.TransactionalFileInputStream#setMigrated(boolean)
     * @see javapm.io.TransactionalFileOutputStream#setMigrated(boolean)
     */
    public void migrated(){
        this.id = ProcessManager.getInstance().generateID();

        Field f[] = this.getClass().getDeclaredFields();
        for (int i = 0; i < f.length; i++) {
        	if(f[i].getType().getSimpleName().equals("TransactionalFileInputStream"))
        	{
        		f[i].setAccessible(true);
        		TransactionalFileInputStream in = null;
        		try {
					in = (TransactionalFileInputStream) f[i].get(this);
				} catch (IllegalArgumentException e) {

					e.printStackTrace();
				} catch (IllegalAccessException e) {

					e.printStackTrace();
				}
        		if(in != null) {
        			in.setMigrated(true);
        		}

        	}
        	if(f[i].getType().getSimpleName().equals("TransactionalFileOutputStream"))
        	{
        		f[i].setAccessible(true);
        		TransactionalFileOutputStream out = null;
        		try {
					out = (TransactionalFileOutputStream) f[i].get(this);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
        		if(out != null)
        		{
	        		out.setMigrated(true);
        		}
        	}
         }
    }
    

    /**
     * Get the process id
     *
     * @return process id
     */
    public long getId(){
        return id;
    }

    /**
     * The printable information of <code>MigratableProcess</code>
     * The string format is: MigratableProcess[ID]: ARG0, ARG1...
     *
     * @return the printable information
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getSimpleName());
        sb.append("[" + id + "]: ");
        for(int i = 0; i < arguments.size(); i++){
            if(i > 0){
                sb.append(", ");
            }
            sb.append(arguments.get(i));
        }
        return sb.toString();
    }

    /**
     * The idle function of process.
     * The implementation of this function should loop with the
     * <code>suspending</code> flag.
     *
     * @throws IOException if any IOException occurs
     */
    public abstract void processing() throws IOException;
}
