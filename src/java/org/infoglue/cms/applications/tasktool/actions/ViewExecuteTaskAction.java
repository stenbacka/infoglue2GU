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

package org.infoglue.cms.applications.tasktool.actions;


import org.infoglue.cms.applications.common.actions.WebworkAbstractAction;
import org.infoglue.cms.entities.content.*;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.controllers.kernel.impl.simple.*;
import org.infoglue.cms.util.*;


import org.apache.velocity.app.Velocity;
import org.apache.velocity.*;

import java.io.*;
import java.util.*;

public class ViewExecuteTaskAction extends WebworkAbstractAction
{
	private Integer taskContentId = null;
	private Integer contentId = null;

	public ViewExecuteTaskAction()
	{
	}
    
    /**
     * This method which is the default one only serves to show a list 
     * of tasks to the user so he/she can select one to run. 
     */
    
	public String doExecute() throws Exception
	{
		return "success";
	}

	public String doUserInput() throws Exception
	{
		ContentVO contentVO = ContentController.getContentController().getContentVOWithId(this.getTaskContentId());
		
		CmsLogger.logInfo("Language:" + LanguageController.getController().getMasterLanguage((Integer)getHttpSession().getAttribute("repositoryId")).getId());
		
		ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestContentVersionVO(contentVO.getId(), LanguageController.getController().getMasterLanguage((Integer)getHttpSession().getAttribute("repositoryId")).getId());

		//TODO - should not do this but find one available version probably
		if(contentVersionVO == null)
			contentVersionVO = ContentVersionController.getContentVersionController().getLatestContentVersionVO(contentVO.getId(), ((LanguageVO)LanguageController.getController().getLanguageVOList().get(0)).getId());
		
		//CmsLogger.logInfo("contentVersionVO:" + contentVersionVO);
		
		String userInputHTML = ContentVersionController.getContentVersionController().getAttributeValue(contentVersionVO.getId(), "UserInputHTML", false);
	
		//CmsLogger.logInfo("Found userInputHTML:" + userInputHTML);
			 
		ScriptController scriptController = getScriptController();
		scriptController.setRequest(this.getRequest());
		scriptController.beginTransaction();

		Map context = new HashMap();
		context.put("scriptLogic", scriptController);

		StringWriter cacheString = new StringWriter();
		PrintWriter cachedStream = new PrintWriter(cacheString);
		renderTemplate(context, cachedStream, userInputHTML);
		String result = cacheString.toString();

		scriptController.commitTransaction();
				
		getResponse().setContentType("text/html");
		PrintWriter out = getResponse().getWriter();
		out.println(result);
		out.flush();
		out.close();
		
		return NONE;
	}

	/**
	 * This method serves as the invoker of a task/script. It uses the velocity-engine to run logic.
	 * It allways run the script within it's own transaction as of now.
	 * 
	 * @throws Exception
	 */

	public String doExecuteTask() throws Exception
	{
		ContentVO contentVO = ContentController.getContentController().getContentVOWithId(this.getTaskContentId());
		ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestContentVersionVO(contentVO.getId(), LanguageController.getController().getMasterLanguage((Integer)getHttpSession().getAttribute("repositoryId")).getId());
		
		//TODO - should not do this but find one available version probably
		if(contentVersionVO == null)
			contentVersionVO = ContentVersionController.getContentVersionController().getLatestContentVersionVO(contentVO.getId(), ((LanguageVO)LanguageController.getController().getLanguageVOList().get(0)).getId());
		
		String code = ContentVersionController.getContentVersionController().getAttributeValue(contentVersionVO.getId(), "ScriptCode", false);
		String userOutputHTML = ContentVersionController.getContentVersionController().getAttributeValue(contentVersionVO.getId(), "UserOutputHTML", false);
		
		ScriptController scriptController = getScriptController();
		scriptController.setRequest(this.getRequest());
		scriptController.beginTransaction();

		Map context = new HashMap();
		context.put("scriptLogic", scriptController);

		StringWriter cacheString = new StringWriter();
		PrintWriter cachedStream = new PrintWriter(cacheString);
		renderTemplate(context, cachedStream, code);
		String result = cacheString.toString();
		
		scriptController.commitTransaction();
		
		cacheString = new StringWriter();
		cachedStream = new PrintWriter(cacheString);
		renderTemplate(context, cachedStream, userOutputHTML);
		result = cacheString.toString();

		getResponse().setContentType("text/html");
		PrintWriter out = getResponse().getWriter();
		out.println(result);
		out.flush();
		out.close();
				
		return NONE;
	}

	/**
	 * This method prepares the script-object which should be supplied to the scripting engine.
	 */
	
	private ScriptController getScriptController() throws Exception
	{
		ScriptController scriptController = new BasicScriptController(this.getInfoGluePrincipal());
		
		return scriptController; 
	}

	/**
	 * This method takes arguments and renders a template given as a string to the specified outputstream.
	 * Improve later - cache for example the engine.
	 */
	
	public void renderTemplate(Map params, PrintWriter pw, String templateAsString) throws Exception 
	{
		Velocity.init();

		VelocityContext context = new VelocityContext();
		Iterator i = params.keySet().iterator();
		while(i.hasNext())
		{
			String key = (String)i.next();
			context.put(key, params.get(key));
		}
        
		Reader reader = new StringReader(templateAsString);
		boolean finished = Velocity.evaluate(context, pw, "Generator Error", reader);        
	}
	

	/**
	 * This method returns the contents that are of contentTypeDefinition "HTMLTemplate"
	 */
	public List getTasks() throws Exception
	{
		HashMap arguments = new HashMap();
		arguments.put("method", "selectListOnContentTypeName");
		
		List argumentList = new ArrayList();
		HashMap argument = new HashMap();
		argument.put("contentTypeDefinitionName", "TaskDefinition");
		argumentList.add(argument);
		arguments.put("arguments", argumentList);
		
		return ContentControllerProxy.getController().getACContentVOList(this.getInfoGluePrincipal(), arguments);
		//return ContentController.getContentController().getContentVOList(arguments);
	}
	     
	public Integer getTaskContentId()
	{
		return taskContentId;
	}

	public void setTaskContentId(Integer taskContentId)
	{
		this.taskContentId = taskContentId;
	}

	public Integer getContentId()
	{
		return contentId;
	}

	public void setContentId(Integer contentId)
	{
		this.contentId = contentId;
	}
}
