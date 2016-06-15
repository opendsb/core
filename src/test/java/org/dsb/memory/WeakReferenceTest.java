package org.dsb.memory;

import java.util.function.Consumer;

import org.dsb.memory.utils.ServiceAndHandler;
import org.dsb.memory.utils.StubRouter;
import org.dsb.memory.utils.Subscription;
import org.junit.Test;

public class WeakReferenceTest {
	
	
	@Test
	public void subscriptionCleanUpOnGarbageCollection() throws Exception {
		
		System.out.println("Testing on collection");
		
		String topic = "someTopic";
		
		StubRouter router = new StubRouter();
		
		Consumer<String> tmpLambda = (s -> System.out.println("Temporary consumer reached with msg '" + s + "'."));
		
		Consumer<String> permLambda = (s -> System.out.println("Permanent consumer reached with msg '" + s + "'."));
		
		System.out.println(tmpLambda.getClass().getSimpleName());
		
		Subscription subscriptionA = router.subscribe(topic, tmpLambda);
		
		Subscription subscriptionB = router.subscribe(topic, permLambda);
		
		router.routeMessage(topic, "LA LA LA");
		
		subscriptionA = null;
		
		System.gc();
		
		router.routeMessage(topic, "REKT, REKT, REKT");
	}
	
	@Test
	public void subscriptionCleanUpOnCancel() throws Exception {
		
		System.out.println("Testing on cancel");
		
		String topic = "someTopic";
		
		StubRouter router = new StubRouter();
		
		Consumer<String> tmpLambda = (s -> System.out.println("Temporary consumer reached with msg '" + s + "'."));
		
		Consumer<String> permLambda = (s -> System.out.println("Permanent consumer reached with msg '" + s + "'."));
		
		System.out.println(tmpLambda.getClass().getSimpleName());
		
		Subscription subscriptionA = router.subscribe(topic, tmpLambda);
		
		Subscription subscriptionB = router.subscribe(topic, permLambda);
		
		router.routeMessage(topic, "LA LA LA");
		
		subscriptionA.cancel();
		
		router.routeMessage(topic, "REKT, REKT, REKT");
	}

	@Test
	public void subscriptionCleanUpWithServiceAndHandler() throws Exception {
		
		System.out.println("Testing on service and handler");
		
		String topic = "someTopic";
		
		StubRouter router = new StubRouter();
		
		Consumer<String> permLambda = (s -> System.out.println("Permanent consumer reached with msg '" + s + "'."));
		
		ServiceAndHandler serviceAndHandler = new ServiceAndHandler(topic, router);
		
		serviceAndHandler.subscribe();
		
		Subscription subscription = router.subscribe(topic, permLambda);
		
		router.routeMessage(topic, "LA LA LA");
		
		serviceAndHandler = null;
		
		System.gc();
		
		router.routeMessage(topic, "REKT, REKT, REKT");
	}
}
