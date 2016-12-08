/*
 * Copyright (C) 2011 LinuxTek, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.templates;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
//import org.apache.velocity.tools.ToolContext;
//import org.apache.velocity.tools.ToolManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

import com.linuxtek.kona.premailer.KPremailer;
import com.linuxtek.kona.util.KStringUtil;

/**
 * Wrapper class to make working with Velocity templates a little easier.
 * 
 * @version 1.0
 * @since 1.0
 */

public class KTemplate {
	private static Logger logger = Logger.getLogger(KTemplate.class);
    
    public static boolean LOG_ENABLED = true;
    public static String LOG_PATH = "/tmp/velocity.log";

	private String templateResource = null;
    private String stringTemplate = null;
	private VelocityEngine ve = null;
	private VelocityContext context = null;
	//private ToolContext context = null;
	private String baseUrl = "";
	private String filesBaseUrl = "";
	
	private boolean logEnabled = LOG_ENABLED;
	private String logPath = LOG_PATH;
    
	private boolean inlineCss = false;
    
	public KTemplate() throws KTemplateException {
		try {
			//ToolManager manager = new ToolManager();
			//context = manager.createContext();
			context = new VelocityContext();
			addContextObject("X_BASE_URL__", baseUrl);
			addContextObject("X_FILES_BASE_URL__", filesBaseUrl);
		} catch (Exception e) {
			throw new KTemplateException(e);
		}
	}
	
	public KTemplate(boolean logEnabled) throws KTemplateException {
		this();
		this.logEnabled = logEnabled;
	}

	public KTemplate(String templateResource) throws KTemplateException {
		this(templateResource, true);
	}
	
	public KTemplate(String templateResource, boolean logEnabled) throws KTemplateException {
        this(logEnabled);

		try {
			this.templateResource = templateResource;
			this.ve = new VelocityEngine();
			init(ve);
		} catch (Exception e) {
			throw new KTemplateException(e);
		}
	}
    
	public KTemplate(HttpServletRequest req, String templatePath, String templateResource) throws KTemplateException {
		this(req, templatePath, templateResource, true);
	}
	
	public KTemplate(HttpServletRequest req, String templatePath, String templateResource, boolean logEnabled) throws KTemplateException {
        this(logEnabled);
		try {
			this.templateResource = templateResource;
			this.ve = new VelocityEngine();
			initFileResourceLoader(req, templatePath);
		} catch (Exception e) {
			throw new KTemplateException(e);
		}
	}
    
	public KTemplate(String rootPath, String templateResource) throws KTemplateException {
		this(rootPath, templateResource, true);
	}
	
	public KTemplate(String rootPath, String templateResource, boolean logEnabled) throws KTemplateException {
		this(logEnabled);
		try {
			this.templateResource = templateResource;
			this.ve = new VelocityEngine();
			initFileResourceLoader(rootPath);
		} catch (Exception e) {
			throw new KTemplateException(e);
		}
	}
    
	
    public KTemplate(String templateResource, Map<String,Object> params, String baseUrl, String filesBaseUrl) throws KTemplateException {
    	this(templateResource, params, baseUrl, filesBaseUrl, true);
    }
    
    public KTemplate(String templateResource, Map<String,Object> params, 
    		String baseUrl, String filesBaseUrl, boolean logEnabled) throws KTemplateException {
        this(templateResource, logEnabled);

    	setBaseUrl(baseUrl);
        setFilesBaseUrl(filesBaseUrl);
        
        if (params == null) {
        	params = new HashMap<String,Object>();
        }
        
        addContextMap(params);
        addContextObject("KUtil", new KTemplateUtil());

    	if (!isValid()) {
    		throw new IllegalStateException(
    				"KTemplate: Template is not valid: " + templateResource);
    	}
    }
    
    public static String getTemplate(String templateResource, Map<String,Object> params) throws KTemplateException {
    	KTemplate t = new KTemplate(templateResource, params, null, null);
    	return t.toString();
    }
    
    public static String getTemplate(String templateResource, Map<String,Object> params, boolean logEnabled) throws KTemplateException {
    	KTemplate t = new KTemplate(templateResource, params, null, null, logEnabled);
    	return t.toString();
    }
    
    
    public static String evalStringTemplate(String stringTemplate, Map<String,Object> params) throws KTemplateException {
    	KTemplate t = new KTemplate(null, params, null, null);
        t.setStringTemplate(stringTemplate);
    	return t.toString();
    }
    
    public static String evalStringTemplate(String stringTemplate, Map<String,Object> params, boolean logEnabled) throws KTemplateException {
    	KTemplate t = new KTemplate(null, params, null, null, logEnabled);
        t.setStringTemplate(stringTemplate);
    	return t.toString();
    }
    
    public void setInlineCss(boolean inlineCss) {
        this.inlineCss = inlineCss;
    }
    
  
    // Premailer css inliner
    private String inlineCss(String body) {
    	String s = null;
        
        try {
			s = KPremailer.processHtml(body);
		} catch (IOException e) {
            logger.error(e);
            s = body;
		}

    	logger.debug("inlineCss raw:\n" + body);
    	logger.debug("inlineCss processed:\n" + s);
    	return s;
    }
    
	public void setStringTemplate(String stringTemplate) {
		this.stringTemplate = stringTemplate;
	}

	/**
	 * sets the context param X_BASE_URL__ to this value.
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
        /*
        if (baseUrl != null && baseUrl.endsWith("/")) {
        	baseUrl = KStringUtil.chop(baseUrl);
        }
        */
		addContextObject("X_BASE_URL__", baseUrl);
	}
    
	/**
	 * sets the context param X_BASE_URL__ to this value.
	 */
	public void setFilesBaseUrl(String filesBaseUrl) {
		this.filesBaseUrl = filesBaseUrl;
		addContextObject("X_FILES_BASE_URL__", filesBaseUrl);
	}

	/**
	 * Configures the engine to use classpath to find templates
	 */
	private void init(VelocityEngine ve) throws Exception {
		/*
		 * ve.setProperty("resource.loader", "class");
		 * ve.setProperty("class.resource.loader.class",
		 * "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader"
		 * ); ve.init();
		 */

		Properties props = new Properties();
		props.setProperty(VelocityEngine.RESOURCE_LOADER, "class");
		props.setProperty("class." + VelocityEngine.RESOURCE_LOADER + ".class",
				ClasspathResourceLoader.class.getName());
        
		props.setProperty("runtime.log", getLogPath());
        if (!isLogEnabled()) {
        	props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        }
        
		ve.init(props);
	}
    
	public void initFileResourceLoader(HttpServletRequest req, 
			String templatePath) {
		ServletContext sc = req.getServletContext(); 
		String rootPath = sc.getRealPath("/"); 
        
		if (!templatePath.startsWith("/")) {
			templatePath = "/" + templatePath;
		}
		
		if (!templatePath.endsWith("/")) {
			templatePath += "/";
		}
		
		if (rootPath.endsWith("/")) {
			rootPath = KStringUtil.chop(rootPath);
		}
		
		rootPath += templatePath;
        
		initFileResourceLoader(rootPath);
	}
	
	public void initFileResourceLoader(String rootPath) {
		Properties props = new Properties();
		props.setProperty("resource.loader", "file");
		props.setProperty("file.resource.loader.class", 
				FileResourceLoader.class.getName());
		props.setProperty("file.resource.loader.path", rootPath);
		props.setProperty("file.resource.loader.cache", "true");
		props.setProperty("file.resource.loader.modificationCheckInterval", "30");
        
		props.setProperty("runtime.log", getLogPath());
        if (!isLogEnabled()) {
        	props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        }
		ve.init(props);
	}
	
	public boolean isLogEnabled() {
		return logEnabled;
	}
	
	public String getLogPath() {
		return logPath;
	}

	/*
	 * public void addContextFunction(String name, Object obj, String
	 * methodName) { context.putFunction(name, obj, methodName); }
	 */

	public void addContextObject(String name, Object obj) {
		context.put(name, obj);
	}

	public void addContextMap(Map<String, Object> contextMap) {
        if (contextMap == null) return;
		for (String key : contextMap.keySet()) {
			Object value = contextMap.get(key);
			addContextObject(key, value);
		}
	}
    
	//private Template getTemplate() throws ParseException {
	private Template getTemplate() {
		if (templateResource != null) {
			return ve.getTemplate(templateResource);
		}

        /*
		RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();            
		StringReader reader = new StringReader(stringTemplate);
		SimpleNode node = runtimeServices.parse(reader, "Template");
		Template template = new Template();
		template.setRuntimeServices(runtimeServices);
		template.setData(node);
		template.initDocument();
		return template;
        */
		return null;
	}

	public boolean isValid() {
		boolean isValid = true;

		try {
            getTemplate();
			//ve.getTemplate(templateResource);
		} catch (Exception e) {
			isValid = false;
			logger.error(e);
		}
		return (isValid);
	}

	public String toString() {
		String s = null;

		try {
			//Template t = ve.getTemplate(templateResource);
			Template t = getTemplate();
			StringWriter sw = new StringWriter();
            
            if (t != null) {
            	logger.debug("Found Velocity template: " + t);
            	t.merge(context, sw);
            } else if (stringTemplate != null) {
            	Velocity.evaluate(context, sw, "Template", stringTemplate);
            } else {
            	return null;
            }
                
			s = sw.toString();
		} catch (Exception e) {
			logger.error(e);
		}
		return (s);
	}
    
    /**
     * 
     * Use ASCII charset to produce hex entities.
     * 
     * User UTF-8 to create named entities.
     * 
     * @param charset
     * @return
     */
	public String toHtml(String charset) {
        return toHtml(null, charset);
	}
    
	public String toHtml() {
        return toHtml(null, null);
	}
    
	public String toHtml(Entities.EscapeMode escapeMode, String charset) {
        if (escapeMode == null) {
        	escapeMode = Entities.EscapeMode.base;
        }
        
        if (charset == null) {
        	charset = "UTF-8";
        }
        
        String html = toString();
        if (html != null) {
        	Document doc = Jsoup.parse(html);
        	doc.outputSettings().escapeMode(escapeMode);
            doc.outputSettings().charset(charset);
	    	doc.outputSettings().prettyPrint(true);
            html = doc.toString();
        }
        
    	if (html == null) {
    		throw new IllegalStateException(
    				"KTemplate: Failed to generate template body for template: " 
    						+ templateResource);
    	}
        
        if (inlineCss) {
        	//body = KCssInliner.process(body);
            html = inlineCss(html);
        }
        return html;
	}
    
	public static void main(String[] args) {
        boolean html = false;
        String rootPath = args[0];
        String templateName = args[1];
        String params = null;
        Map<String,Object> map = null;
        
        if (args[0].startsWith("-")) {
            if (args[0].equals("-html")) {
            	html = true;
            } else if (args[0].equals("-text")) {
            	html = false;
            } else {
            	System.err.println("\nError: Invalid option.\n\nUsage: main [-text|-html] templateRootPath templateName\n\n");
                System.exit(1);
            }
            rootPath = args[1];
            templateName = args[2];
            if (args.length == 4) {
            	params = args[3];
            }
        } else {
            if (args.length == 3) {
            	params = args[2];
            }
        }
        
        if (params != null) {
        	map = KStringUtil.toMap(params);
        }
        
        
        KTemplate t = null;
		try {
			System.err.println("creating template for rootPath: " + rootPath + "  templateName: " + templateName);
    		t = new KTemplate(rootPath, templateName, false);
    		t.addContextMap(map);
    	} catch (KTemplateException e) {
    		throw new IllegalStateException(
    				"KTemplate: Failed to create template object for template: " 
    						+ templateName, e);
    	}

    	if (!t.isValid()) {
    		throw new IllegalStateException(
    				"KTemplate: Template is not valid: " + templateName);
    	}
        
    	String s = null;
    	if (html) {
    		s = t.toHtml();
    	} else {
    		s = t.toString();
    	}
    	System.out.println(s);
	}
}
