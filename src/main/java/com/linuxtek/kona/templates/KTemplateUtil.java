/*
 * Copyright (C) 2011 LinuxTek, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.templates;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.linuxtek.kona.util.KClassUtil;
import com.linuxtek.kona.util.KDateUtil;
import com.linuxtek.kona.util.KStringUtil;

/**
 * Helper class for use inside of Velocity templates. 
 * This object will be exposed as $KUtil inside templates.
 */
public class KTemplateUtil {
    private static Logger logger = Logger.getLogger(KTemplateUtil.class);
    
    public static String toString(Object obj) {
        if (obj == null) {
            return "[null]";
        }
        return KClassUtil.toString(obj);
    }

	public static String year() {
		Integer year = KDateUtil.getYear(new Date());
		return year.toString();
	}
	
    public static String createLink(String baseUrl, String url) {
        if (baseUrl == null) baseUrl = "";
        if (url == null) url = "/";

        if (baseUrl.endsWith("/")) {
            baseUrl = KStringUtil.chop(baseUrl);
        }

        if (!url.startsWith("/")) {
            url = "/" + url;
        }

        String link = baseUrl + url;

        logger.debug("LINK: " + link);

        return link;
    }

    public static String format(Date date) {
        String f = "MM/dd/yyyy";
        return format(date, f);
    }

    public static String format(Date date, String format) {
        return format(date, format, null);
    }
    
    public static String format(Date date, String format, String timeZone) {
        if (date == null) return "";
        
        Locale locale = Locale.getDefault();
        
        if (timeZone == null) {
            timeZone = "America/New_York";
        }
        
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        return KDateUtil.format(date, format, locale, tz);
    }
    
    public static String format(BigDecimal bd) {
        //Locale locale = new Locale("en_US");
        return format(bd, Locale.US);
    }
    
    public static String format(BigDecimal bd, Locale locale) {
        if (bd == null)
            return (null);

        double d = bd.doubleValue();
        NumberFormat form = NumberFormat.getCurrencyInstance(locale);
        return (form.format(d));
    }


}
