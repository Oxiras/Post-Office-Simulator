import java.util.LinkedList;							// Needed for LinkedList class.
import java.util.Queue;								// Needed for Queue class.
import java.util.concurrent.Semaphore;						// Needed for Semaphore class.

/*
 * PROGRAM: Poffice.java
 * @author: Moustapha Dieng
 * This program simulates a post office. Postal workers
 * are tasked to process various customer orders. All
 * tasks execute conccurently through the use of threading.
 * Semaphores are used to keep certain sections mutually exclusive.
 **/
public class Poffice implements Runnable {
	
	private static final int MAXWRKS = 3;					// To hold number of workers.
	private final Thread[] pWorker = new Thread[MAXWRKS];			// Declare worker threads.
	private int wID;							// To hold worker IDs.
	public final static int MAXCX = 50;					// To hold number of customers.
	public static volatile int custProcessed = 1;				// To keep count of number of customers processed.
	public static volatile int wrkID;					// ID given by worker to customer
	public static boolean shouldStop = false;				// Flag to indicate end of processing. 
	public static Queue<Integer> line = new LinkedList<>();			// Keeps a list of customer (about to be served) IDs.
	public static Queue<String> order = new LinkedList<>();			// Keeps a list of customer (about to be served)  orders.
	public static Semaphore enterPostOffice = new Semaphore(10, true);	// Regulates number of customers that can get in post office.
	public static Semaphore readyToBeServed = new Semaphore(3, true);	// Regulates number of customers ready to be served.
	public static Semaphore mutex1 = new Semaphore(1, true);		// Handles first critical section.
	public static Semaphore mutex2 = new Semaphore(0, true);		// Handles second critical section.
	public static Semaphore workerReady = new Semaphore(0, true);		// Handles ready status of worker.
	public static Semaphore takeOrder = new Semaphore(0, true);		// Handles ready status to take order.
	public static Semaphore scaleInUse = new Semaphore(1, true);		// Handles scale mutual exclusion.
	public static Semaphore taskComplete[] = new Semaphore[50];		// Handles task completion.

	/*
	 * Constructor: Initiliazes worker thread.
	 * @param: thread number.
	 */
	public Poffice(int wID) {
		this.wID = wID;
		pWorker[wID] = new Thread("Postal worker " + wID);
		System.out.println(pWorker[wID].getName() + " created.");
	}
	
	/*
	 * execTask method: Process customer order.
	 * @param: thread, thread number, task.
	 */
 	private void execTask(Thread wrk, int num, String tsk) {
		int duration = 0;
		System.out.println(wrk.getName() + " serving customer " + num + ".");
		switch (tsk) {
			case "buy stamps":
					duration = 1000;
					break;
			case "mail a letter":
					duration = 1500;
					break;
			case "mail a package":
					duration = 2000;
					break;
		}
		try {
			wrk.sleep(duration);
		} catch(InterruptedException e) {
			System.out.println(e.getMessage());
		}
	}
	
	/*
	 * wait method: Acquires semaphore permit.
	 * @param: semaphore.
	 */
	public static void wait(Semaphore sem) {
		try {
			sem.acquire();
		} catch(InterruptedException e) {
			System.out.println(e.getMessage());
		}
	}
	
	/*
	 * signal method: Releases semaphore permit.
	 * @param: semaphore.
	 */
	public static void signal(Semaphore sem) {
		sem.release();
	}
	
	/*
	 * enqueueNumber method: Adds id to queue.
	 * @param: id.
	 */
	public static void enqueueNumber(int id) {
		line.add(id);
	}
	
	/*
	 * enqueueTask method: Adds task to queue.
	 * @param: task.
	 */
	public static void enqueueTask(String tsk) {
		order.add(tsk);
	}
	
	/*
	 * dequeueNumber method: Removes id from queue.
	 * @return: id.
	 */
	public static int dequeueNumber() {
		return line.remove();
	}
	
	/*
	 * dequeueTask method: Removes task from queue.
	 * @return: task.
	 */
	public static String dequeueTask() {
		return order.remove();
	}
	
	/*
	 * run method: Executes thread.
	 * @param: N/A.
	 */
	public void run() {
		// Keep processing customers until all of them have been served.
		while(!shouldStop) {
			// Beginning of first critical section.
			// Only one worker may signal ready at a time.
			wait(mutex1);
			// Worker provides his worker ID to customer.
			wrkID = wID;
			// Signal ready for next customer.
			signal(workerReady);
			// Wait for customer to provide order information.
			wait(takeOrder);
			// Grabs order information from customer.
			int number = dequeueNumber();
			String request = dequeueTask();
			// End of second critical section.
			// Ensures customer waits for worker to take order information before releasing first mutex.
			// Avoids race condition.
			signal(mutex2);
			// Service customer order.
			if(request == "mail a package") {
				wait(scaleInUse);
				System.out.println("Scales in use by postal worker " + wID + ".");
				execTask(pWorker[wID], number, request);
				signal(scaleInUse);
				System.out.println("Scales released by postal worker " + wID + ".");
			} else 
				execTask(pWorker[wID], number, request);
			System.out.println(pWorker[wID].getName() + " finished serving customer " + number + ".");
			// Signal customer of service completion.
			signal(taskComplete[number]);
			// Signals for next customer to move up in line.
			signal(readyToBeServed);
		}
		// End program when all customers have been served.
		System.exit(0);
	}
	
	/*
	 * Main method
	 * @param: args as String array.
	 */
	public static void main(String[] args) {
		// Create Poffice class objects.
		Poffice[] pOffice = new Poffice[MAXWRKS];
		// Create Customer class objects.
		Customer[] cx = new Customer[MAXCX];
		// Create one thread for each Poffice object.
		Thread[] pwThread = new Thread[MAXWRKS];
		// Create one thread for each Customer object.
		Thread[] cxThread = new Thread[MAXCX];
   		// Start worker threads.
   		for(int i = 0; i < MAXWRKS; ++i) {
			pOffice[i] = new Poffice(i);
			pwThread[i] = new Thread(pOffice[i]);
			pwThread[i].start();
		}
		// Start customer threads.
		for(int i = 0; i < MAXCX; ++i) {
			cx[i] = new Customer(i);
			cxThread[i] = new Thread(cx[i]);
			cxThread[i].start();
		}
	}

}
