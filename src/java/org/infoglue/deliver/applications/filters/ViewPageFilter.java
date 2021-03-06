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

package org.infoglue.deliver.applications.filters;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.controllers.kernel.impl.simple.RedirectController;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.management.RepositoryVO;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.deliver.applications.databeans.DeliveryContext;
import org.infoglue.deliver.controllers.kernel.impl.simple.BaseDeliveryController;
import org.infoglue.deliver.controllers.kernel.impl.simple.ExtranetController;
import org.infoglue.deliver.controllers.kernel.impl.simple.LanguageDeliveryController;
import org.infoglue.deliver.controllers.kernel.impl.simple.NodeDeliveryController;
import org.infoglue.deliver.controllers.kernel.impl.simple.RepositoryDeliveryController;
import org.infoglue.deliver.util.CacheController;
import org.infoglue.deliver.util.RequestAnalyser;
import org.infoglue.deliver.util.Timer;


/**
 *
 *
 * @author Lars Borup Jensen (lbj@atira.dk)
 * @author Mattias Bogeblad (bogeblad@yahoo.com)
 * 
 */

public class ViewPageFilter implements Filter 
{
    public final static Logger logger = Logger.getLogger(ViewPageFilter.class.getName());

    private FilterConfig filterConfig = null;
    private URIMatcher uriMatcher = null;
    private URIMapperCache uriCache = null;
    public static String attributeName = null;
    public static boolean caseSensitive = false;

    public void init(FilterConfig filterConfig) throws ServletException 
    {
        this.filterConfig = filterConfig;
        String filterURIs = filterConfig.getInitParameter(FilterConstants.FILTER_URIS_PARAMETER);

        String caseSensitiveString = CmsPropertyHandler.getCaseSensitiveRedirects();
        logger.info("caseSensitiveString:" + caseSensitiveString);
        caseSensitive = Boolean.parseBoolean(caseSensitiveString);
        
        uriMatcher = URIMatcher.compilePatterns(splitString(filterURIs, ","), caseSensitive);

        attributeName = CmsPropertyHandler.getNiceURIAttributeName();
        logger.info("attributeName from properties:" + attributeName);
        
        if(attributeName == null || attributeName.indexOf("@") > -1)
            attributeName = filterConfig.getInitParameter(FilterConstants.ATTRIBUTE_NAME_PARAMETER);
        
        logger.info("attributeName from web.xml, filter parameters:" + attributeName);
        if(attributeName == null || attributeName.equals(""))
            attributeName = "NavigationTitle";

        logger.info("attributeName used:" + attributeName);

        uriCache = new URIMapperCache();
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException 
    {       
        Timer t = new Timer();

        long end, start = System.currentTimeMillis();
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        if(httpRequest.getParameter("igEncodingTest") != null && !httpRequest.getParameter("igEncodingTest").equals(""))
        	logger.warn("httpRequest:" + httpRequest.getParameter("name"));
        
        String enableNiceURI = CmsPropertyHandler.getEnableNiceURI();
        if (enableNiceURI == null)
            enableNiceURI = "false";

        validateCmsProperties(httpRequest);
        String requestURI = URLDecoder.decode(getContextRelativeURI(httpRequest), "UTF-8");
        if(logger.isInfoEnabled())
        	logger.info("requestURI:" + requestURI);

        try
        {
        	//System.out.println("requestURI:" + requestURI);
			if(logger.isInfoEnabled())
	        	logger.info("requestURI before decoding:" + requestURI);
            
			requestURI = URLDecoder.decode(requestURI, CmsPropertyHandler.getURIEncoding());
			if(logger.isInfoEnabled())
	        	logger.info("requestURI after decoding:" + requestURI);

        	String fromEncoding = CmsPropertyHandler.getURIEncoding();
			String toEncoding = "utf-8";
			String testRequestURI = new String(requestURI.getBytes(fromEncoding), toEncoding);
			if(testRequestURI.indexOf((char)65533) == -1)
				requestURI = testRequestURI;
			//System.out.println("requestURI:" + requestURI);
        }
        catch (Exception e) 
        {
        	logger.warn("Error checking for unicode chars:" + e.getMessage());
		}

        if(logger.isInfoEnabled())
        	logger.info("requestURI after encoding check:" + requestURI);

		Integer languageId = null;
		Set<RepositoryVO> repositoryVOList = null;
		String[] nodeNames = null;
		InfoGluePrincipal infoGluePrincipal = null;
        try
        {
        	if(requestURI.indexOf(CmsPropertyHandler.getDigitalAssetBaseUrl() + "/protected") > -1)
        	{
            	throw new Exception("Not allowed to view protected assets...");
        	}
        	
	        if (enableNiceURI.equalsIgnoreCase("true") && !uriMatcher.matches(requestURI)) 
	        {
	            while(CmsPropertyHandler.getActuallyBlockOnBlockRequests() && RequestAnalyser.getRequestAnalyser().getBlockRequests())
	            {
	            	if(logger.isInfoEnabled())
	            		logger.info("Queing up requests as cache eviction are taking place..");
	            	try { Thread.sleep(10); } catch (Exception e) {}
	            }

	            RequestAnalyser.getRequestAnalyser().incNumberOfCurrentRequests(null);

	            HttpSession httpSession = httpRequest.getSession(true);

	            Database db = CastorDatabaseService.getDatabase();
	    		
	            BaseDeliveryController.beginTransaction(db);
	            
	            try
	            {
	                repositoryVOList = getRepositoryId(httpRequest, db);
	                if(logger.isInfoEnabled())
	                	logger.info("repositoryVOList:" + repositoryVOList.size());
	                
	            	languageId = getLanguageId(httpRequest, httpSession, repositoryVOList, requestURI, db);
	            
		            Integer siteNodeId = null;
	                if(languageId != null)
	                {
			            nodeNames = splitString(requestURI, "/");
			            logger.info("nodeNames:" + nodeNames.length);
			            
			            List<String> nodeNameList = new ArrayList<String>();
			            for(int i=0; i < nodeNames.length; i++)
			            {
			            	String nodeName = nodeNames[i];
			            	if(nodeName.indexOf(".cid") == -1)
			            	{
			            		nodeNameList.add(nodeName);
			            	}
			            	/*
			            	boolean add = true;
			            	
			            	if(i == 0 && nodeName.length() < 4)
			            	{
			            		System.out.println("Checking for language...:" + nodeName);
			            		LanguageVO languageVO = LanguageController.getController().getLanguageVOWithCode(nodeName);
			            		if(languageVO != null)
			            			add = false;
			            	}
			            	
			            	if(nodeName.indexOf(".cid") == -1 && add)
			            	{
			            		nodeNameList.add(nodeName);
			            	}
			            	*/
			            }

	            		nodeNames = new String[nodeNameList.size()];
	            		nodeNames = nodeNameList.toArray(nodeNames);
			            
	            		//logger.info("RepositoryId.: "+repositoryId);
			            //logger.info("LanguageId...: "+languageId);
			            //logger.info("RequestURI...: "+requestURI);
			
		                infoGluePrincipal = (InfoGluePrincipal) httpSession.getAttribute("infogluePrincipal");
		                if (infoGluePrincipal == null) 
		                {
		                    try 
		                    {
		                        infoGluePrincipal = (InfoGluePrincipal) CacheController.getCachedObject("userCache", "anonymous");
		                        if (infoGluePrincipal == null) 
		                        {
		                            Map arguments = new HashMap();
		        				    arguments.put("j_username", CmsPropertyHandler.getAnonymousUser());
		        				    arguments.put("j_password", CmsPropertyHandler.getAnonymousPassword());
		        					infoGluePrincipal = (InfoGluePrincipal)ExtranetController.getController().getAuthenticatedPrincipal(db, arguments);
		        					if(infoGluePrincipal != null)
		        						CacheController.cacheObject("userCache", "anonymous", infoGluePrincipal);
		                        }
		                        //this.principal = ExtranetController.getController().getAuthenticatedPrincipal("anonymous", "anonymous");
		
		                    } 
		                    catch (Exception e) 
		                    {
		    	                BaseDeliveryController.rollbackTransaction(db);
		                        throw new SystemException("There was no anonymous user found in the system. There must be - add the user anonymous/anonymous and try again.", e);
		                    }
		                }
		
		                Iterator repositorVOListIterator = repositoryVOList.iterator();
		                while(repositorVOListIterator.hasNext())
		                {
		                    RepositoryVO repositoryVO = (RepositoryVO)repositorVOListIterator.next();
		                    //logger.warn("Getting node from:" + repositoryVO.getName());
		                    //for(String nodeName : nodeNames)
		                    //	System.out.println("nodeName:" + nodeName);
		                    
		                    //TODO
		                    DeliveryContext deliveryContext = DeliveryContext.getDeliveryContext();
	                    	siteNodeId = NodeDeliveryController.getSiteNodeIdFromPath(db, infoGluePrincipal, repositoryVO, nodeNames, attributeName, deliveryContext, httpSession, languageId);
	                    	//System.out.println("siteNodeId:" + siteNodeId);
	                    	//siteNodeId = NodeDeliveryController.getSiteNodeIdFromPath(db, infoGluePrincipal, repositoryVO, nodeNames, attributeName, languageId, DeliveryContext.getDeliveryContext());
		                    if(siteNodeId != null)
		                        break;
		                }
	                }
	                
	                BaseDeliveryController.rollbackTransaction(db);

	                end = System.currentTimeMillis();
	                
	                if(siteNodeId == null)
	                {
	                    String redirectUrl = RedirectController.getController().getRedirectUrl(httpRequest);
	                    if(redirectUrl != null && redirectUrl.length() > 0)
	                    {
		                    httpResponse.sendRedirect(redirectUrl);
		                    return;
	                    }
	                    
	        			String extraInformation = "Referer: " + httpRequest.getHeader("Referer") + "\n";
	        			extraInformation += "UserAgent: " + httpRequest.getHeader("User-Agent") + "\n";
	        			extraInformation += "User IP: " + httpRequest.getRemoteAddr();
	        			
	        			logger.info("Could not map URI " + requestURI + " against any page on this website." + "\n" + extraInformation);
	                    throw new ServletException("Could not map URI " + requestURI + " against any page on this website.");	                    	
	                }
	                else
	                    logger.info("Mapped URI " + requestURI + " --> " + siteNodeId + " in " + (end - start) + "ms");
	             
	                Integer contentId = getContentId(httpRequest);
	                
	                HttpServletRequest wrappedHttpRequest = prepareRequest(httpRequest, siteNodeId, languageId, contentId);
	             
	               	RequestAnalyser.getRequestAnalyser().registerComponentStatistics("ViewPageFilter before ViewPage", t.getElapsedTime());
	                
	                wrappedHttpRequest.getRequestDispatcher("/ViewPage.action").forward(wrappedHttpRequest, httpResponse);
	            } 
	            catch (SystemException e) 
	            {
	                BaseDeliveryController.rollbackTransaction(db);
	                logger.error("Failed to resolve siteNodeId:" + e.getMessage());
	                logger.warn("Failed to resolve siteNodeId:" + e.getMessage(), e);
	                throw new ServletException(e);
	            } 
	            catch (Exception e) 
	            {
	                BaseDeliveryController.rollbackTransaction(db);
	                throw new ServletException(e);
	            }
	            finally
	            {
	            	try
	            	{
	            		BaseDeliveryController.closeDatabase(db);
	            	}
	            	catch (Exception e) 
	            	{
	            		e.printStackTrace();
					}
	                RequestAnalyser.getRequestAnalyser().decNumberOfCurrentRequests(-1);
	            }
	            
	        } 
	        else 
	        {
	        	//filterChain.doFilter(httpRequest, httpResponse);
	        	if(!httpResponse.isCommitted())
	        	{
	        		try
		        	{
		        		filterChain.doFilter(httpRequest, httpResponse);
		        	}
		        	catch (Exception e) 
		        	{
		        		logger.error("Response was committed - could not continue filter chains:" + e.getMessage());
		        	}
	        	}
	        }    
        }
        catch (SystemException se) 
        {
        	if(!httpResponse.isCommitted())
        	{
	            httpRequest.setAttribute("responseCode", "500");
	            httpRequest.setAttribute("error", se);
	            httpRequest.setAttribute("languageId", languageId);
	            httpRequest.getRequestDispatcher("/ErrorPage.action").forward(httpRequest, httpResponse);
        	}
        	else
        		logger.error("Error and response was committed:" + se.getMessage(), se);
        }
        catch (Exception e) 
        {
        	if(!httpResponse.isCommitted())
        	{
				httpRequest.setAttribute("responseCode", "404");
				httpRequest.setAttribute("error", e);
				Integer errorLanguageId = languageId;
				Database db = null;
				try
				{
					db = CastorDatabaseService.getDatabase();
					BaseDeliveryController.beginTransaction(db);
					Integer siteNodeId = getClosestExistingParentSiteNodeId(db, httpRequest, infoGluePrincipal, nodeNames, repositoryVOList, languageId);
					if (siteNodeId != null)
					{
						httpRequest.setAttribute("closestExistingParentSiteNodeId", siteNodeId);
						List<LanguageVO> languages = (List<LanguageVO>)LanguageDeliveryController.getLanguageDeliveryController().getLanguagesForSiteNode(db, siteNodeId, infoGluePrincipal);
						if (languages.size() >= 1)
						{
							errorLanguageId = languages.get(0).getLanguageId();
						}
						else
						{
							logger.warn("Closest exisiting parent site node did not have any languages, weird. SiteNodeId: " + siteNodeId);
						}
					}
				}
				catch (Exception ex)
				{
					logger.warn("An error occured when trying to find parent site node for non-existing path.");
					logger.info("An error occured when trying to find parent site node for non-existing path.", ex);
				}
				finally {
					if (db != null)
					{
						try
						{
							BaseDeliveryController.rollbackTransaction(db);
							BaseDeliveryController.closeDatabase(db);
						}
						catch (Exception ex2)
						{
							logger.error("Failed to close database connection in error page language id computation. Message: " + ex2.getMessage());
							logger.warn("Failed to close database connection in error page language id computation.", ex2);
						}
					}
				}
				httpRequest.setAttribute("languageId", errorLanguageId);
				httpRequest.getRequestDispatcher("/ErrorPage.action").forward(httpRequest, httpResponse);
			}
			else
			{
				logger.error("Error and response was committed:" + e.getMessage(), e);
			}
		}
	}

    public void destroy() 
    {
        this.filterConfig = null;
    }

	private Integer getClosestExistingParentSiteNodeId(Database db, HttpServletRequest httpRequest, InfoGluePrincipal infoGluePrincipal, String[] nodeNames, Set<RepositoryVO> repositoryVOList, Integer languageId) throws SystemException, Exception
	{
		Integer siteNodeId = null;

		HttpSession httpSession = httpRequest.getSession(true);
		Iterator<RepositoryVO> repositorVOListIterator;

		while (nodeNames.length > 0)
		{
			nodeNames = Arrays.copyOf(nodeNames, nodeNames.length - 1);
			repositorVOListIterator = repositoryVOList.iterator();
			while(repositorVOListIterator.hasNext())
			{
				RepositoryVO repositoryVO = (RepositoryVO)repositorVOListIterator.next();

				DeliveryContext deliveryContext = DeliveryContext.getDeliveryContext();
				siteNodeId = NodeDeliveryController.getSiteNodeIdFromPath(db, infoGluePrincipal, repositoryVO, nodeNames, attributeName, deliveryContext, httpSession, languageId);

				if (siteNodeId != null)
				{
					return siteNodeId;
				}
			}
		}

		return siteNodeId;
	}

    private void validateCmsProperties(HttpServletRequest request) 
    {
        if (CmsPropertyHandler.getServletContext() == null) 
        {
        	CmsPropertyHandler.setServletContext(request.getContextPath());
        }
    }

    private Set<RepositoryVO> getRepositoryId(HttpServletRequest request, Database db) throws ServletException, SystemException, Exception 
    {
        /*
        if (session.getAttribute(FilterConstants.REPOSITORY_ID) != null) 
        {
            logger.info("Fetching repositoryId from session");
            return (Integer) session.getAttribute(FilterConstants.REPOSITORY_ID);
        }
        */

        logger.info("Trying to lookup repositoryId");
        String serverName = request.getServerName();
        String portNumber = new Integer(request.getServerPort()).toString();
        String repositoryName = request.getParameter("repositoryName");
        String requestURI = request.getRequestURI();
        logger.info("serverName:" + serverName);
        logger.info("repositoryName:" + repositoryName);
        String firstPath = requestURI.replaceAll(request.getContextPath(), "").replaceAll("//", "/");
        
        logger.info("firstPath:" + firstPath);
        if(firstPath.startsWith("/"))
        	firstPath = firstPath.substring(1);
        String[] splitPath = firstPath.split("/");
        if(splitPath.length > 2)
        	firstPath = "/" + splitPath[0] + "/" + splitPath[1];
        
        String repCacheKey = "" + serverName + "_" + portNumber + "_" + repositoryName + "_" + firstPath;
        logger.info("repCacheKey:" + repCacheKey);
        Set<RepositoryVO> repositoryVOList = (Set<RepositoryVO>)CacheController.getCachedObject(uriCache.CACHE_NAME, repCacheKey);
        if (repositoryVOList != null) 
        {
        	logger.info("Using cached repositoryVOList");
            return repositoryVOList;
        }
        
        Set<RepositoryVO> repositories = RepositoryDeliveryController.getRepositoryDeliveryController().getRepositoryVOListFromServerName(db, serverName, portNumber, repositoryName, requestURI);
        if(logger.isInfoEnabled())
        	logger.info("repositories:" + repositories);
        
        if (repositories.size() == 0)
        {
            String redirectUrl = RedirectController.getController().getRedirectUrl(request);
            logger.info("redirectUrl:" + redirectUrl);
            if(redirectUrl == null || redirectUrl.length() == 0)
            {
                if (repositories.size() == 0) 
                {
                    try 
                    {
                        logger.info("Adding master repository instead - is this correct?");
                        
                        repositories.add(RepositoryDeliveryController.getRepositoryDeliveryController().getMasterRepository(db));
                    } 
                    catch (Exception e1) 
                    {
                        logger.error("Failed to lookup master repository");
                    }
                }
                
                if (repositories.size() == 0)
                    throw new ServletException("Unable to find a repository for server-name " + serverName);
            }
        }
        //t.printElapsedTime("getRepositoryVOListFromServerName took");
        
        CacheController.cacheObject(uriCache.CACHE_NAME, repCacheKey, repositories);
        //session.setAttribute(FilterConstants.REPOSITORY_ID, repository.getRepositoryId());
        return repositories;
    }

    private Integer getLanguageId(HttpServletRequest request, HttpSession session, Set<RepositoryVO> repositoryVOList, String requestURI, Database db) throws ServletException, Exception 
    {
        Integer languageId = null;
        if (request.getParameter("languageId") != null) 
        {
            logger.info("Language is explicitely given in request");
            try 
            {
                languageId = Integer.valueOf(request.getParameter("languageId"));
                session.setAttribute(FilterConstants.LANGUAGE_ID, languageId);
            } 
            catch (NumberFormatException e) {}
        }

        if (languageId != null)
            return languageId;

        if (session.getAttribute(FilterConstants.LANGUAGE_ID) != null) {
            logger.info("Fetching languageId from session");
            return (Integer) session.getAttribute(FilterConstants.LANGUAGE_ID);
        }

        Integer repositoryId = null;
        if(repositoryVOList != null && repositoryVOList.size() > 0)
            repositoryId = ((RepositoryVO)repositoryVOList.toArray()[0]).getId();
        
        logger.info("Looking for languageId for repository " + repositoryId);

        if(repositoryId == null)
        	return null;

        Locale requestLocale = request.getLocale();

        try 
        {
            List availableLanguagesForRepository = LanguageDeliveryController.getLanguageDeliveryController().getAvailableLanguagesForRepository(db, repositoryId);
            
            String useBrowserLanguage = CmsPropertyHandler.getUseBrowserLanguage();
            logger.info("useBrowserLanguage:" + useBrowserLanguage);
            if (requestLocale != null && useBrowserLanguage != null && useBrowserLanguage.equals("true")) 
            {
                for (int i = 0; i < availableLanguagesForRepository.size(); i++) 
                {
                    LanguageVO language = (LanguageVO) availableLanguagesForRepository.get(i);
                    logger.info("language:" + language.getLanguageCode());
                    logger.info("browserLanguage:" + requestLocale.getLanguage());
                    if (language.getLanguageCode().equalsIgnoreCase(requestLocale.getLanguage())) {
                        languageId = language.getLanguageId();
                    }
                }
            }
            if (languageId == null && availableLanguagesForRepository.size() > 0) {
                languageId = ((LanguageVO) availableLanguagesForRepository.get(0)).getLanguageId();
            }
        } 
        catch (Exception e) 
        {
            logger.error("Failed to fetch available languages for repository " + repositoryId);
        }

        if (languageId == null)
            throw new ServletException("Unable to determine language for repository " + repositoryId);

        session.setAttribute(FilterConstants.LANGUAGE_ID, languageId);
        
        return languageId;
    }

    private Integer getContentId(HttpServletRequest request) throws ServletException, Exception 
    {
        Integer contentId = null;
        
    	String contentIdString = null;
        if (request.getParameter("contentId") != null) 
        {
            contentIdString = request.getParameter("contentId");
        }
        else
        {
        	int cidIndex = request.getRequestURL().indexOf(".cid");
        	int cidIndexEnd = request.getRequestURL().indexOf("?", cidIndex);
        	if(cidIndexEnd == -1)
        		cidIndexEnd = request.getRequestURL().indexOf("/", cidIndex);

        	if(cidIndex > -1)
        	{
        		if(cidIndexEnd == -1)
        			contentIdString = request.getRequestURL().substring(cidIndex + 4);
        		else
        			contentIdString = request.getRequestURL().substring(cidIndex + 4, cidIndexEnd);
        	}
        }

        try 
        {
        	contentId = Integer.valueOf(contentIdString);
        } 
        catch (NumberFormatException e) {}

        return contentId;
    }

    
    // @TODO should I URLDecode the strings first? (incl. context path)
    private String getContextRelativeURI(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && requestURI.length() > 0) {
            requestURI = requestURI.substring(contextPath.length(), requestURI.length());
        }
        if (requestURI.length() == 0)
            return "/";
        return requestURI;
    }
    
    private String[] splitString(String str, String delimiter) {
        List list = new ArrayList();
        StringTokenizer st = new StringTokenizer(str, delimiter);
        while (st.hasMoreTokens()) {
            // Updated to handle portal-url:s
            String t = st.nextToken();
            if (t.startsWith("_")) {
                break;
            } else {
                // Not related to portal - add
                list.add(t.trim());
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    private HttpServletRequest prepareRequest(HttpServletRequest request, Integer siteNodeId, Integer languageId, Integer contentId) 
    {
        HttpServletRequest wrappedRequest = new IGHttpServletRequest(request, siteNodeId, languageId, contentId);

        return wrappedRequest;
    }
    

    private class IGHttpServletRequest extends HttpServletRequestWrapper 
    {
        Map requestParameters = new HashMap();
        
        public IGHttpServletRequest(HttpServletRequest httpServletRequest, Integer siteNodeId, Integer languageId, Integer contentId) 
        {
    		super(httpServletRequest);
    		
    		requestParameters.putAll(httpServletRequest.getParameterMap());
            requestParameters.put("siteNodeId", new String[] { String.valueOf(siteNodeId)});
            requestParameters.put("languageId", new String[] { String.valueOf(languageId)});

            if(contentId != null)
            	requestParameters.put("contentId", new String[] { String.valueOf(contentId)});
            else
            {
	            if (requestParameters.get("contentId") == null)
	                requestParameters.put("contentId", new String[] { String.valueOf(-1)});
            }
            
            String originalServletPath = ((HttpServletRequest)httpServletRequest).getServletPath();
            String originalRequestURL = ((HttpServletRequest)httpServletRequest).getRequestURL().toString();
            String originalRequestURI = ((HttpServletRequest)httpServletRequest).getRequestURI();
            String originalQueryString = ((HttpServletRequest)httpServletRequest).getQueryString();

            requestParameters.put("originalServletPath", new String[] { originalServletPath });
    		requestParameters.put("originalRequestURL", new String[] { originalRequestURL });
    		requestParameters.put("originalRequestURI", new String[] { originalRequestURI });
    		if(originalQueryString != null && originalQueryString.length() > 0)
    			requestParameters.put("originalQueryString", new String[] { originalQueryString });
    			
            //logger.info("siteNodeId:" + siteNodeId);
            //logger.info("languageId:" + languageId);
            //logger.info("contentId:" + requestParameters.get("contentId"));
        }

        public String getParameter(String s) 
        {
            String[] array = (String[]) requestParameters.get(s);
            if (array != null && array.length > 0)
                return array[0];
        
            return null;
        }

        public Map getParameterMap() 
        {
            return Collections.unmodifiableMap(requestParameters);
        }

        public Enumeration getParameterNames() 
        {
            return new ParameterNamesEnumeration(requestParameters.keySet().iterator());
        }

        public String[] getParameterValues(String s) 
        {
            String[] array = (String[]) requestParameters.get(s);
            if (array != null && array.length > 0)
                return array;
            
            return null;
        }
        
    }

    private class ParameterNamesEnumeration implements Enumeration {
        Iterator it = null;

        public ParameterNamesEnumeration(Iterator it) {
            this.it = it;
        }

        public boolean hasMoreElements() {
            return it.hasNext();
        }

        public Object nextElement() {
            return it.next();
        }

    }

}