/*******************************************************************************
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco Mobile for Android.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.alfresco.mobile.android.async.node;

import java.io.IOException;

import org.alfresco.mobile.android.api.model.ContentFile;
import org.alfresco.mobile.android.api.model.Document;
import org.alfresco.mobile.android.api.model.Folder;
import org.alfresco.mobile.android.async.LoaderResult;
import org.alfresco.mobile.android.async.OperationAction;
import org.alfresco.mobile.android.async.OperationsDispatcher;
import org.alfresco.mobile.android.async.Operator;
import org.alfresco.mobile.android.async.utils.ContentFileProgressImpl;
import org.alfresco.mobile.android.async.utils.ContentFileProgressImpl.ReaderListener;

import android.util.Log;

public abstract class UpNodeOperation extends NodeOperation<Document> implements ReaderListener
{
    private static final String TAG = UpNodeOperation.class.getName();

    /** Binary Content of the future document. */
    protected ContentFile contentFile;

    private int segment = 0;

    private long totalLength = 0;

    // ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    // ///////////////////////////////////////////////////////////////////////////
    public UpNodeOperation(Operator operator, OperationsDispatcher dispatcher, OperationAction action)
    {
        super(operator, dispatcher, action);
        if (request instanceof UpNodeRequest)
        {
            this.contentFile = ((UpNodeRequest) request).contentFile;
            this.totalLength = contentFile.getLength();
            this.segment = initSegment();
        }
    }

    // ///////////////////////////////////////////////////////////////////////////
    // LIFE CYCLE
    // ///////////////////////////////////////////////////////////////////////////
    protected LoaderResult<Document> doInBackground()
    {
        try
        {
            super.doInBackground();

            if (contentFile instanceof ContentFileProgressImpl)
            {
                ((ContentFileProgressImpl) contentFile).setReaderListener(this);
            }

            if (listener != null)
            {
                listener.onPreExecute(this);
            }
        }
        catch (Exception e)
        {
            Log.w(TAG, Log.getStackTraceString(e));
        }
        return new LoaderResult<Document>();
    }

    // ///////////////////////////////////////////////////////////////////////////
    // UTILS
    // ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onRead(ContentFileProgressImpl contentFile, Long amountCopied) throws IOException
    {
        if (Thread.currentThread().isInterrupted())
        {
            hasCancelled = true;
            throw new IOException(EXCEPTION_OPERATION_CANCEL);
        }
        saveProgress(amountCopied);
    }

    protected void saveProgress(long progress)
    {
        if (request.notificationUri != null && request instanceof UpNodeRequest)
        {
            context.getContentResolver().update(request.notificationUri,
                    ((UpNodeRequest) request).createContentValues(progress), null, null);
        }
    }

    private int initSegment()
    {
        int segment = 1;

        // 100kb
        if (totalLength < 102400)
        {
            segment = 2;
        }
        else
        // 500kb
        if (totalLength < 512000)
        {
            segment = 3;
        }
        else if (totalLength < 1048576)
        {
            // 1MB
            segment = 4;
        }
        else if (totalLength < 5242880)
        {
            // 5MB
            segment = 10;
        }
        else if (totalLength < 10485760)
        {
            // 10MB
            segment = 15;
        }
        else if (totalLength < 20971520)
        {
            // 20MB
            segment = 20;
        }
        else if (totalLength < 52428800)
        {
            // 50MB
            segment = 25;
        }
        else
        {
            segment = Math.round(totalLength / 1048576);
        }

        return segment;
    }

    protected Folder retrieveParentFolder()
    {
        if (parentFolder == null && parentFolderIdentifier != null)
        {
            parentFolder = (Folder) session.getServiceRegistry().getDocumentFolderService()
                    .getNodeByIdentifier(parentFolderIdentifier);
        }
        return parentFolder;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    // ///////////////////////////////////////////////////////////////////////////
    public int getSegment()
    {
        return segment;
    }

    public ContentFile getContentFile()
    {
        return contentFile;
    }

    public Folder getParentFolder()
    {
        return parentFolder;
    }

    public boolean hasCancelled()
    {
        return hasCancelled;
    }
}
