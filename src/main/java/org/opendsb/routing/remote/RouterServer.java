package org.opendsb.routing.remote;

import java.io.IOException;

import org.opendsb.routing.Router;

public abstract class RouterServer {

	protected Router localRouter;

	public RouterServer(Router localRouter) {
		super();
		this.localRouter = localRouter;
	}
	
	public abstract void start() throws IOException;
	
}
