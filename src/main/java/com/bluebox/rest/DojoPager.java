package com.bluebox.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class DojoPager {
    private static final Logger log = Logger.getAnonymousLogger();
    private final List<Boolean> ascending = new ArrayList<Boolean>();
    private final List<String> orderBy = new ArrayList<String>();
    private int first, last;
    private final HttpServletRequest request;

    public DojoPager(HttpServletRequest req, String defaultOrder) {
        this.request = req;
        String n;
        // handle the form : /FooObject/?foo=value1&sortBy=+foo,-bar
        // as per http://dojotoolkit.org/reference-guide/1.10/dojo/store/JsonRest.html
        if (req.getParameter("sortBy") != null) {
            log.info("0:" + req.getParameter("sortBy"));
            log.warning("This code has never been tested!");
            String sortString = req.getParameter("sortBy");
            StringTokenizer tok = new StringTokenizer(sortString, ",");
            while (tok.hasMoreTokens()) {
                sortString = tok.nextToken();
                if (sortString.startsWith("+"))
                    ascending.add(true);
                else
                    ascending.add(false);
                orderBy.add(sortString.substring(1));
            }
        } else {
            // this one happens only on Liberty
            if (req.getParameterMap().size() == 0) {
                n = req.getQueryString() + "";
                if (n.contains("sort")) {
                    String sortString = n.substring(n.indexOf('(') + 1, n.indexOf(')'));
                    StringTokenizer tok = new StringTokenizer(sortString, ",");
                    while (tok.hasMoreTokens()) {
                        sortString = tok.nextToken();
                        if (sortString.startsWith("-")) {
                            ascending.add(false);
                        } else {
                            ascending.add(true);
                        }
                        orderBy.add(sortString.substring(1));
                    }
                }
            } else {
                // handle the form /FooObject/?foo=value1&sort(+foo,-bar)
                for (Enumeration<String> names = req.getParameterNames(); names.hasMoreElements(); ) {
                    n = names.nextElement();
                    if (n.contains("sort")) {
                        String sortString = n.substring(n.indexOf('(') + 1, n.indexOf(')'));
                        StringTokenizer tok = new StringTokenizer(sortString, ",");
                        while (tok.hasMoreTokens()) {
                            sortString = tok.nextToken();
                            if (sortString.startsWith("-")) {
                                ascending.add(false);
                            } else {
                                ascending.add(true);
                            }
                            orderBy.add(sortString.substring(1));
                        }
                    }
                }
            }
        }
        // process the paging params "Range: items=0-24"
        String contentHeader = req.getHeader("Range");
        setFirst(getStart(contentHeader));
        setLast(getEnd(contentHeader));

        if (orderBy.size() == 0) {
            orderBy.add(defaultOrder);
            ascending.add(false);
        }
    }

    public void setRange(HttpServletResponse resp, long totalCount) {
        resp.setHeader("Content-Range", "items " + getFirst() + "-" + getLast() + "/" + totalCount);//Content-Range: items 0-24/66
    }

    public boolean isAscending(int index) {
        return ascending.get(index);
    }

    public List<String> getOrderBy() {
        return orderBy;
    }

    public int getFirst() {
        return first;
    }

    public void setFirst(int first) {
        this.first = first;
    }

    public int getFirst(int defaultStart) {
        if (request.getHeader("Range") != null)
            return first;
        else
            return defaultStart;
    }

    public int getLast() {
        return last;
    }


    public void setLast(int last) {
        this.last = last;
    }

    public int getCount() {
        return getLast() - getFirst() + 1;
    }

    public int getCount(int defaultCount) {
        if (request.getHeader("Range") != null)
            return getCount();
        else
            return defaultCount;
    }

    private int getStart(String contentHeader) {
        if (contentHeader != null) {
            try {
                // items=0-24, return 0
                int s = contentHeader.indexOf('=') + 1;
                int e = contentHeader.indexOf("-", s);
                return Integer.parseInt(contentHeader.substring(s, e));
            } catch (Throwable t) {
                log.warning("Invalid Content header :" + contentHeader);
            }
        }
        return 0;
    }

    private int getEnd(String contentHeader) {
        if (contentHeader != null) {
            try {
                // items=0-24, return 24
                int s = contentHeader.indexOf('-') + 1;
                int e = contentHeader.length();
                return Integer.parseInt(contentHeader.substring(s, e));
            } catch (Throwable t) {
                log.warning("Invalid Content header :" + contentHeader);
            }
        }
        return 250;//Integer.MAX_VALUE;
    }

}
