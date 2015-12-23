package com.firefly.server.http2;

import java.util.List;

import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.AbstractHTTPHandler;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLEventHandler;
import com.firefly.net.tcp.ssl.SSLSession;

public class HTTP2ServerHandler extends AbstractHTTPHandler {

	private final ServerSessionListener listener;

	public HTTP2ServerHandler(HTTP2Configuration config, ServerSessionListener listener) {
		super(config);
		this.listener = listener;
	}

	@Override
	public void sessionOpened(final Session session) throws Throwable {
		if (config.isSecure()) {
			final SSLEngine sslEngine = sslContext.createSSLEngine();
			HTTP2ServerSSLHandshakeContext handshakeContext = new HTTP2ServerSSLHandshakeContext();
			handshakeContext.sslSession = new SSLSession(sslContext, sslEngine, session, false, new SSLEventHandler() {

				@Override
				public void handshakeFinished(SSLSession sslSession) {
					log.debug("server session {} SSL handshake finished", session.getSessionId());
					if (session.getAttachment() instanceof HTTP2ServerSSLHandshakeContext) {
						HTTP2ServerSSLHandshakeContext context = (HTTP2ServerSSLHandshakeContext) session.getAttachment();
						HttpVersion httpVersion = context.httpVersion;
						log.debug("server current HTTP version is {}", httpVersion);

						if (httpVersion == HttpVersion.HTTP_2) {
							session.attachObject(new HTTP2ServerConnection(config, session, sslSession, listener));
						} else {
							// TODO initialize HTTP 1.1 server connection
						}
					} else {
						log.error("HTTP2 server can not get the HTTP version of session {}", session.getSessionId());
						session.closeNow();
					}
				}
			}, new ALPN.ServerProvider() {

				@Override
				public void unsupported() {
					ALPN.remove(sslEngine);
				}

				@Override
				public String select(List<String> clientProtocols) {
					try {
						HTTP2ServerSSLHandshakeContext handshakeContext = (HTTP2ServerSSLHandshakeContext)session.getAttachment();
						for (String clientProtocol : clientProtocols) {
							for (String serverProtocol : protocols) {
								if (serverProtocol.equals(clientProtocol)) {
									log.debug("HTTP2 server selected protocol {}", clientProtocol);
									if (serverProtocol.equals("http/1.1")) {
										handshakeContext.httpVersion = HttpVersion.HTTP_1_1;
									} else {
										handshakeContext.httpVersion = HttpVersion.HTTP_2;
									}
									return clientProtocol;
								}
							}
						}

						handshakeContext.httpVersion = HttpVersion.HTTP_1_1;
						return "http/1.1";
					} finally {
						ALPN.remove(sslEngine);
					}
				}
			});
			
			session.attachObject(handshakeContext);
		} else {
			// TODO negotiate protocol without ALPN
			
		}
	}

}
