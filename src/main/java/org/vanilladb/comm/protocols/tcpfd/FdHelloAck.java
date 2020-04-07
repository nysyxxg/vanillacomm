package org.vanilladb.comm.protocols.tcpfd;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;

public class FdHelloAck extends SendableEvent {
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public FdHelloAck() {
		super();
	}
	
	public FdHelloAck(Channel channel, Session src)
			throws AppiaEventException {
		super(channel, Direction.DOWN, src);
	}
}
