import java.util.LinkedList;

/**
 * Creates a queue that executes thread tasks with a pool of workers
 * 
 * @author Robert Macaibay
 *
 */
public class WorkQueue {

	//pool of worker threads that will wait in the background until work is available
	private final PoolWorker[] workers;

	//queue of pending work requests
	private final LinkedList<Runnable> queue;

	//used to signal the queue should be shutdown
	private volatile boolean shutdown;

	//the default number of threads to use when not specified
	public static final int DEFAULT = 5;
	
	//number of pending work
	private int pending;

	/**
	 * Starts a work queue with the default number of threads.
	 *
	 * @see #WorkQueue(int)
	 */
	public WorkQueue() {
		this(DEFAULT);
	}

	/**
	 * Starts a work queue with the specified number of threads.
	 *
	 * @param threads - number of worker threads; should be greater than 1
	 */
	public WorkQueue(int threads) {
		this.queue = new LinkedList<Runnable>();
		this.workers = new PoolWorker[threads];

		this.shutdown = false;
		this.pending = 0;

		// start the threads so they are waiting in the background
		for (int i = 0; i < threads; i++) {
			workers[i] = new PoolWorker();
			workers[i].start();
		}
	}

	/**
	 * Adds a work request to the queue. A thread will process this request when
	 * available.
	 *
	 * @param r - work request (in the form of a {@link Runnable} object)
	 */
	public void execute(Runnable r) {
		synchronized (queue) {
			queue.addLast(r);
			incrementPending();
			queue.notifyAll();
		}
	}

	/**
	 * Waits for all pending work to be finished.
	 */
	public void finish() {
		try {
			synchronized (queue) {
				while (pending > 0) {
					queue.wait();
				}
			}
		} catch (RuntimeException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Asks the queue to shutdown. Any unprocessed work will not be finished,
	 * but threads in-progress will not be interrupted.
	 */
	public void shutdown() {
		// safe to do unsynchronized due to volatile keyword
		shutdown = true;
		
		synchronized (queue) {
			queue.notifyAll();
		}
	}

	/**
	 * Returns the number of worker threads being used by the work queue.
	 *
	 * @return number of worker threads
	 */
	public int size() {
		return workers.length;
	}

	/**
	 * Waits until work is available in the work queue. When work is found, will
	 * remove the work from the queue and run it. If a shutdown is detected,
	 * will exit instead of grabbing new work from the queue. These threads will
	 * continue running in the background until a shutdown is requested.
	 */
	private class PoolWorker extends Thread {
		
		@Override
		public void run() {
			Runnable r = null;
			
			while (true) {
				synchronized (queue) {
					while (queue.isEmpty() && !shutdown) {
						try {
							queue.wait();
						}
						catch (InterruptedException ex) {
							System.err.println("Warning: Work queue interrupted.");
							Thread.currentThread().interrupt();
						}
					}

					// exit while for one of two reasons:
					// (a) queue has work, or (b) shutdown has been called

					if (shutdown) {
						break;
					}
					else {
						r = queue.removeFirst();
					}
				}

				try {
					r.run();
				}
				catch (RuntimeException ex) {
					// catch runtime exceptions to avoid leaking threads
					System.err.println("Warning: Work queue encountered an " + "exception while running.");
					ex.printStackTrace();
				}
				
				decrementPending();
			}
		}
	}
	
	/**
	 * Increments number of pending work
	 */
	private void incrementPending() {
		synchronized (queue) {
			pending++;
		}
	}
	
	/**
	 * Decrements number of pending work
	 */
	private void decrementPending() {
		synchronized (queue) {
			pending--;
			
			if (pending <= 0)
				queue.notifyAll();
		}
	}
}
