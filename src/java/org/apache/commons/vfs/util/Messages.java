/* ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.commons.vfs.util;

import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Map;
import java.util.HashMap;
import java.text.MessageFormat;

/**
 * Formats messages.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 1.3 $ $Date: 2002/10/23 11:59:42 $
 */
public class Messages
{
    /** Map from message code to MessageFormat object for the message. */
    private static final Map messages = new HashMap();
    private static ResourceBundle resources;

    private Messages()
    {
    }

    /**
     * Formats a message.
     *
     * @param code The message code.
     * @return The formatted message.
     */
    public static String getString( final String code )
    {
        return getString( code, new Object[ 0 ] );
    }

    /**
     * Formats a message.
     *
     * @param code The message code.
     * @param param The message parameter.
     * @return The formatted message.
     */
    public static String getString( final String code, final Object param )
    {
        return getString( code, new Object[] { param } );
    }

    /**
     * Formats a message.
     *
     * @param code The message code.
     * @param params The message parameters.
     * @return The formatted message.
     */
    public static String getString( final String code, final Object[] params )
    {
        try
        {
            final MessageFormat msg = findMessage( code );
            return msg.format( params );
        }
        catch ( final MissingResourceException mre )
        {
            return "Unknown message with code \"" + code + "\".";
        }
    }

    /**
     * Locates a message by its code.
     */
    private static synchronized MessageFormat findMessage( final String code )
        throws MissingResourceException
    {
        // Check if the message is cached
        MessageFormat msg = (MessageFormat)messages.get( code );
        if ( msg != null )
        {
            return msg;
        }

        // Locate the message
        if ( resources == null )
        {
            resources = ResourceBundle.getBundle( "org.apache.commons.vfs.Resources" );
        }
        final String msgText = resources.getString( code );
        msg = new MessageFormat( msgText );
        messages.put( code, msg );
        return msg;
    }
}
