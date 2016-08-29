package com.firefly.net.support.wrap.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.firefly.net.Client;
import com.firefly.net.Decoder;
import com.firefly.net.Encoder;
import com.firefly.net.tcp.aio.AsynchronousTcpClient;
import com.firefly.utils.concurrent.FuturePromise;

public class SimpleTcpClient {
	private String host;
	private int port;
	private SimpleTcpClientHandler handler;

	private Map<Integer, ConnectionInfo> connectionInfo = new ConcurrentHashMap<Integer,ConnectionInfo>();
	private Client client;
	private AtomicInteger sessionId = new AtomicInteger(0);

	public SimpleTcpClient(String host, int port, Decoder decoder, Encoder encoder, SimpleTcpClientHandler handler) {
		this.host = host;
		this.port = port;
		this.handler = handler;
		this.handler.setConnectionInfo(connectionInfo);
		this.client = new AsynchronousTcpClient(decoder, encoder, handler);
	}

	public Future<TcpConnection> connect() {
		return connect(0);
	}
	
	public Future<TcpConnection> connect(final long timeout) {
		final long t = timeout <= 0 ? 5000L : timeout;
		final FuturePromise<TcpConnection> future = new FuturePromise<>();
		
		connect(t, new SessionOpenedCallback(){

			@Override
			public void sessionOpened(TcpConnection connection) {
				future.succeeded(connection);
			}
		});
		return future;
	}
	
	public void connect(long timeout, SessionOpenedCallback callback) {
		int id = sessionId.getAndIncrement();
		ConnectionInfo info = new ConnectionInfo();
		info.callback = callback;
		info.timeout = timeout;
		connectionInfo.put(id, info);
		client.connect(host, port, id);
	}

	public void shutdown() {
		client.stop();
	}
}
