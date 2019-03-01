package osp.Threads;
import java.util.Vector;
import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/*
 * Kevin Giraldo
 * 110653109
 */
 

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
    /**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    public ThreadCB()
    {
        // your code goes here
    	//calling super as first statement
    	super();

    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
        // your code goes here
    	//create ready queue ?

    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
        // your code goes here
    	//creating thread object using  deafault constructor
    	if(task.getThreadCount() > ThreadCB.MaxThreadsPerTask) {
	    	ThreadCB thread = new ThreadCB();
	    	//linking thread to its task , if add thread fails then we return FAILURE
	    	if(task.addThread(thread) == FAILURE) {
	    		return null;
	    	}
	    	//associate thread with task 
	    	thread.setTask(task);
	    	
	    	
	    	//set threads initial priority , the priority of the thread is determined by
	    	//PRIORITY = (total time the thread was waiting in the ready queue) / 
	    	//             (1+ total CPU time all the threads in the same task have used so far)
	    	
	    	//set thread to ready state
	    	thread.setStatus(ThreadReady);
	   	   return thread;
    	}
    	else {
    		
    		return null;
    	}
    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
        // destroy threads by setting status to ThreadKill and a number of other actions mus be performed
    	if(this.getStatus() == ThreadReady) {
    		
    		//remove from ready queue
    		
    		
    		//number of operations need to be performed
    		//A thread being destroyed might have initiated  an I/O operation
    		// and this is suspended on the corresponding IORB , in order to do this
    		//we scan devices in the table and cancel the pending ones
    		// 
    		for(int i = 0 ; i < Device.getTableSize() ; i++) {
    			
    			Device.get(i).cancelPendingIO(this);
    		}
    		//resources consumed by thread release into the pool
    		ResourceCB.giveupResources(this);
    		
    		//set status to ThreadKill
    		this.setStatus(ThreadKill);
    		//checking if corresponding thread task 
    		if(this.getTask().getThreadCount() == 0) {
    			//kill task since it has no threads left
    			this.getTask().kill(); 
    		}
    		
    		//dispatch a new thread by scanning ready queue and selecting which
    		//thread to dispatch
    	}
    	else if(this.getStatus() == ThreadRunning){
    		
    		//number of operations need to be performed
    		//A thread being destroyed might have initiated  an I/O operation
    		// and this is suspended on the corresponding IORB , in order to do this
    		//we scan devices in the table and cancel the pending ones
    		// 
    		for(int i = 0 ; i < Device.getTableSize() ; i++) {
    			
    			Device.get(i).cancelPendingIO(this);
    		}
    		//resources consumed by thread release into the pool
    		ResourceCB.giveupResources(this);
    		this.setStatus(ThreadKill);
    	}
    
    	
    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {
        // your code goes here

    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
        // your code goes here
    	MyOut.print(this,"RESUMING" + this);

    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    public static int do_dispatch()
    {
        // your code goes here
    	
    	return 0;
    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
