package org.vanilladb.comm.protocols.totalorderappl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.process.ProcessStateListener;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.AllProcessesReady;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;

public class TotalOrderApplicationSession extends Session {
	private static Logger logger = Logger.getLogger(TotalOrderApplicationSession.class.getName());
	
	private ProcessStateListener procListener;
	private TotalOrderMessageListener totalMsgListener;
	private ProcessList processList;
	private boolean willRegisterSocket;
	
	TotalOrderApplicationSession(Layer layer,
			ProcessStateListener procListener,
			TotalOrderMessageListener totalMsgListener,
			ProcessList processList, boolean willRegisterSocket) {
		super(layer);
		
		this.procListener = procListener;
		this.totalMsgListener = totalMsgListener;
		this.processList = processList;
		this.willRegisterSocket = willRegisterSocket;
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof AllProcessesReady)
			handleAllProcessesReady((AllProcessesReady) event);
		else if (event instanceof RegisterSocketEvent)
			handleRegisterSocketEvent((RegisterSocketEvent) event);
		else if (event instanceof FailureDetected)
			handleFailureDetected((FailureDetected) event);
		else if (event instanceof TotalOrderMessage)
			handleTotalOrderMessage((TotalOrderMessage) event);
	}
	
	private void handleChannelInit(ChannelInit init) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received ChannelInit");
		
		try {
			// ChannelInit must go() before inserting other events
			init.go();
			
			if (willRegisterSocket) {
				// Register socket for TCP connection
				RegisterSocketEvent rse = new RegisterSocketEvent(init.getChannel(),
						Direction.DOWN, this);
				rse.localHost = processList.getSelfProcess().getAddress().getAddress();
				rse.port = processList.getSelfProcess().getAddress().getPort();
				rse.init();
				rse.go();
				
				if (logger.isLoggable(Level.INFO))
					logger.info("Socket registration request sent.");
			}
			
			// Send process init
			ProcessListInit processInit = new ProcessListInit(init.getChannel(),
					this, new ProcessList(processList));
			processInit.init();
			processInit.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleAllProcessesReady(AllProcessesReady event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received AllProcessesReady");
		
		// Set all process states to correct
		for (int i = 0; i < processList.getSize(); i++) {
			processList.getProcess(i).setState(ProcessState.CORRECT);
		}
		
		// Notify the listener
		procListener.onAllProcessesReady();
	}
	
	private void handleRegisterSocketEvent(RegisterSocketEvent event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received RegisterSocket");
		
		if (event.error) {
			if (logger.isLoggable(Level.SEVERE))
				logger.severe(event.getErrorDescription());
			System.exit(2);
		} else {
			if (logger.isLoggable(Level.INFO))
				logger.info(String.format("Socket registration completed. (%s:%d)",
						event.localHost, event.port));
		}
	}
	
	private void handleFailureDetected(FailureDetected event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received FailureDetected (failed id = " +
					event.getFailedProcessId() + ")");
		
		// Set the process state as failed
		processList.getProcess(event.getFailedProcessId()).setState(ProcessState.FAILED);
		
		// Notify the listener
		procListener.onProcessFailed(event.getFailedProcessId());
	}
	
	private void handleTotalOrderMessage(TotalOrderMessage event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received TotalOrderMessage (serial number: " +
					event.getSerialNumber() + ")");
		
		// Notify the listener
		totalMsgListener.onRecvTotalOrderMessage(event.getSerialNumber(),
				event.getMessage());
	}
}
