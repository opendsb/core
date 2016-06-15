package org.dsb;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class ConcurrencyTest {
	
	
	@Test
	public void test() throws Exception {
		
		ExecutorService service = Executors.newFixedThreadPool(1);
		
		service.submit(() -> {
			try {
			
				System.out.println("I'm executing a task...");
				
				threadInfo("Task 1");
				
				Thread.sleep(1500);
				
				System.out.println("Time to launch a new task...");
				
				service.submit(() -> {
					try {
						System.out.println("I'm trying to execute a new task from within a task!");
						threadInfo("Task 2");
						Thread.sleep(2000);
						threadInfo("Task 2");
						System.out.println("Task 2 done");
					} catch(Exception e) {
						e.printStackTrace();
					}
				});

				System.out.println("Task 2  launched back to processing ...");
				
				threadInfo("Task 1");
				
				Thread.sleep(5000);
				
				threadInfo("Task 1");
				
				System.out.println("Task 1 done.");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		Thread.sleep(10000);
	}

	
	private void threadInfo(String taskName) {
		System.out.println("Task '" + taskName + "'. Thread hash '" + Thread.currentThread().hashCode() + "', thread name: '" + Thread.currentThread().getName() + "'");
	}
}
