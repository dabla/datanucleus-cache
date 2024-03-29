/**********************************************************************
Copyright (c) 2009 Clive Cox and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
    ...
**********************************************************************/
package org.datanucleus.cache.spymemcached;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import org.datanucleus.NucleusContext;
import org.datanucleus.PersistenceConfiguration;
import org.datanucleus.cache.AbstractLevel2Cache;
import org.datanucleus.cache.CachedPC;
import org.datanucleus.exceptions.NucleusException;

/**
 * Plugin using Spymemcached implementation of "memcached" as a Level2 cache.
 * We use the "cacheName" as the keyPrefix to distinguish our objects from others in memcached.
 */
public class SpymemcachedLevel2Cache extends AbstractLevel2Cache
{
    private MemcachedClient client;

    private int expireSeconds = 0;

    public SpymemcachedLevel2Cache(NucleusContext nucleusCtx)
    {
        super(nucleusCtx);

        PersistenceConfiguration conf = nucleusCtx.getPersistenceConfiguration();

        String expireStr = conf.getStringProperty("datanucleus.cache.level2.memcached.expireSeconds");
        if (expireStr != null && !"".equals(expireStr))
        {
            expireSeconds = Integer.parseInt(expireStr);
        }

        try
        {
            String servers = conf.getStringProperty("datanucleus.cache.level2.memcached.servers");
            client = new MemcachedClient(AddrUtil.getAddresses(servers));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new NucleusException("Cant create cache", e);
        }
        catch (NumberFormatException ex)
        {
            throw new NucleusException("Cant create cache: Bad expireSeconds value:" + expireStr, ex);
        }
    }

    public void close()
    {
        if (clearAtClose)
        {
            client.flush();
            client.shutdown();
        }
    }

    public boolean containsOid(Object oid)
    {
        return get(oid) != null;
    }

    public synchronized void evict(Object oid)
    {
        client.delete(cacheName + getCacheKeyForId(oid));
    }

    public synchronized void evictAll()
    {
        client.flush();
    }

    public synchronized void evictAll(Object[] oids)
    {
        if (oids == null)
        {
            return;
        }

        for (int i = 0; i < oids.length; i++)
        {
            evict(oids[i]);
        }
    }

    public synchronized void evictAll(Collection oids)
    {
        if (oids == null)
        {
            return;
        }

        Iterator iter = oids.iterator();
        while (iter.hasNext())
        {
            evict(iter.next());
        }
    }

    public synchronized void evictAll(Class arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("evictAll(Class,Boolean) method not supported by spymemcached plugin");
    }

    public CachedPC get(Object oid)
    {
        return (CachedPC) client.get(cacheName + getCacheKeyForId(oid));
    }

    public int getSize()
    {
        throw new UnsupportedOperationException("getSize() method not supported by spymemcached plugin");
    }

    public boolean isEmpty()
    {
        throw new UnsupportedOperationException("isEmpty() method not supported by spymemcached plugin");
    }

    public synchronized CachedPC put(Object oid, CachedPC pc)
    {
        if (oid == null || pc == null)
        {
            return null;
        }

        client.set(cacheName + getCacheKeyForId(oid), expireSeconds, pc);

        return pc;
    }

    protected String getCacheKeyForId(Object id)
    {
        if (nucleusCtx.getApiAdapter().isSingleFieldIdentity(id))
        {
            String targetClassName = nucleusCtx.getApiAdapter().getTargetClassNameForSingleFieldIdentity(id);
            return targetClassName + ":" + id;
        }
        else
        {
            return id.toString();
        }
    }
}