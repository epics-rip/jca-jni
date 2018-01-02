package gov.aps.jca.jni;

import static org.junit.Assert.assertEquals;

import java.util.prefs.Preferences;

import org.junit.After;
import org.junit.Before;

import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;

public class JCAIT {

	static final String JNIType = "JNIType";

	/**
	 * Context to be tested.
	 */
	protected Context context;

	/**
	 * Channel to be tested.
	 */
	protected Channel channel;

	/*
	 * @see TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {
		Preferences prefs = Preferences.userNodeForPackage(JCAIT.class);
		prefs.put(JNIType, JCALibrary.JNI_SINGLE_THREADED);
		context = JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
		channel = context.createChannel("record1");
		context.pendIO(5.0);
		assertEquals(Channel.CONNECTED, channel.getConnectionState());
		channel.put(12.34);
		context.pendIO(5.0);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	@After
	public void tearDown() throws Exception {
		context.dispose();
		context = null;
	}

}
