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

import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.entities.management.Chat;

import java.util.*;

/**
 * This class represents the message center where you can chat with users
 * 
 * @author mattias
 */

public class ViewMessageCenterAction extends InfoGlueAbstractAction
{
	private static Chat chat = new Chat();
	private Integer lastId;
	private String userName;
	private String message;
	private List messages;
	
    public String doExecute() throws Exception
    {
        return "success";
    }

    public String doGetMessages() throws Exception
    {
    	System.out.println("Last id:" + lastId);

    	if(lastId == null || lastId.intValue() == -1)
    		messages = chat.getMessages();
    	else
    		messages = chat.getMessages(lastId.intValue());

    	System.out.println("Getting messages:" + messages.size());

    	return "successGetMessages";
    }

    public String doSendMessage() throws Exception
    {
    	System.out.println("Adding message:" + message);
    	
    	chat.addMessage(this.getUserName(), this.message);
    	
        return "successMessageSent";
    }

	public String getMessage() 
	{
		return message;
	}

	public void setMessage(String message) 
	{
		this.message = message;
	}

	public List getMessages() 
	{
		return messages;
	}

	public Integer getLastId() {
		return lastId;
	}

	public void setLastId(Integer lastId) {
		this.lastId = lastId;
	}

}
