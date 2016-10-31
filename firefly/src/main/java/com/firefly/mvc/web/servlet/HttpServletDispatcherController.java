package com.firefly.mvc.web.servlet;

import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpServletDispatcherController implements DispatcherController {

	private static Logger log = LoggerFactory.getLogger("firefly-system");

	protected WebContext webContext;
	
	public HttpServletDispatcherController(WebContext webContext) {
		this.webContext = webContext;
	}

	@Override
	public void dispatch(HttpServletRequest request, HttpServletResponse response) {
		String encoding = webContext.getEncoding();
		try {
			request.setCharacterEncoding(encoding);
		} catch (Throwable t) {
			log.error("dispatcher error", t);
		}
		response.setCharacterEncoding(encoding);

		StringBuilder uriBuilder = new StringBuilder(request.getRequestURI());
		uriBuilder.delete(0, request.getContextPath().length() + request.getServletPath().length());
		String servletURI = uriBuilder.length() <= 0 ? null : uriBuilder.toString();
		HandlerChain chain = webContext.match(request.getRequestURI(), servletURI);
		View v = null;
		try {
			v = chain.doNext(request, response, chain);
		} catch (Throwable e) {
			log.error("dispatcher error", e);
			if(!response.isCommitted()) {
				String msg = "Server internal error";
				SystemHtmlPage.responseSystemPage(request, response, webContext.getEncoding(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
			}
			return;
		}
		
		if(request.isAsyncStarted())
			return;
		
		if(v == null) {
			if(!response.isCommitted()) {
				String msg = request.getRequestURI() + " not found";
				SystemHtmlPage.responseSystemPage(request, response, webContext.getEncoding(), HttpServletResponse.SC_NOT_FOUND, msg);
			}
			return;
		}
		
		try {
			v.render(request, response);
		} catch (Throwable t) {
			log.error("dispatcher error", t);
			if(!response.isCommitted()) {
				String msg = "Server internal error";
				SystemHtmlPage.responseSystemPage(request, response, webContext.getEncoding(), 500, msg);
			}
		}
	}

}
