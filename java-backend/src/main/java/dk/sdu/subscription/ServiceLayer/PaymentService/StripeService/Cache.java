package dk.sdu.subscription.ServiceLayer.PaymentService.StripeService;

import com.stripe.exception.StripeException;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Cache<E> {
    private final Semaphore _semaphore = new Semaphore(1, true);
    private final HashMap<String, Long> _cacheRefreshed;
    private final HashMap<String, E> _cache;
    private final long _cacheTtl;

    public Cache(long cacheTtl) {
        _cache = new HashMap<>();
        _cacheRefreshed = new HashMap<>();
        _cacheTtl = cacheTtl;
    }

    public Cache() {
        this(60 * 1000); //Default cache ttl is 60 seconds
    }

    public E getOrSetCache(String identifier, ICacheSource<E> cacheSource, boolean forceRefresh) throws StripeException {
        if (shouldRefreshCache(identifier, forceRefresh)) {
            try {
                _semaphore.acquire();
                if (shouldRefreshCache(identifier, forceRefresh)) {
                    //System.out.println("REFRESHING CACHE for identifier: " + identifier);
                    //Refresh cache
                    E freshData = cacheSource.refreshCache();
                    _cache.put(identifier, freshData);
                    _cacheRefreshed.put(identifier, Instant.now().toEpochMilli());
                    return freshData;
                }
            } catch (InterruptedException ignored) {
                //System.out.println("Semaphore interrupted in Cache.java!! This might cause errors!!");
            } finally {
                _semaphore.release();
            }
        }

        //System.out.println("Loading from cache for identifier: " + identifier);
        return _cache.getOrDefault(identifier, null);
    }

    public E getOrSetCache(String identifier, ICacheSource<E> cacheSource) throws StripeException {
        return getOrSetCache(identifier, cacheSource, false);
    }

    public void clear(String identifier) {
        try {
            _semaphore.acquire();
            _cache.remove(identifier);
            _cacheRefreshed.remove(identifier);
        } catch (InterruptedException ignored) {
        } finally {
            _semaphore.release();
        }
    }

    public void clear() {
        try {
            _semaphore.acquire();
            _cache.clear();
            _cacheRefreshed.clear();
        } catch (InterruptedException ignored) {
        } finally {
            _semaphore.release();
        }
    }

    //Private methods

    private boolean shouldRefreshCache(String identifier, boolean forceRefresh) {
        return !_cache.containsKey(identifier) || cacheExpiredForIdentifier(identifier) || forceRefresh;
    }

    private boolean cacheExpiredForIdentifier(String identifier) {
        return (_cacheRefreshed.getOrDefault(identifier, 0L) + _cacheTtl) < Instant.now().toEpochMilli();
    }
}
