package com.bluebox;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

import static org.mockito.Mockito.mock;

public class FakeServletRequest implements HttpServletRequest {

    private String method;
    private String contextPath;
    private Map<String, String[]> parameterMap;
    private Map<String,Object> attributes;
    private Map<String,String[]> headers;
    private String pathInfo;

    public FakeServletRequest() {
        parameterMap = new HashMap<String, String[]>();
        attributes = new HashMap<String, Object>();
        headers = new HashMap<String, String[]>();
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
    }

    @Override
    public String getHeader(String name) {
        if (headers.containsKey(name))
            return headers.get(name)[0];
        return null;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return java.util.Collections.enumeration(Arrays.asList(headers.get(name)));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return java.util.Collections.enumeration(headers.keySet());
    }

    @Override
    public int getIntHeader(String name) {
      try {
          return Integer.parseInt(getHeader(name));
      }
      catch (Throwable t) {
          return 0;
      }
    }

    @Override
    public String getMethod() {
        return method;
    }

    public void setMethod(String m) {
        this.method = m;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getServletPath() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return "utf-8";
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getParameter(String name) {
        if (parameterMap.containsKey(name)) {
            return parameterMap.get(name)[0];
        }
        return null;
    }

    public void setParameter(String name, String[] value) {
        parameterMap.put(name, value);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return java.util.Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 80;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return "localhost";
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name,o);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return java.util.Collections.enumeration(Arrays.asList(Locale.getAvailableLocales()));
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 80;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return mock(ServletContext.class);
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return mock(AsyncContext.class);
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return startAsync();
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return mock(AsyncContext.class);
    }

    @Override
    public DispatcherType getDispatcherType() {
        return mock(DispatcherType.class);
    }

    public void setContextPath(String cp) {
        this.contextPath = cp;
    }
}
