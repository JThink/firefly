package com.firefly.mvc.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.mvc.web.support.ControllerMetaInfo;
import com.firefly.mvc.web.support.URLParser;
import com.firefly.utils.pattern.Pattern;

public class Resource {
	public static final String WILDCARD = "?";
	private static final String[] EMPTY = new String[0];
	private static String ENCODING;

	private final Map<String, Result> CONSTANT_URI;

	private String uri;
	private Pattern pattern;
	private ControllerMetaInfo controller;
	private ResourceSet children = new ResourceSet();

	public Resource(String encoding) {
		CONSTANT_URI = new HashMap<String, Result>();
		ENCODING = encoding;
	}

	private Resource(boolean root) {
		CONSTANT_URI = root ? new HashMap<String, Result>() : null;
	}

	public ControllerMetaInfo getController() {
		return controller;
	}

	public String getEncoding() {
		return ENCODING;
	}

	public void add(String uri, ControllerMetaInfo c) {
		if (uri.contains(WILDCARD)) {
			Resource current = this;
			List<String> list = URLParser.parse(uri);
			int max = list.size() - 1;

			for (int i = 0;; i++) {
				String name = list.get(i);
				if (i == max) {
					current = current.children.add(name, c);
					return;
				}

				current = current.children.add(name, null);
			}
		} else {
			char last = uri.charAt(uri.length() - 1);
			if (last != '/') {
				uri += "/";
			}
			Resource resource = new Resource(false);
			resource.uri = uri;
			resource.controller = c;
			Result result = new Result(resource, null);
			CONSTANT_URI.put(uri, result);
		}
	}

	public Result match(String uri) {
		char last = uri.charAt(uri.length() - 1);
		if (last != '/') {
			uri += "/";
		}

		Result ret = CONSTANT_URI.get(uri);
		if (ret != null)
			return ret;

		Resource current = this;
		List<String> list = URLParser.parse(uri);
		List<String> params = new ArrayList<String>();

		for (String i : list) {
			ret = current.children.match(i);
			if (ret == null)
				return ret;

			if (ret.params != null) {
				for (String p : ret.params)
					params.add(p);
			}

			current = ret.resource;
		}

		if (ret == null || ret.resource.controller == null)
			return null;

		if (params.size() > 0)
			ret.params = params.toArray(EMPTY);
		return ret;
	}

	public static class Result implements WebHandler {
		private final Resource resource;
		private String[] params;

		public Result(Resource resource, String[] params) {
			this.resource = resource;
			this.params = params;
		}

		public ControllerMetaInfo getController() {
			return resource.controller;
		}

		public String[] getParams() {
			return params;
		}

		@Override
		public View invoke(HttpServletRequest request, HttpServletResponse response) {
			if (!resource.controller.allowMethod(request.getMethod())) {
				notAllowMethodResponse(request, response);
				return null;
			}

			Object[] p = getController().getParameters(request, response, null, getParams());
			return getController().invoke(p);
		}

		private void notAllowMethodResponse(HttpServletRequest request, HttpServletResponse response) {
			response.setHeader("Allow", resource.controller.getAllowMethod());
			SystemHtmlPage.responseSystemPage(request, response, ENCODING, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"Only support " + resource.controller.getAllowMethod() + " method");
		}

		@Override
		public String toString() {
			return "Result [resource=" + resource + ", params=" + Arrays.toString(params) + "]";
		}

	}

	private class ResourceSet implements Iterable<Resource> {
		private Map<String, Resource> map = new HashMap<String, Resource>();
		private List<Resource> list = new LinkedList<Resource>();

		private boolean isVariable() {
			return list.size() > 0;
		}

		private Result match(String str) {
			Resource ret = map.get(str);
			if (ret != null)
				return new Result(ret, null);

			for (Resource res : list) {
				String[] p = res.pattern.match(str);
				if (p != null)
					return new Result(res, p);
			}

			return null;
		}

		private Resource add(String uri, ControllerMetaInfo c) {
			Resource resource = findByURI(uri);
			if (resource == null) {
				resource = new Resource(false);
				resource.uri = uri;

				if (uri.contains(WILDCARD)) {
					resource.pattern = Pattern.compile(resource.uri, WILDCARD);
					list.add(resource);
				} else {
					map.put(uri, resource);
				}
			}
			if (c != null)
				resource.controller = c;
			return resource;
		}

		private Resource findByURI(String uri) {
			Resource r = map.get(uri);
			if (r != null) {
				return r;
			} else {
				for (Resource res : list) {
					if (uri.equals(res.uri))
						return res;
				}
			}
			return null;
		}

		@Override
		public Iterator<Resource> iterator() {
			return new ResourceSetItr();
		}

		private class ResourceSetItr implements Iterator<Resource> {

			private Iterator<Resource> listItr = list.iterator();
			private Iterator<Entry<String, Resource>> mapItr = map.entrySet().iterator();

			@Override
			public boolean hasNext() {
				return mapItr.hasNext() || listItr.hasNext();
			}

			@Override
			public Resource next() {
				if (mapItr.hasNext())
					return mapItr.next().getValue();
				else
					return listItr.next();
			}

			@Override
			public void remove() {
				throw new RuntimeException("not implements this method!");
			}

		}

	}

	@Override
	public String toString() {
		return toString(" ", "");
	}

	private String toString(String l, String append) {
		StringBuilder s = new StringBuilder();
		s.append(append + uri + "(" + children.isVariable() + ")" + "\r\n");
		for (Resource r : children) {
			s.append(l + r.toString(l + " ", "├"));
		}
		return s.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Resource other = (Resource) obj;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

}
