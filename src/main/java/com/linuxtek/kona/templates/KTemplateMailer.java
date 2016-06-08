/*
 * Copyright (C) 2014 LinuxTek, Inc.  All Rights Reserved.
 */

package com.linuxtek.kona.templates;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.linuxtek.kona.mailer.KMailer;
import com.linuxtek.kona.mailer.KMailerException;
import com.linuxtek.kona.premailer.KPremailer;

public class KTemplateMailer {
	private static Logger logger = Logger.getLogger(KTemplateMailer.class);
    
	private String mailhost;
	private String mailTemplate;
    private KTemplate t;
    private boolean prettyPrint = false;
    
    private boolean inlineCss = false;
    private String inlineCssApiUrl = null;
    private String inlineCssApiKey = null;
    private String sender = null;
    
    public KTemplateMailer(String mailhost, String mailTemplate) {
    	this(mailhost, mailTemplate, null, null);
    }
    
    public KTemplateMailer(String mailhost, String mailTemplate, String baseUrl) {
    	this(mailhost, mailTemplate, baseUrl, null);
    }
    
    public KTemplateMailer(String mailhost, String mailTemplate, String baseUrl, String filesBaseUrl) {

    	this.mailhost = mailhost;
    	this.mailTemplate = mailTemplate;
    	this.t = null;
        
    	if (mailTemplate == null) {
    		throw new IllegalArgumentException("KTemplateMailer: Email template is null");
    	}
        
    	if (mailhost == null) {
    		throw new IllegalArgumentException("KTemplateMailer: mail host is null");
		}
    	
    	try {
    		this.t = new KTemplate(mailTemplate);
    	} catch (KTemplateException e) {
    		throw new IllegalStateException(
    				"KTemplateMailer: Failed to create template object for template: " 
    						+ mailTemplate, e);
    	}

    	this.t.setBaseUrl(baseUrl);
        this.t.setFilesBaseUrl(filesBaseUrl);

    	if (!this.t.isValid()) {
    		throw new IllegalStateException(
    				"KTemplateMailer: Template is not valid: " + mailTemplate);
    	}
    }
    
    public void setSender(String sender) {
    	this.sender = sender;
    }
    
    public void setPrettyPrint(boolean prettyPrint) {
    	this.prettyPrint = prettyPrint;
    }
    
    public void setInlineCss(boolean inlineCss) {
        this.inlineCss = inlineCss;
    }
    
    public void setInlineCssApiUrl(String inlineCssApiUrl) {
        this.inlineCssApiUrl = inlineCssApiUrl;
    }
    
    public void setInlineCssApiKey(String inlineCssApiKey) {
        this.inlineCssApiKey = inlineCssApiKey;
    }
    
    public void addContextObject(String key, Object value) {
    	this.t.addContextObject(key, value);
    }
    
    public void addContextMap(Map<String,Object> map) {
    	this.t.addContextMap(map);
    }
    
    /*
    @SuppressWarnings("unchecked")
    private String inlineCss_mailchimp(String body) {
    	String s = null;

    	Map<String,Object> request = new HashMap<String,Object>();
    	request.put("apikey", inlineCssApiKey);
    	request.put("html", body);

    	try {
    		RestTemplate template = new RestTemplate();
    		Map<String,Object> result = template.postForObject(inlineCssApiUrl, request, Map.class);
    		if (result != null) {
    			s = result.get("html").toString();
    		}
    	} catch (RestClientException e) {
    		logger.error(e);;
    	}

    	logger.debug("inlineCss raw:\n" + body);
    	logger.debug("inlineCss processed:\n" + s);
    	return s;
    }
    */
    
    // Premailer css inliner
    private String inlineCss(String body) {
    	String s = null;
        
        try {
			s = KPremailer.processHtml(body);
		} catch (IOException e) {
            logger.error(e);
            s = body;
		}

        /*
    	Map<String,Object> request = new HashMap<String,Object>();
    	request.put("html", body);

    	if (inlineCssApiUrl == null) {
    		inlineCssApiUrl = "http://premailer.dialect.ca/api/0.1/documents";
    	}

    	try {
    		RestTemplate template = new RestTemplate();
    		Map<String,Object> result = template.postForObject(inlineCssApiUrl, request, Map.class);
    		if (result != null) {
    			Map<String,String> documents = (Map<String,String>)result.get("documents");
    			String htmldocUrl = documents.get("html");
                if (htmldocUrl != null) {
                	s = KFileUtil.getUrlContentsAsString(htmldocUrl);
                }
    		}
    	} catch (RestClientException|IOException e) {
    		logger.error(e);;
    	}
        */

    	logger.debug("inlineCss raw:\n" + body);
    	logger.debug("inlineCss processed:\n" + s);
    	return s;
    }

    public MimeMessage send(String subject, String from, String to) throws KMailerException {
        return send(subject, from, null, to, null, null);
    }
    
    public MimeMessage send(String subject, String from, String replyTo, 
    		String to, String cc, String bcc) throws KMailerException {
        String body = null;
        
        if (prettyPrint) {
        	body = t.toHtml();
        } else {
        	body = t.toString();
        }

    	if (body == null) {
    		throw new IllegalStateException(
    				"KTemplateMailer: Failed to generate template body for template: " 
    						+ mailTemplate);
    	}
        
        if (inlineCss) {
        	//body = KCssInliner.process(body);
            body = inlineCss(body);
        }
        
        logger.debug("KTemplateMailer: processed HTML:\n" + body);

    	return sendEmail(subject, body, true, from, replyTo, to, cc, bcc);
    }

    protected MimeMessage sendEmail(String subject, String body, Boolean isHTML,
    		String from, String replyTo, String to, String cc, String bcc) 
    		throws KMailerException {

    	if (from == null) {
    		throw new IllegalArgumentException("KTemplateMailer: 'from' is null.");
    	}

    	if (to == null) {
    		throw new IllegalArgumentException("KTemplateMailer: 'to' is null.");
    	}

    	KMailer mailer = new KMailer(mailhost);
        mailer.setSender(sender); // sender's domain must match mailer's authenticated domain
    	mailer.setFrom(from); // could be different from sender but will likely be shown as "on behalf of" sender
        mailer.setReplyTo(replyTo);
    	mailer.setTo(to);
    	mailer.setCc(cc);
    	mailer.setBcc(bcc);
    	mailer.setSubject(subject);
        if (isHTML) {
        	mailer.setHtmlBody(body);
        } else {
        	mailer.setTextBody(body);
        }
    	mailer.setHTML(isHTML);
    	return mailer.send();
    }
}
