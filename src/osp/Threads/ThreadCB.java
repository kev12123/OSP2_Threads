package osp.Threads;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/*     
 * Author: Kevin Giraldo
 * ID: 	   110653109
 * 
 * 
 * I pledge my honor that all parts of this project were done by me individually, without collaboration
 *  with anyone, and without consulting any external sources that could help with similar projects.
 */
 

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB implements Comparable<ThreadCB>
{   
	/*
	 * ArrayList used with  FIFO insertion 
	 * this ArrayList stores the ready threads
	 * in descending priority 
	 */
	private static ArrayList<ThreadCB> readyQueue;


	/*
	 * hash-map that will hold the task and the threads
	 * the purpose of this hash-map is to use the task as keys and a list of
	 * threads as the  value of each key/task this will allow the easy calculation
	 * of total CPU time time of all threads in the same tasks
	 */
	
	private static HashMap<TaskCB, GenericList> taskTracker;
	
	
    
    /**      
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    public ThreadCB()
    {
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
    	readyQueue = new ArrayList<ThreadCB>();
    	taskTracker = new HashMap<TaskCB,GenericList>();
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
    		dispatch();
    		return null;
    	}
    	else {
    			
    		ThreadCB thread = new ThreadCB();
    		//associate thread with task 
	    	thread.setTask(task);
	    	//add task as key to hashmap and put thread into list
	    	addToTaskTracker(task, thread);
	    	//linking thread to its task , if add thread fails then we return FAILURE
	    	if(task.addThread(thread) == FAILURE) {
	    		dispatch();
	    		return null;
	    	}
	    	//set priority of thread
	    	thread.setPriority((int) getPriority(task, thread));
	    	//set thread to ready state
	    	thread.setStatus(ThreadReady);
	    	//insert thread into ready queue
	    	readyQueue.add(thread);
	    	//sort prorities
	    	Collections.sort(readyQueue);
	    	//call dispatch method
	    	dispatch();
	   	   return thread;
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
        // if thread is read then it must be removed from the ready queue
    	if(this.getStatus() == ThreadReady) {
    		
    		//set status to ThreadKill
    		readyQueue.remove(this);
    		//this.setStatus(ThreadKill);
    		Collections.sort(readyQueue);
    
    	}
//    	else if(this.getStatus() == ThreadWaiting) {
//    		 
//    		this.setStatus(ThreadKill);
//    			
//    	}
    	else if(this.getStatus() == ThreadRunning){
    		//the running thread must be removed from the cpu 
    		//since we're changinng the state of the currently running hrread fromom thread running 
    		//we first set the page table register to null
    		MMU.setPTBR(null);
    		//change current thread to null
    		this.getTask().setCurrentThread(null);
    		
    		//this.setStatus(ThreadKill);
    		//number of operations need to be performed
    		//A thread being destroyed might have initiated  an I/O operation
    		// and this is suspended on the corresponding IORB , in order to do this
    		//we scan devices in the table and cancel the pending ones
    		// 
    	
    		
    	}
    	
    	//removing thread task list
    	this.getTask().removeThread(this);
    	//remove thread from task in tracker list
    	removeThreadFromTrackerList(this);
    	//set kill status for waiting thread
    	this.setStatus(ThreadKill);
    	cancelAllIODevices(this);
		//resources consumed by thread released into the pool of available 
		ResourceCB.giveupResources(this);
		
		// dispatch new thread
		dispatch();
		//sort prorities
    	//Collections.sort(readyQueue, prc);
		//check if corresponding task has any threads left
		if(taskHasThreads(this)) this.getTask().kill();
		
    
    	
    }

    /** Suspends the thread that is currently on the processor on the 
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
         
    		if(this.getStatus() == ThreadRunning) {
    			//since thread is changing from thread running 	//thread must lose control of the CPU  
    
    			//we first set the page table register to null
    			MMU.setPTBR(null); 
    			//change current thread to null
        		this.getTask().setCurrentThread(null);
        		//set status of thread to waiting
    			this.setStatus(ThreadWaiting);
        		
        	
    		}
    		else if(this.getStatus() >= ThreadWaiting) {
    			
    			this.setStatus(this.getStatus()+1);
    		
    		}
    		
    		event.addThread(this);
    	
    	

    		dispatch();

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
    	if(this.getStatus() > ThreadWaiting) {
    		
    		this.setStatus(this.getStatus()-1);
    	
    	}
    	else if(this.getStatus()==ThreadWaiting) {
        	
    		//since the thread becomes ready it should be placed in the 
        	//ready queue
    		//set status of thread to ready then add to the queue
    		this.setStatus(ThreadReady);
    		
        	readyQueue.add(this);
        	//this.setPriority((int) getPriority(task, this)); //I think makes sense to also recalculate priority here since the thread is being added to the readyQueue
        	//however for some reason this method prevents the program from runni
        	Collections.sort(readyQueue);
        
        	
    	}
    	
    	dispatch();
        
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
    	//Check for thread currently on the processor and reschedule it
    
    	ThreadCB thread = null;
        
    	try {
    		
    		thread = MMU.getPTBR().getTask().getCurrentThread();
    	}catch(NullPointerException e) {}
    	
    	if(thread != null) {
    		thread.getTask().setCurrentThread(null);
    		MMU.setPTBR(null);
    		thread.setStatus(ThreadReady);
    		//add to ready queue
    		readyQueue.add(thread);
    		thread.setPriority((int) getPriority(thread.getTask(), thread));
    		Collections.sort(readyQueue);
    	}
    	
    	//check if ready queue is empty in this case we return 
    	//failure
    	if(readyQueue.isEmpty()) {
    		MMU.setPTBR(null);
    		return FAILURE;
    	}else {
    		
    		//Get new ready thread from the ready queue , update PTBR and status of thread accordingly
    		thread = (ThreadCB) readyQueue.remove(0);
    		//since new thread is chosen we must perform a context switch 
    		MMU.setPTBR(thread.getTask().getPageTable());
        	thread.getTask().setCurrentThread(thread);
        	thread.setStatus(ThreadRunning);
    		
    	}
    	
    	//when a thread is dispatched it is given a time slice of 100 time unit
    	HTimer.set(100);
    	
    	return SUCCESS;
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
    
    /** Method used to scan all devices in device table and cancel them
     * for the respective thread
   
    @OSPProject Threads
 */
    
    private void cancelAllIODevices(ThreadCB thread) {
    	
    	for(int i = 0 ; i < Device.getTableSize() ; i++) {
			
			Device.get(i).cancelPendingIO(thread);
		}
    }
    
    
    private boolean taskHasThreads(ThreadCB thread) {
    	
    	//checking if corresponding thread tasks has any threads
    	//if it does not  then remove it from the taskTracker
		if(this.getTask().getThreadCount() == 0) {
			
			taskTracker.remove(thread.getTask());
			return true;
		}
		
		return false;
    	
    }
    
    
    /*
	 * getPriority returns priority of the thread based 
	 * off the formula given
	 */
    private static double getPriority(TaskCB task , ThreadCB thread){
    	
    	//set threads initial priority , the priority of the thread is determined by
    	//PRIORITY = (total time the thread was waiting in the ready queue) / 
    	//(1+ total CPU time all the threads in the same task have used so far
    	 
    	return ((HClock.get()-thread.getCreationTime()-thread.getTimeOnCPU()) / (1 + getCpuTimeOfAllThreadsInTask(task)));
    }
   
    
    /*
     * addToTaskTracker adds a key and value to the map
     * it first check if the key exists , if false then
     * we put the new task and the and create a new list
     * of threads for the tasks.
     * 
     * if the key already exists then we get the threads list
     * and append the new thread
     * 
     */
    private static void addToTaskTracker(TaskCB task  , ThreadCB thread) {
    	
    	if(!(taskTracker.containsKey(task))) {
    		
    		GenericList threadList = new GenericList();
    		taskTracker.put(task, threadList);
    	}else {
    		
    		taskTracker.get(task).append(thread);
    	}
    }
    
    
    /* 
     * getCpuTimeOfAllThreadsInTask finds the totalCpuTime by using
     * the taskTracker that keeps track of task and its threads
     * 
     */
    
    private static double getCpuTimeOfAllThreadsInTask(TaskCB task) {
    	
    	
    	Enumeration<?> eNum  = taskTracker.get(task).forwardIterator();
    	Double totalTimeOfThreadsInTask = 0.0;
    	while(eNum.hasMoreElements()) {
    		ThreadCB thread = (ThreadCB) eNum.nextElement();
    		totalTimeOfThreadsInTask+= thread.getTimeOnCPU();
    	}
    	return totalTimeOfThreadsInTask;
    	
    }
    
    
    /* 
     * removeThreadFromTrackerList , it removes the thread from the tracker list
     * if the is removed from the task
     * 
     */
    
    private static void removeThreadFromTrackerList(ThreadCB thread) {
    	
    	GenericList ttList = taskTracker.get(thread.getTask());
    	
    	ttList.remove(thread);
    	
    	
    }

	@Override
	public int compareTo(ThreadCB o) {
		// TODO Auto-generated method stub
		return o.getPriority()-this.getPriority();
	}
 
}

/*
      Feel free to add local classes to improve the readability of your code
*/


