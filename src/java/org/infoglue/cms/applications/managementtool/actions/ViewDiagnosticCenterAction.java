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

package org.infoglue.cms.applications.managementtool.actions;

import java.util.ArrayList;
import java.util.List;

import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.entities.management.Chat;
import org.infoglue.cms.entities.management.Message;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.CmsSessionContextListener;

/**
 * This class represents the diagnostic screen of the system
 * 
 * @author mattias
 */

public class ViewDiagnosticCenterAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = 1L;

	private List internalDeliverUrls = null;
	private List publicDeliverUrls = null;
		
    public String doExecute() throws Exception
    {
    	this.internalDeliverUrls = CmsPropertyHandler.getInternalDeliveryUrls();
    	this.publicDeliverUrls = CmsPropertyHandler.getPublicDeliveryUrls();
    	
        return "success";
    }

	public List getInternalDeliverUrls()
	{
		return internalDeliverUrls;
	}

	public List getPublicDeliverUrls()
	{
		return publicDeliverUrls;
	}

}