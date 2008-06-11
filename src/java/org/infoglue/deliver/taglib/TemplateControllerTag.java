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

package org.infoglue.deliver.taglib;

import java.util.Locale;

import javax.servlet.jsp.JspTagException;

import org.infoglue.cms.util.StringManager;
import org.infoglue.cms.util.StringManagerFactory;
import org.infoglue.deliver.controllers.kernel.impl.simple.TemplateController;

/**
 * Base class for all tags using the TemplateController object.
 */
public abstract class TemplateControllerTag extends AbstractTag 
{
	/**
	 * Default constructor.
	 */
	protected TemplateControllerTag()
	{
		super();
	}
	
	/**
	 * Returns the template controller.
	 * 
	 * Note! Do not called this function before the PageContext is initialized.
	 * 
	 * @return the template controller.
	 * @throws JspTagException if the template controller wasn't found.
	 */
	protected TemplateController getController() throws JspTagException
	{
	    TemplateController controller;
    	
	    try 
		{
			controller = (TemplateController) this.pageContext.getRequest().getAttribute("org.infoglue.cms.deliver.templateLogic");
			if(controller == null)
			{
				throw new NullPointerException("No TemplateController found in context.");
			}
		} 
		catch(Exception e) 
		{
			e.printStackTrace();
			throw new JspTagException(e.getMessage());
		}
			
		return controller;
	}
	
	protected String getLocalizedString(Locale locale, String key) 
  	{
    	StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.applications", locale);

    	return stringManager.getString(key);
  	}

	protected String getLocalizedString(Locale locale, String key, Object arg1) 
  	{
    	StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.applications", locale);

    	return stringManager.getString(key, arg1);
  	}

	protected String getLocalizedString(Locale locale, String key, Object arg1, Object arg2) 
  	{
    	StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.applications", locale);

    	return stringManager.getString(key, arg1, arg2);
  	}

}
