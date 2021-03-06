package tahrir.io.net.udp;

import java.net.InetAddress;
import java.security.interfaces.*;

import org.testng.Assert;
import org.testng.annotations.Test;

import tahrir.io.crypto.TrCrypto;
import tahrir.io.net.*;
import tahrir.io.net.TrNetworkInterface.TrMessageListener;
import tahrir.io.net.TrNetworkInterface.TrSentListener;
import tahrir.io.net.TrNetworkInterface.TrSentReceivedListener;
import tahrir.io.net.udpV1.*;
import tahrir.io.net.udpV1.UdpNetworkInterface.Config;
import tahrir.tools.*;
import tahrir.tools.ByteArraySegment.ByteArraySegmentBuilder;

import com.google.common.base.Function;

public class UdpNetworkInterfaceTest {
	@Test
	public void simpleSendReceiveTest() throws Exception {
		final Config conf1 = new Config();
		conf1.listenPort = 3956;
		conf1.maxUpstreamBytesPerSecond = 1024;

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp1 = TrCrypto.createRsaKeyPair();

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp2 = TrCrypto.createRsaKeyPair();

		final UdpNetworkInterface i1 = new UdpNetworkInterface(conf1, kp1);

		final Config conf2 = new Config();
		conf2.listenPort = 3957;
		conf2.maxUpstreamBytesPerSecond = 1024;
		final UdpNetworkInterface i2 = new UdpNetworkInterface(conf2, kp2);
		final UdpRemoteAddress ra2 = new UdpRemoteAddress(InetAddress.getLocalHost(), conf2.listenPort);

		final byte[] msg_ = new byte[900];
		for (int x = 0; x < msg_.length; x++) {
			msg_[x] = (byte) (x % 256);
		}

		final ByteArraySegment msg = new ByteArraySegment(msg_);

		final boolean[] receivedFlag = new boolean[2];

		i2.registerListener(new TrMessageListener() {

			public void received(final TrNetworkInterface iFace, final TrRemoteAddress sender,
					final ByteArraySegment message) {
				receivedFlag[0] = true;
				Assert.assertEquals(message, msg);

			}
		});

		i1.sendTo(ra2, msg, new TrSentListener() {

			public void sent() {
				receivedFlag[1] = true;
			}

			public void failure() {
			}
		}, 0);

		Thread.sleep(200);

		Assert.assertTrue(receivedFlag[0]);
		Assert.assertTrue(receivedFlag[1]);

		i1.shutdown();
		i2.shutdown();
	}

	@Test
	public void simpleReliableMessageSend() throws Exception {
		final Config conf1 = new Config();
		conf1.listenPort = 3956;
		conf1.maxUpstreamBytesPerSecond = 1024;

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp1 = TrCrypto.createRsaKeyPair();

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp2 = TrCrypto.createRsaKeyPair();

		final UdpNetworkInterface i1 = new UdpNetworkInterface(conf1, kp1);

		final Config conf2 = new Config();
		conf2.listenPort = 3957;
		conf2.maxUpstreamBytesPerSecond = 1024;
		final UdpNetworkInterface i2 = new UdpNetworkInterface(conf2, kp2);

		final UdpRemoteAddress ra1 = new UdpRemoteAddress(InetAddress.getLocalHost(), conf1.listenPort);
		final UdpRemoteAddress ra2 = new UdpRemoteAddress(InetAddress.getLocalHost(), conf2.listenPort);

		final TrMessageListener noopListener = new TrMessageListener() {

			public void received(final TrNetworkInterface iFace, final TrRemoteAddress sender,
					final ByteArraySegment message) {
			}

		};

		final Called receivedSuccessfully = new Called();

		final ByteArraySegmentBuilder msgBuilder = ByteArraySegment.builder();

		for (int x = 0; x < 100; x++) {
			msgBuilder.writeByte(33);
		}

		final ByteArraySegment sentMessage = msgBuilder.build();

		final TrMessageListener listener = new TrMessageListener() {

			public void received(final TrNetworkInterface iFace, final TrRemoteAddress sender,
					final ByteArraySegment message) {
				Assert.assertEquals(message, sentMessage);
				receivedSuccessfully.called = true;
			}
		};
		final Called connected1 = new Called();
		final Called disconnected1 = new Called();
		final TrRemoteConnection one2two = i1.connect(ra2, kp2.a, noopListener, connected1,
				disconnected1, false);

		final Called connected2 = new Called();
		final Called disconnected2 = new Called();
		final TrRemoteConnection two2one = i2.connect(ra1, kp1.a, listener, connected2,
				disconnected2, false);

		for (int x = 0; x < 100; x++) {
			if (connected1.called && connected2.called) {
				break;
			}
			Thread.sleep(100);
		}

		final Called ackReceived = new Called();

		one2two.send(sentMessage, 1, new TrSentReceivedListener() {

			public void sent() {
				System.out.println("Sent successfully");
			}

			public void failure() {
				Assert.fail();
			}

			public void received() {
				ackReceived.called = true;
				System.out.println("Received successfully");
			}
		});

		for (int x = 0; x < 10; x++) {
			if (ackReceived.called && receivedSuccessfully.called) {
				break;
			}
			Thread.sleep(500);
		}

		Assert.assertTrue(ackReceived.called);
		Assert.assertTrue(receivedSuccessfully.called);
	}

	@Test
	public void longReliableMessageSend() throws Exception {
		final Config conf1 = new Config();
		conf1.listenPort = 3986;
		conf1.maxUpstreamBytesPerSecond = 1024;

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp1 = TrCrypto.createRsaKeyPair();

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp2 = TrCrypto.createRsaKeyPair();

		final UdpNetworkInterface i1 = new UdpNetworkInterface(conf1, kp1);

		final Config conf2 = new Config();
		conf2.listenPort = 3987;
		conf2.maxUpstreamBytesPerSecond = 1024;
		final UdpNetworkInterface i2 = new UdpNetworkInterface(conf2, kp2);

		final UdpRemoteAddress ra1 = new UdpRemoteAddress(InetAddress.getLocalHost(), conf1.listenPort);
		final UdpRemoteAddress ra2 = new UdpRemoteAddress(InetAddress.getLocalHost(), conf2.listenPort);

		final TrMessageListener noopListener = new TrMessageListener() {

			public void received(final TrNetworkInterface iFace, final TrRemoteAddress sender,
					final ByteArraySegment message) {
			}

		};

		final Called receivedSuccessfully = new Called();

		final ByteArraySegmentBuilder msgBuilder = ByteArraySegment.builder();

		for (int x = 0; x < 2000; x++) {
			msgBuilder.writeByte(33);
		}

		final ByteArraySegment sentMessage = msgBuilder.build();

		final TrMessageListener listener = new TrMessageListener() {

			public void received(final TrNetworkInterface iFace, final TrRemoteAddress sender,
					final ByteArraySegment message) {
				Assert.assertEquals(message, sentMessage);
				receivedSuccessfully.called = true;
			}
		};
		final Called connected1 = new Called();
		final Called disconnected1 = new Called();
		final TrRemoteConnection one2two = i1.connect(ra2, kp2.a, noopListener, connected1,
				disconnected1, false);

		final Called connected2 = new Called();
		final Called disconnected2 = new Called();
		final TrRemoteConnection two2one = i2.connect(ra1, kp1.a, listener, connected2,
				disconnected2, false);

		for (int x = 0; x < 100; x++) {
			if (connected1.called && connected2.called) {
				break;
			}
			Thread.sleep(100);
		}

		final Called ackReceived = new Called();

		one2two.send(sentMessage, 1, new TrSentReceivedListener() {

			public void sent() {
				System.out.println("Sent successfully");
			}

			public void failure() {
				Assert.fail();
			}

			public void received() {
				ackReceived.called = true;
				System.out.println("Received successfully");
			}
		});

		for (int x = 0; x < 10; x++) {
			if (ackReceived.called && receivedSuccessfully.called) {
				break;
			}
			Thread.sleep(500);
		}

		Assert.assertTrue(ackReceived.called);
		Assert.assertTrue(receivedSuccessfully.called);
	}

	public static class Called implements Runnable, Function<TrRemoteConnection, Void> {
		public volatile boolean called = false;

		public void run() {
			called = true;
		}

		public Void apply(final TrRemoteConnection arg0) {
			called = true;
			return null;
		}

	}
}
