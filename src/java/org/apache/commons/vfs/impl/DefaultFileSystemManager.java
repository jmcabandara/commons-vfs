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
package org.apache.commons.vfs.impl;

import java.io.File;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.provider.DefaultURLStreamHandler;
import org.apache.commons.vfs.provider.FileProvider;
import org.apache.commons.vfs.provider.FileReplicator;
import org.apache.commons.vfs.provider.LocalFileProvider;
import org.apache.commons.vfs.provider.UriParser;
import org.apache.commons.vfs.provider.VfsComponent;

/**
 * A default file system manager implementation.  To use this class:
 * <ul>
 * <li>Create an instance of this class.
 * <li>Call {@link #setLogger} to set the logger that the manager, and its
 * components, should use.
 * <li>Add one or more file providers using {@link #addProvider}.
 * <li>Set the default provider using {@link #setDefaultProvider} (optional).
 * <li>Set the file replicator using {@link #setReplicator} (optional).
 * <li>Set the base file using {@link #setBaseFile} (optional).
 * </ul>
 *
 * <p>When finished with the manager, call its {@link #close} method to clean
 * up any resources it is using.
 *
 * @todo - Extract an AbstractFileSystemManager super-class from this class.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 1.12 $ $Date: 2002/07/05 06:51:45 $
 */
public class DefaultFileSystemManager
    implements FileSystemManager
{

    /** The provider for local files. */
    private LocalFileProvider localFileProvider;

    /** The default provider. */
    private FileProvider defaultProvider;

    /** The file replicator to use. */
    private FileReplicator fileReplicator;

    /** Mapping from URI scheme to FileProvider. */
    private final Map providers = new HashMap();

    /** All components used by this manager. */
    private final ArrayList components = new ArrayList();

    /** The base file to use for relative URI. */
    private FileObject baseFile;

    /** The logger to use. */
    private Log log = LogFactory.getLog( DefaultFileSystemManager.class );

    /** The context to pass to providers. */
    private final DefaultProviderContext context =
        new DefaultProviderContext( this );

    /**
     * Registers a file system provider.  The manager takes care of all
     * lifecycle management.
     */
    public void addProvider( final String urlScheme,
                             final FileProvider provider )
        throws FileSystemException
    {
        addProvider( new String[]{urlScheme}, provider );
    }

    /**
     * Registers a file system provider.  The manager takes care of all
     * lifecycle management.
     */
    public void addProvider( final String[] urlSchemes,
                             final FileProvider provider )
        throws FileSystemException
    {
        // Check for duplicates
        for ( int i = 0; i < urlSchemes.length; i++ )
        {
            final String scheme = urlSchemes[ i ];
            if ( providers.containsKey( scheme ) )
            {
                throw new FileSystemException( "vfs.impl/multiple-providers-for-scheme.error", scheme );
            }
        }

        // Contextualise the component (if not already)
        setupComponent( provider );

        // Add to map
        for ( int i = 0; i < urlSchemes.length; i++ )
        {
            final String scheme = urlSchemes[ i ];
            providers.put( scheme, provider );
        }

        if ( provider instanceof LocalFileProvider )
        {
            localFileProvider = (LocalFileProvider)provider;
        }
    }

    /**
     * Sets the default provider.  This is the provider that will handle URI
     * with unknown schemes.  The manager takes care of all lifecycle
     * management.
     */
    public void setDefaultProvider( final FileProvider provider )
        throws FileSystemException
    {
        setupComponent( provider );
        defaultProvider = provider;
    }

    /**
     * Sets the file replicator to use.  The manager takes care of all
     * lifecycle management.
     */
    public void setReplicator( final FileReplicator replicator )
        throws FileSystemException
    {
        setupComponent( replicator );
        fileReplicator = replicator;
    }

    /**
     * Sets the logger to use.
     */
    public void setLogger( final Log log )
    {
        this.log = log;
    }

    /**
     * Adds a component to the set of components owned by this manager.
     */
    private void setupComponent( final Object component )
        throws FileSystemException
    {
        if ( !components.contains( component ) )
        {
            if ( component instanceof VfsComponent )
            {
                final VfsComponent vfsComponent = (VfsComponent)component;
                vfsComponent.setLogger( log );
                vfsComponent.setContext( context );
                vfsComponent.init();
            }
            components.add( component );
        }
    }

    /**
     * Closes a component.
     */
    private void closeComponent( final Object component )
    {
        if ( component instanceof VfsComponent )
        {
            final VfsComponent vfsComponent = (VfsComponent)component;
            vfsComponent.close();
        }
    }

    /**
     * Returns the file replicator.
     *
     * @return The file replicator.  Never returns null.
     */
    public FileReplicator getReplicator()
        throws FileSystemException
    {
        if ( fileReplicator == null )
        {
            throw new FileSystemException( "vfs.impl/no-replicator.error" );
        }
        return fileReplicator;
    }

    /**
     * Closes all files created by this manager, and cleans up any temporary
     * files.  Also closes all providers and the replicator.
     */
    public void close()
    {
        // Dispose the components (making sure we only dispose each provider
        // only once).  Close the replicator last.
        for ( int i = 0; i < components.size(); i++ )
        {
            Object component = components.get( i );
            if ( component == fileReplicator )
            {
                continue;
            }
            closeComponent( component );
        }
        if ( fileReplicator != null )
        {
            closeComponent( fileReplicator );
        }

        components.clear();
        providers.clear();
        localFileProvider = null;
        defaultProvider = null;
        fileReplicator = null;
    }

    /**
     * Sets the base file to use when resolving relative URI.
     */
    public void setBaseFile( final FileObject baseFile )
        throws FileSystemException
    {
        this.baseFile = baseFile;
    }

    /**
     * Sets the base file to use when resolving relative URI.
     */
    public void setBaseFile( final File baseFile ) throws FileSystemException
    {
        this.baseFile = getLocalFileProvider().findLocalFile( baseFile );
    }

    /**
     * Returns the base file used to resolve relative URI.
     */
    public FileObject getBaseFile()
    {
        return baseFile;
    }

    /**
     * Locates a file by URI.
     */
    public FileObject resolveFile( final String uri ) throws FileSystemException
    {
        return resolveFile( baseFile, uri );
    }

    /**
     * Locates a file by URI.
     */
    public FileObject resolveFile( final File baseFile, final String uri )
        throws FileSystemException
    {
        final FileObject baseFileObj =
            getLocalFileProvider().findLocalFile( baseFile );
        return resolveFile( baseFileObj, uri );
    }

    /**
     * Resolves a URI, relative to a base file.
     */
    public FileObject resolveFile( final FileObject baseFile, final String uri )
        throws FileSystemException
    {
        // Extract the scheme
        final String scheme = UriParser.extractScheme( uri );
        if ( scheme != null )
        {
            // An absolute URI - locate the provider
            final FileProvider provider = (FileProvider)providers.get( scheme );
            if ( provider != null )
            {
                return provider.findFile( baseFile, uri );
            }

            // Otherwise, assume a local file
        }

        // Decode the URI (remove %nn encodings)
        final String decodedUri = UriParser.decode( uri );

        // Handle absolute file names
        if ( localFileProvider != null
            && localFileProvider.isAbsoluteLocalName( decodedUri ) )
        {
            return localFileProvider.findLocalFile( decodedUri );
        }

        if ( scheme != null )
        {
            // An unknown scheme - hand it to the default provider
            if ( defaultProvider == null )
            {
                throw new FileSystemException( "vfs.impl/unknown-scheme.error", new Object[]{scheme, uri} );
            }
            return defaultProvider.findFile( baseFile, uri );
        }

        // Assume a relative name - use the supplied base file
        if ( baseFile == null )
        {
            throw new FileSystemException( "vfs.impl/find-rel-file.error", uri );
        }
        return baseFile.resolveFile( decodedUri );
    }

    /**
     * Converts a local file into a {@link FileObject}.
     */
    public FileObject toFileObject( final File file )
        throws FileSystemException
    {
        return getLocalFileProvider().findLocalFile( file );
    }

    /**
     * Creates a layered file system.
     */
    public FileObject createFileSystem( final String scheme,
                                        final FileObject file )
        throws FileSystemException
    {
        FileProvider provider = (FileProvider)providers.get( scheme );
        if ( provider == null )
        {
            throw new FileSystemException( "vfs.impl/unknown-provider.error", scheme );
        }
        return provider.createFileSystem( scheme, file );
    }

    /**
     * Locates the local file provider.
     */
    private LocalFileProvider getLocalFileProvider()
        throws FileSystemException
    {
        if ( localFileProvider == null )
        {
            throw new FileSystemException( "vfs.impl/no-local-file-provider.error" );
        }
        return localFileProvider;
    }

    /**
     * Get the URLStreamHandlerFactory.
     */
    public URLStreamHandlerFactory getURLStreamHandlerFactory()
    {
        return new VfsStreamHandlerFactory();
    }

    /**
     * This is an internal class because it needs access to the private
     * member providers.
     */
    final class VfsStreamHandlerFactory implements URLStreamHandlerFactory
    {
        public URLStreamHandler createURLStreamHandler( final String protocol )
        {
            FileProvider provider = (FileProvider)providers.get( protocol );
            if ( provider != null )
            {
                return new DefaultURLStreamHandler( context );
            }

            //Route all other calls to the default URLStreamHandlerFactory
            return new URLStreamHandlerProxy();
        }
    }
}
