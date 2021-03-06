/*
 * Copyright (c) 2004 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package gov.aps.jca.jni;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class JCAMonitorIT extends JCAIT {

    /**
     * Implementation of MonitorListener.
     */
    private class MonitorListenerImpl implements MonitorListener {
        /*
         * (non-Javadoc)
         * 
         * @see
         * gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.
         * MonitorEvent)
         */

        public volatile CAStatus status;
        public volatile DBR response;

        public synchronized void monitorChanged(MonitorEvent ev) {
            status = ev.getStatus();
            response = ev.getDBR();
            this.notifyAll();
        }

        public synchronized void reset() {
            status = null;
            response = null;
        }
    }

    /**
     * Simple monitor test.
     */
    @Test
    public void testMonitor() throws CAException {
        int mask = Monitor.VALUE | Monitor.ALARM;
        Monitor monitor = channel.addMonitor(mask);
        context.flushIO();
        assertTrue(channel == monitor.getChannel());
        assertTrue(context == monitor.getContext());
        assertEquals(0, monitor.getMonitorListeners().length);
        assertEquals(channel.getElementCount(), monitor.getCount());
        assertEquals(channel.getFieldType(), monitor.getType());
        assertEquals(mask, monitor.getMask());
        monitor.clear();
        // ... multiple clears allowed
        // CAJ code shoud prevent calling clear on the server
        monitor.clear();
    }

    /**
     * Simple monitor test.
     */
    @Test
    public void testMonitorResponses() throws CAException, TimeoutException, InterruptedException {
        channel.destroy();
        // this record increments value once per second
        channel = context.createChannel("record2");
        context.pendIO(15.0);

        Monitor monitor = null;
        MonitorListenerImpl listener = new MonitorListenerImpl();

        final int COUNT = 3;
        double lastVal = Double.MIN_VALUE;
        synchronized (listener) {
            monitor = channel.addMonitor(DBR_Double.TYPE, 1, Monitor.VALUE, listener);
            context.flushIO();

            for (int i = 0; i < COUNT; i++) {
                listener.wait(3000);

                assertEquals(CAStatus.NORMAL, listener.status);
                double val = ((double[]) listener.response.getValue())[0];
                if (lastVal != Double.MIN_VALUE)
                    assertEquals(1.0, val - lastVal, 0.0001);
                lastVal = val;
                assertEquals(CAStatus.NORMAL, listener.status);

                listener.reset();
            }
        }

        monitor.clear();
    }

    /**
     * No-transport test.
     */
    @Test
    public void testNoTransport() throws CAException {
        /* Monitor monitor = */ channel.addMonitor(Monitor.VALUE);
        context.flushIO();

        channel.destroy();

        /*
         * not possible to clear monitor twice try { monitor.clear();
         * fail("monitor destroyed without transport"); } catch
         * (IllegalStateException ise) { // ok }
         */

        try {
            channel.addMonitor(Monitor.VALUE);
            fail("monitor created when closed");
        } catch (IllegalStateException ise) {
            // ok
        }

    }

    /**
     * Register monitor listener.
     */
    @Test
    public void testMonitorListeners() throws CAException {
        Monitor monitor = channel.addMonitor(Monitor.VALUE);
        context.flushIO();

        assertEquals(0, monitor.getMonitorListeners().length);

        MonitorListener cl1 = new MonitorListenerImpl();
        monitor.addMonitorListener(cl1);
        assertEquals(1, monitor.getMonitorListeners().length);

        MonitorListener cl2 = new MonitorListenerImpl();
        monitor.addMonitorListener(cl2);
        assertEquals(2, monitor.getMonitorListeners().length);

        MonitorListener[] listeners = monitor.getMonitorListeners();
        assertTrue(listeners[0] == cl1);
        assertTrue(listeners[1] == cl2);

        monitor.removeMonitorListener(cl1);
        listeners = monitor.getMonitorListeners();
        assertEquals(1, listeners.length);
        assertTrue(listeners[0] == cl2);

        // removing twice (non registered) does not raise any error
        monitor.removeMonitorListener(cl1);
        listeners = monitor.getMonitorListeners();
        assertEquals(1, listeners.length);
        assertTrue(listeners[0] == cl2);

        monitor.removeMonitorListener(cl2);
        assertEquals(0, monitor.getMonitorListeners().length);

        monitor.clear();

        // listener passed via factory
        monitor = channel.addMonitor(Monitor.VALUE, cl1);
        assertEquals(1, monitor.getMonitorListeners().length);
        assertTrue(monitor.getMonitorListeners()[0] == cl1);
        assertTrue(monitor.getMonitorListener() == cl1);
    }

}
