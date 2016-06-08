/*
 * Copyright (C) 2011 LinuxTek, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.templates;

@SuppressWarnings("serial")
public class KTemplateException extends Exception
{
	public KTemplateException()
	{ 
        super();
	}

	public KTemplateException(String message)
	{ 
		super(message);
	}

	public KTemplateException(String message, Throwable cause)
	{ 
		super(message, cause);
	}

	public KTemplateException(Throwable cause)
	{ 
		super(cause);
	}
}
