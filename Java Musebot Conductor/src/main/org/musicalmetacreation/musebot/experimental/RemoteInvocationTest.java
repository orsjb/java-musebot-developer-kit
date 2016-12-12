package org.musicalmetacreation.musebot.experimental;

import org.musicalmetacreation.musebot.conductor.Host;

public class RemoteInvocationTest {

	public static void main(String[] args) {
		Host h = new Host("boing.local", "ollie", "/Users/ollie/Documents");
		h.getMusebotList();
	}
}
