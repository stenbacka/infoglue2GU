/* ===============================================================================
 *
 * Part of the InfoGlue Content Management Platform (www.infoglue.org)
 *
 * ===============================================================================
 *
 *  Copyright (C)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2, as published by the
 * Free Software Foundation. See the file LICENSE.html for more information.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, including the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc. / 59 Temple
 * Place, Suite 330 / Boston, MA 02111-1307 / USA.
 *
 * ===============================================================================
 */

package org.infoglue.cms.applications.common.actions;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.infoglue.cms.applications.common.Session;
import org.infoglue.cms.exception.AccessConstraintException;
import org.infoglue.cms.exception.Bug;
import org.infoglue.cms.exception.ConfigurationError;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.ChangeNotificationController;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.StringManager;
import org.infoglue.cms.util.StringManagerFactory;
import org.infoglue.deliver.util.BrowserBean;
import org.infoglue.deliver.util.RequestAnalyser;
import org.infoglue.deliver.util.Timer;

import webwork.action.Action;
import webwork.action.CommandDriven;
import webwork.action.ResultException;
import webwork.action.ServletRequestAware;
import webwork.action.ServletResponseAware;

/**
 * @author Mattias Bogeblad
 * @author Frank Febbraro (frank@phase2technology.com)
 */

public abstract class WebworkAbstractAction implements Action, ServletRequestAware, ServletResponseAware, CommandDriven 
{
    private final static Logger logger = Logger.getLogger(WebworkAbstractAction.class.getName());

	private final String ACCESS_DENIED = "accessDenied";
	
	private Error error;
  	private Errors errors = new Errors();

  	private HttpServletRequest request;
  	private HttpServletResponse response;
  	private String commandName;
  
	
	/**
     *
     */
  	public Error getError() 
  	{
  		if(logger.isInfoEnabled())
  	  		logger.info("Fetching error from error-template:" + this.error);
    	
  		return this.error;
  	}

    /**
     *
     */
  	public Errors getErrors() 
  	{
  		if(logger.isInfoEnabled())
  			logger.info("Errors:" + this.errors);

  		return errors;
  	}
  
    /**
     *
     */
	public String doDefault() throws Exception 
    { 
        return INPUT;
    }



	/**
	 * This is the main execution point for any webwork action.
	 * Lately we added statistics on each action for debugging and optimization purposes.
	 */
    public String execute() throws Exception 
    {
    	Timer t = new Timer();
    	
    	String result = "";
    	
        try 
        {
        	result = isCommand() ? invokeCommand() : doExecute();
        	setStandardResponseHeaders();
        	
        	long elapsedTime = t.getElapsedTime();
        	long memoryDiff = t.getMemoryDifferenceAsMegaBytes();
        	if(elapsedTime > 5000 || memoryDiff > 100)
        	{
        		//RequestAnalyser.getRequestAnalyser().registerComponentStatistics("" + this.getUnencodedCurrentURIWithParameters() + " (" + memoryDiff + ")", elapsedTime);
        		logger.warn("The " + CmsPropertyHandler.getApplicationName() + " request: " + this.getUnencodedCurrentURIWithParameters() + " took " + elapsedTime + " ms to render and seems to have allocated " + memoryDiff + " MB of memory)");
        	}
        	//else
        	//	RequestAnalyser.getRequestAnalyser().registerComponentStatistics("" + this.getUnencodedCurrentURI(), elapsedTime);
        } 
        catch(ResultException e) 
        {
        	logger.error("ResultException " + e.getMessage());
        	logger.warn("ResultException " + e.getMessage(), e);
        	result = e.getResult();
        } 
		catch(AccessConstraintException e) 
		{
			logger.info("AccessConstraintException " + e, e);
			setErrors(e);
			result = ACCESS_DENIED;
		} 
        catch(ConstraintException e) 
        {
        	logger.info("ConstraintException " + e, e);
            setErrors(e);
			if(e.getResult() != null && !e.getResult().equals(""))
				result = e.getResult();
			else
				result = INPUT;
        } 
        catch(Bug e) 
        {
        	logger.error("Bug " + e.getMessage());
        	logger.warn("Bug  " + e.getMessage(), e);
            setError(e, e.getCause());
            result = ERROR;
        } 
        catch(ConfigurationError e) 
        {
         	logger.error("ConfigurationError " + e.getMessage());
         	logger.warn("ConfigurationError " + e.getMessage(), e);
            setError(e, e.getCause());
            result = ERROR;
        } 
        catch(SystemException e) 
        {
            logger.error("SystemException " + e.getMessage());
            logger.warn("SystemException " + e.getMessage(), e);
            setError(e, e.getCause());
            result = ERROR;
        } 
        catch(ThreadDeath e) 
        {
            logger.error("Thread died: " + e);
            logger.warn("Thread died: " + e.getMessage(), e);
            final SystemException exception = new SystemException("Page took to long to load! Please try again later.");
            setError(exception, e);
            result = ERROR;
        }
        catch(Throwable e) 
        {
            logger.error("Throwable " + e.getMessage());
            logger.warn("Throwable " + e.getMessage(), new Exception(e));
            final Bug bug = new Bug("Uncaught exception!", e);
            setError(bug, bug.getCause());
            result = ERROR;
        }
        
        try
        {
        	if(CmsPropertyHandler.getApplicationName().equalsIgnoreCase("cms"))
        		ChangeNotificationController.notifyListeners();
        }
        catch(Exception e)
        {
        	logger.error("Error notifying listener " + e.getMessage());
        	logger.warn("Error notifying listener " + e.getMessage(), e);
        }
        
        return result;
    }

	/**
	 * This method returns the url to the current page.
	 * Could be used in case of reload for example or for logging reasons.
	 */
	
	public String getCurrentUrl() throws Exception
	{
		String urlBase = getRequest().getRequestURL().toString();
		String urlParameters = getRequest().getQueryString();
		
		return URLEncoder.encode(urlBase + "?" + urlParameters, "UTF-8");
	}

	/**
	 * This method returns the url to the current page.
	 * Could be used in case of reload for example or for logging reasons.
	 */
	
	public String getUnencodedCurrentUrl() throws Exception
	{
		String urlBase = getRequest().getRequestURL().toString();
		String urlParameters = getRequest().getQueryString();
		
		return urlBase + (urlParameters != null ? "?" + urlParameters : "");
	}

	/**
	 * This method returns the URI to the current page.
	 */
	
	public String getUnencodedCurrentURI() throws Exception
	{
		String urlBase = getRequest().getRequestURI().toString();
		
		return urlBase;
	}

	/**
	 * This method returns the URI to the current page.
	 */
	
	public String getUnencodedCurrentURIWithParameters() throws Exception
	{
		String urlBase = getRequest().getRequestURI().toString();
		String queryString = "";
		Enumeration parameterNames = getRequest().getParameterNames();
		while(parameterNames.hasMoreElements())
		{
			String name = (String)parameterNames.nextElement();
			queryString += "&" + name + "=" + getRequest().getParameter(name);
		}
		if(queryString != null && !queryString.equals(""))
			urlBase = urlBase + "?" + queryString.substring(1);

		return urlBase;
	}

    /**
     *
     */
    public void setCommand(String commandName) 
    {
      	this.commandName = commandName;
    }

    /**
     *
     */
    public void setServletRequest(HttpServletRequest request) 
    {
      	this.request = request;
    }
  


    /**
     *
     */
    public void setServletResponse(HttpServletResponse response) 
    {
      	this.response = response;
    }



  	/**
   	 *
     */
	public void setError(Throwable throwable, Throwable cause) 
  	{
    	this.error = new Error(throwable, cause);
  	}


	/**
	 *
	 */
	private void setErrors(ConstraintException exception)
	{
		final Locale locale = getSession().getLocale();
		for (ConstraintException ce = exception;
			ce != null;
			ce = ce.getChainedException())
		{
			final String fieldName = ce.getFieldName();
			final String errorCode = ce.getErrorCode();
			final String extraInformation = ce.getExtraInformation();
			final String localizedErrorMessage = getLocalizedErrorMessage(locale, errorCode);
			getErrors().addError(fieldName, localizedErrorMessage + (extraInformation.length() > 0 ? " (" + extraInformation + ")" : ""));
		}
		logger.debug(getErrors().toString());
	}

	/**
     * <todo>Move to a ConstraintExceptionHelper class?</todo>
     */
  
  	private String getLocalizedErrorMessage(Locale locale, String errorCode) 
  	{
    	// <todo>fetch packagename from somewhere</todo>
    	StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.entities", locale);

	    // check if a specific error message exists - <todo/>	
  	  	// nah, use the general error message
    	return stringManager.getString(errorCode);
  	}
  

  	public String getLocalizedString(Locale locale, String key) 
  	{
    	StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.applications", locale);

    	return stringManager.getString(key);
  	}

  	public String getLocalizedString(Locale locale, String key, Object arg1) 
  	{
    	StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.applications", locale);

    	return stringManager.getString(key, arg1);
  	}

   /**
   	*
   	*/
	
	private boolean isCommand() 
  	{
    	return this.commandName != null && commandName.trim().length() > 0 && (this instanceof CommandDriven);
  	}

  	/**
   	 * This is a complement to the normal webwork execution which allows for a command-based execution of actions. 
   	 */
  	
  	private String invokeCommand() throws Exception 
  	{
  		Timer t = new Timer();
  		
    	final StringBuffer methodName = new StringBuffer("do" + this.commandName);
    	methodName.setCharAt(2, Character.toUpperCase(methodName.charAt(2)));

    	String result = "";
    	
    	try 
    	{
      		final Method method = getClass().getMethod(methodName.toString(), new Class[0]);
      		result = (String) method.invoke(this, new Object[0]);
        	setStandardResponseHeaders();
    	} 
    	catch (NoSuchMethodException e) 
    	{
    		logger.warn("No such method in:" + getRequest().getRequestURI() + ":" + e.getMessage());
		}
    	catch(Exception ie) 
    	{
			if(ie.getMessage() != null)
				logger.error("Exception in top action:" + ie.getMessage(), ie);
    	    
			try 
			{
				throw ie.getCause();
			} 
			catch(ResultException e) 
			{
				logger.error("ResultException " + e, e);
				result = e.getResult();
			} 
			catch(AccessConstraintException e) 
			{
				logger.info("AccessConstraintException " + e, e);
				setErrors(e);
				result = ACCESS_DENIED;
			} 
			catch(ConstraintException e) 
			{
				logger.info("ConstraintException " + e, e);
				setErrors(e);
				if(e.getResult() != null && !e.getResult().equals(""))
					result = e.getResult();
				else
					result = INPUT;
			} 
			catch(Bug e) 
			{
				logger.error("Bug " + e.getMessage(), e);
				setError(e, e.getCause());
				result = ERROR;
			} 
			catch(ConfigurationError e) 
			{
				logger.error("ConfigurationError " + e);
				setError(e, e.getCause());
				result = ERROR;
			} 
			catch(SystemException e) 
			{
				logger.error("SystemException " + e, e);
				setError(e, e.getCause());
				result = ERROR;
			} 
			catch(Throwable e) 
			{
				logger.error("Throwable " + e.getMessage(), e);
				final Bug bug = new Bug("Uncaught exception!", e);
				setError(bug, bug.getCause());
				result = ERROR;
			}	        
    	}

    	try
        {
        	ChangeNotificationController.notifyListeners();
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }
        
        return result;
    	
  	}

  	/**
  	 * This method adds a header to all responses which makes all latest IE-browsers fallback to IE8-mode.
  	 * Some aspects of Infoglue (FCKEditor) does not work in IE9 for example.
  	 */
	public void setStandardResponseHeaders() 
	{
		try
		{
			getResponse().setHeader("X-UA-Compatible", "IE=EmulateIE8");
		}
		catch (Exception e) 
		{
			logger.warn("Could not set headers:" + e.getMessage());
		}
	}

    public final String getRoot() 
    {
    	return request.getContextPath();
    }
    
    /**
     * This method returns a logged in principal if existing.
     */

    public final InfoGluePrincipal getInfoGluePrincipal()
    {
		return getSession().getInfoGluePrincipal();
    }

	/**
	 * Subclasses implement this
	 */

	protected abstract String doExecute() throws Exception;

	/**
	 * 
	 */

	public final BrowserBean getBrowserBean()
	{
		BrowserBean browserBean = new BrowserBean();
		browserBean.setRequest(this.request);
		return browserBean;
	}
	
	/**
	 * Returns the HttpServletRequest
	 * TODO: Hide the implementation behind WebWorks abstractions in ActionContext
	 */
	protected final HttpServletRequest getRequest()
	{
		return this.request;
	}

	/** 
	 * Returns the HttpServletResponse
	 * TODO: Hide the implementation behind WebWorks abstractions in ActionContext
	 */

	protected final HttpServletResponse getResponse()
	{
		return this.response;
	}

	/**
	 * Returns the HttpSession
	 * TODO: Hide the implementation behind WebWorks abstractions in ActionContext
	 */
	protected final HttpSession getHttpSession()
	{
		return getRequest().getSession();
	}

    /**
	 * Use the ActionContext to initialize the Session and remove the dependence
	 * on HTTP and the Servlet Spec. Makes it much easier for testing.
     */

    public final Session getSession() 
    {
        return new Session(getHttpSession());
    }
  
    public final String getSessionId()
    {
    	return getHttpSession().getId();
    }
}