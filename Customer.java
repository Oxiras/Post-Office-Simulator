import java.util.Random;									// Needed for Random class.
import java.util.concurrent.Semaphore;								// Needed for Semaphore class.

/*
 * PROGRAM: Customer.java
 * @author: Moustapha Dieng
 * This program simulates the act of customer going into
 * a postal office and having their orders processed by 
 * a postal worker. Threads are used to have the customers'
 * actions independent of each other and running in parralel.
 **/
public class Customer implements Runnable {

	private final String[] tasks = {"buy stamps", "mail a letter", "mail a package"};	// To hold possible orders.
	private final Thread[] customer = new Thread[Poffice.MAXCX];				// Declare customer threads.
	private int cxID;									// To hold customer IDs.				
	private String task;									// To hold customer request.
	
	/*
	 * Constructor: Initializes customer thread.
	 * @param: thread number.
	 */
	public Customer(int cxID) {
		this.cxID = cxID;
		customer[cxID] = new Thread("Customer " + cxID);
		Poffice.taskComplete[cxID] = new Semaphore(0, true);
		task = assignTask();
		System.out.println(customer[cxID].getName() + " created.");
	}
	
	/*
	 * stop method: Joins each customer thread and keeps count of how many customer processed.
	 * @param: N/A.
	 */
	private void stop() {
		try {
			// Signals customr out of post office.
			Poffice.signal(Poffice.enterPostOffice);
			// Increment count of customer processed.
			Poffice.custProcessed++;
			System.out.println(customer[cxID].getName() + " leaves post office.");
			customer[cxID].join();
			System.out.println("Joined customer " + cxID + ".");
			if(Poffice.custProcessed == Poffice.MAXCX)
				Poffice.shouldStop = true;
		}
		catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
	}
	
	/*
	 * assignTask method: Randomly selects one of the pre-defined tasks.
	 * @return: task.
	 */
	private String assignTask() {
		int rand = new Random().nextInt(this.tasks.length);
		return tasks[rand];
	}
	
	/*
	 * run method: Executes thread.
	 * @param: N/A.
	 */
	public void run() {
		// Allows pre-defined amount of customers to enter post office.
		Poffice.wait(Poffice.enterPostOffice);
		System.out.println(customer[cxID].getName() + " enters post office.");
		// Allows pre-defined amount of customers to be ready for service.
		Poffice.wait(Poffice.readyToBeServed);
		// Wait for worker to be available.
		Poffice.wait(Poffice.workerReady);
		System.out.println(customer[cxID].getName() + " asks postal worker " + Poffice.wrkID + " to " + task + ".");
		// Provide order information to worker.
		Poffice.enqueueNumber(cxID);
		Poffice.enqueueTask(task);
		// Signals for worker to take order.
		Poffice.signal(Poffice.takeOrder);
		// Beginning of second critical section.
		// Customers wait for worker to take information before releasing first mutex.
		// Avoids race condition.
		Poffice.wait(Poffice.mutex2);
		// End of first critical section.
		// Next worker is free to enter and set its own ID.
		Poffice.signal(Poffice.mutex1);
		// Wait for service completion.
		Poffice.wait(Poffice.taskComplete[cxID]);
		switch(task) {
			case "buy stamps":
				System.out.println(customer[cxID].getName() + " finished buying stamps.");
				break;
			case "mail a letter":
				System.out.println(customer[cxID].getName() + " finished mailing a letter.");
				break;
			case "mail a package":
				System.out.println(customer[cxID].getName() + " finished mailing a package.");
				break;
		}
		// Wait for current thread to finish executing.
		stop();
	}
}
